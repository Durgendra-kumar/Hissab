package com.project.hissab.domain.usecase

import com.project.hissab.domain.model.ExpenseSummary
import com.project.hissab.domain.repository.ExpenseRepository
import java.time.Clock
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Summary of the CURRENT month only. The clock is injectable so tests can fake "today". */
class ObserveExpenseSummaryUseCase(
    private val repository: ExpenseRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    operator fun invoke(): Flow<ExpenseSummary> = repository.observeExpenses().map { all ->
        val month = YearMonth.now(clock)
        val list = all.inMonth(month)
        ExpenseSummary(
            month = month,
            expenses = list.sortedByDescending { it.dateTime },
            total = list.sumOf { it.amount },
            totalsByPayment = list.groupBy { it.paymentMethod }.mapValues { (_, v) -> v.sumOf { it.amount } },
            unnecessaryTotal = list.filter { it.isUnnecessary }.sumOf { it.amount }
        )
    }
}