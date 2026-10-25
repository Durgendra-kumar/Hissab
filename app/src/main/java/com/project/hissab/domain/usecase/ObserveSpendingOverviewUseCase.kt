package com.project.hissab.domain.usecase

import com.project.hissab.domain.model.BudgetStatus
import com.project.hissab.domain.model.PaymentMethod
import com.project.hissab.domain.model.SpendingOverview
import com.project.hissab.domain.repository.BudgetRepository
import com.project.hissab.domain.repository.ExpenseRepository
import java.time.Clock
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveSpendingOverviewUseCase(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    operator fun invoke(): Flow<SpendingOverview> =
        combine(expenseRepository.observeExpenses(), budgetRepository.observeBudgets()) { all, budgets ->
            val month = YearMonth.now(clock)
            val list = all.inMonth(month)
            fun total(vararg methods: PaymentMethod) =
                list.filter { it.paymentMethod in methods }.sumOf { it.amount }

            SpendingOverview(
                month = month,
                budget = BudgetStatus(limit = budgets.budgetFor(month), spent = list.sumOf { it.amount }),
                upi = total(PaymentMethod.UPI),
                cash = total(PaymentMethod.CASH),
                creditCard = total(PaymentMethod.CREDIT_CARD)
            )
        }
}