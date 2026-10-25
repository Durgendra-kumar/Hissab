package com.project.hissab.domain.usecase

import com.project.hissab.domain.model.BudgetInputError
import com.project.hissab.domain.model.SetBudgetResult
import com.project.hissab.domain.repository.BudgetRepository
import java.time.Clock
import java.time.YearMonth

/** Sets the budget for the CURRENT month. Later months carry it forward until changed. */
class SetMonthlyBudgetUseCase(
    private val repository: BudgetRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    suspend operator fun invoke(amount: Double?): SetBudgetResult {
        if (amount == null || amount.isNaN() || amount.isInfinite() || amount <= 0.0) {
            return SetBudgetResult.Invalid(BudgetInputError.INVALID)
        }
        if (amount > MAX_BUDGET) return SetBudgetResult.Invalid(BudgetInputError.TOO_LARGE)
        repository.setBudget(YearMonth.now(clock), amount)
        return SetBudgetResult.Success
    }

    private companion object { const val MAX_BUDGET = 100_000_000.0 }   // ₹10 crore
}