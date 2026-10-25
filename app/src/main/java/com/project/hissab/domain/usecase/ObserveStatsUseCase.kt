package com.project.hissab.domain.usecase

import com.project.hissab.domain.model.CategoryTotal
import com.project.hissab.domain.model.MonthTotal
import com.project.hissab.domain.model.StatsData
import com.project.hissab.domain.repository.BudgetRepository
import com.project.hissab.domain.repository.ExpenseRepository
import java.time.Clock
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveStatsUseCase(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val monthsShown: Int = 5
) {
    operator fun invoke(): Flow<StatsData> =
        combine(expenseRepository.observeExpenses(), budgetRepository.observeBudgets()) { all, budgets ->
            val current = YearMonth.now(clock)
            val byMonth = all.groupBy { YearMonth.from(it.dateTime) }
            val thisMonth = byMonth[current].orEmpty()

            StatsData(
                month = current,
                monthTotal = thisMonth.sumOf { it.amount },
                categoryTotals = thisMonth.groupBy { it.categoryId }
                    .map { (id, list) -> CategoryTotal(id, list.sumOf { it.amount }) }
                    .sortedByDescending { it.amount },
                recentMonths = (monthsShown - 1 downTo 0).map { back ->
                    val m = current.minusMonths(back.toLong())
                    MonthTotal(m, byMonth[m].orEmpty().sumOf { it.amount }, budgets.budgetFor(m))
                }
            )
        }
}