package com.project.hissab.domain.usecase

import com.project.hissab.domain.model.BudgetStatus
import com.project.hissab.domain.repository.BudgetRepository
import com.project.hissab.domain.repository.ExpenseRepository
import java.time.Clock
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** This month's spending against this month's budget (or the one carried over from an earlier month). */
class ObserveBudgetStatusUseCase(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    operator fun invoke(): Flow<BudgetStatus> =
        combine(expenseRepository.observeExpenses(), budgetRepository.observeBudgets()) { all, budgets ->
            val month = YearMonth.now(clock)
            val source = budgets.sourceMonthFor(month)
            BudgetStatus(
                limit = source?.let { budgets[it] },
                spent = all.inMonth(month).sumOf { it.amount },
                carriedFrom = source?.takeIf { it != month }
            )
        }
}