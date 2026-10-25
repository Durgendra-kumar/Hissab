package com.project.hissab.domain.usecase

import com.project.hissab.domain.model.MonthDetail
import com.project.hissab.domain.model.MonthTotal
import com.project.hissab.domain.repository.BudgetRepository
import com.project.hissab.domain.repository.ExpenseRepository
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveMonthDetailUseCase(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(month: YearMonth): Flow<MonthDetail> =
        combine(expenseRepository.observeExpenses(), budgetRepository.observeBudgets()) { all, budgets ->
            val list = all.inMonth(month).sortedByDescending { it.dateTime }
            MonthDetail(
                summary = MonthTotal(month, list.sumOf { it.amount }, budgets.budgetFor(month), list.size),
                expenses = list
            )
        }
}