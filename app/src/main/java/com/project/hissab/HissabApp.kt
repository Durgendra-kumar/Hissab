package com.project.hissab

import android.app.Application
import android.content.Context
import com.project.hissab.data.DataStoreBudgetRepository
import com.project.hissab.data.DefaultCategoryRepository
import com.project.hissab.data.RoomExpenseRepository
import com.project.hissab.data.local.AppDatabase
import com.project.hissab.domain.repository.BudgetRepository
import com.project.hissab.domain.repository.CategoryRepository
import com.project.hissab.domain.repository.ExpenseRepository
import com.project.hissab.domain.usecase.AddExpenseUseCase
import com.project.hissab.domain.usecase.DeleteExpenseUseCase
import com.project.hissab.domain.usecase.DeleteMonthUseCase
import com.project.hissab.domain.usecase.ExpenseValidator
import com.project.hissab.domain.usecase.GetExpenseUseCase
import com.project.hissab.domain.usecase.ObserveBudgetStatusUseCase
import com.project.hissab.domain.usecase.ObserveExpenseSummaryUseCase
import com.project.hissab.domain.usecase.ObserveHistoryUseCase
import com.project.hissab.domain.usecase.ObserveMonthDetailUseCase
import com.project.hissab.domain.usecase.ObserveSpendingOverviewUseCase
import com.project.hissab.domain.usecase.ObserveStatsUseCase
import com.project.hissab.domain.usecase.SetMonthlyBudgetUseCase
import com.project.hissab.domain.usecase.UpdateExpenseUseCase
import com.project.hissab.widget.SpendingWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency injection: the one place that knows the concrete classes. */
class AppContainer(context: Context) {
    private val database = AppDatabase.create(context)
    private val validator = ExpenseValidator()

    val expenseRepository: ExpenseRepository = RoomExpenseRepository(database.expenseDao())
    val categoryRepository: CategoryRepository = DefaultCategoryRepository()
    val budgetRepository: BudgetRepository = DataStoreBudgetRepository(context)

    val addExpense = AddExpenseUseCase(expenseRepository, validator)
    val updateExpense = UpdateExpenseUseCase(expenseRepository, validator)
    val getExpense = GetExpenseUseCase(expenseRepository)
    val deleteExpense = DeleteExpenseUseCase(expenseRepository)
    val observeExpenseSummary = ObserveExpenseSummaryUseCase(expenseRepository)
    val observeStats = ObserveStatsUseCase(expenseRepository, budgetRepository)
    val setMonthlyBudget = SetMonthlyBudgetUseCase(budgetRepository)
    val observeBudgetStatus = ObserveBudgetStatusUseCase(expenseRepository, budgetRepository)

    val observeHistory = ObserveHistoryUseCase(expenseRepository, budgetRepository)
    val observeMonthDetail = ObserveMonthDetailUseCase(expenseRepository, budgetRepository)
    val deleteMonth = DeleteMonthUseCase(expenseRepository)

    val observeSpendingOverview =
        ObserveSpendingOverviewUseCase(expenseRepository, budgetRepository)
    val spendingWidgetUpdater =
        SpendingWidgetUpdater(context.applicationContext, observeSpendingOverview)
}

class HissabApp : Application() {
    val container by lazy { AppContainer(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container.spendingWidgetUpdater.keepInSync(appScope)    // widgets follow every change in the app
    }
}