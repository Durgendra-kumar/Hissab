package com.project.hissab.domain.usecase

import com.project.hissab.domain.repository.ExpenseRepository
import java.time.Clock
import java.time.YearMonth

/** Deletes every expense in a PAST month. The current month can't be cleared this way. */
class DeleteMonthUseCase(
    private val repository: ExpenseRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    /** Returns how many expenses were deleted. */
    suspend operator fun invoke(month: YearMonth): Int {
        if (month >= YearMonth.now(clock)) return 0
        return repository.deleteExpensesInMonth(month)
    }
}