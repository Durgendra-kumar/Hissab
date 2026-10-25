package com.project.hissab.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.project.hissab.HissabApp
import com.project.hissab.domain.model.BudgetLevel
import com.project.hissab.domain.model.BudgetStatus
import com.project.hissab.domain.model.Category
import com.project.hissab.domain.model.Expense
import com.project.hissab.domain.model.ExpenseSummary
import com.project.hissab.domain.model.PaymentMethod
import com.project.hissab.domain.repository.CategoryRepository
import com.project.hissab.domain.usecase.DeleteExpenseUseCase
import com.project.hissab.domain.usecase.ObserveBudgetStatusUseCase
import com.project.hissab.domain.usecase.ObserveExpenseSummaryUseCase
import com.project.hissab.util.Formatters
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Clock

data class ExpenseItemUi(
    val id: Long,
    val title: String,
    val dateText: String,
    val amountText: String,
    val paymentLabel: String,
    val icon: String,
    val iconColor: Int,
    val isUnnecessary: Boolean
)
/** One line in the Home list: either a day header or an expense. */
sealed interface HomeRow {
    data class Header(val key: String, val label: String) : HomeRow
    data class Item(val item: ExpenseItemUi) : HomeRow
}

data class HomeUiState(
    val monthText: String = "",
    val totalText: String = "₹0.00",
    val upiText: String = "₹0",
    val cashText: String = "₹0",
    val cardText: String = "₹0",
    val unnecessaryText: String = "₹0",
    val rows: List<HomeRow> = emptyList(),
    val expenseCount: Int = 0,
    val budgetFill: Float = 0f,
    val budgetLevel: BudgetLevel = BudgetLevel.SAFE
)

/** Domain objects in, display values out. */
class HomeUiMapper(private val clock: Clock = Clock.systemDefaultZone()) {

    fun map(summary: ExpenseSummary, categories: List<Category>, budget: BudgetStatus) = HomeUiState(
        monthText = Formatters.month(summary.month),
        totalText = Formatters.rupees(summary.total),
        upiText = Formatters.rupees(summary.totalFor(PaymentMethod.UPI), decimals = false),
        cashText = Formatters.rupees(summary.totalFor(PaymentMethod.CASH), decimals = false),
        cardText = Formatters.rupees(summary.totalFor(PaymentMethod.CREDIT_CARD), decimals = false),
        unnecessaryText = Formatters.rupees(summary.unnecessaryTotal, decimals = false),
        rows = rowsFor(summary.expenses, categories),
        expenseCount = summary.expenses.size,
        budgetFill = budget.ratio.coerceIn(0.0, 1.0).toFloat(),
        budgetLevel = budget.level
    )

    /** Expenses grouped by day, newest day first, each day under a header. */
    fun rowsFor(expenses: List<Expense>, categories: List<Category>): List<HomeRow> {
        val byId = categories.associateBy { it.id }
        val today = LocalDate.now(clock)
        return expenses.sortedByDescending { it.dateTime }
            .groupBy { it.dateTime.toLocalDate() }
            .flatMap { (day, list) ->
                listOf<HomeRow>(HomeRow.Header(day.toString(), Formatters.dayLabel(day, today))) +
                        list.map { HomeRow.Item(toItem(it, byId[it.categoryId] ?: Category.UNKNOWN)) }
            }
    }

    fun toItem(e: Expense, c: Category) = ExpenseItemUi(
        id = e.id,
        title = e.title,
        dateText = Formatters.dateTime(e.dateTime),
        amountText = "-" + Formatters.rupees(e.amount),
        paymentLabel = e.paymentMethod.label(),
        icon = c.icon,
        iconColor = c.color,
        isUnnecessary = e.isUnnecessary
    )

    private fun PaymentMethod.label() = when (this) {
        PaymentMethod.UPI -> "UPI"
        PaymentMethod.CASH -> "Cash"
        PaymentMethod.CREDIT_CARD -> "Credit Card"
    }
}

sealed interface HomeEvent {
    object ExpenseDeleted : HomeEvent
}

class HomeViewModel(
    observeSummary: ObserveExpenseSummaryUseCase,
    observeBudget: ObserveBudgetStatusUseCase,
    categoryRepository: CategoryRepository,
    mapper: HomeUiMapper,
    private val deleteExpense: DeleteExpenseUseCase
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(observeSummary(), categoryRepository.observeCategories(), observeBudget(), mapper::map)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onDeleteConfirmed(id: Long) {
        viewModelScope.launch {
            deleteExpense(id)
            _events.send(HomeEvent.ExpenseDeleted)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as HissabApp).container
                HomeViewModel(c.observeExpenseSummary, c.observeBudgetStatus, c.categoryRepository, HomeUiMapper(), c.deleteExpense)
            }
        }
    }
}