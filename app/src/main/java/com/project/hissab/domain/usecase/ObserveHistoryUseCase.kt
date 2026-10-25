package com.project.hissab.domain.usecase

import com.project.hissab.domain.model.MonthTotal
import com.project.hissab.domain.repository.BudgetRepository
import com.project.hissab.domain.repository.ExpenseRepository
import java.time.Clock
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** One entry per past month (before the current one), newest first, with that month's budget. */
class ObserveHistoryUseCase(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    operator fun invoke(): Flow<List<MonthTotal>> =
        combine(expenseRepository.observeExpenses(), budgetRepository.observeBudgets()) { all, budgets ->
            val current = YearMonth.now(clock)
            all.groupBy { YearMonth.from(it.dateTime) }
                .filterKeys { it < current }
                .map { (month, list) ->
                    MonthTotal(month, list.sumOf { it.amount }, budgets.budgetFor(month), list.size)
                }
                .sortedByDescending { it.month }
        }
}