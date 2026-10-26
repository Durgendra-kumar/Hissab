package com.project.hissab.domain.model

import java.time.LocalDateTime
import java.time.YearMonth

enum class PaymentMethod { UPI, CASH, CREDIT_CARD }

data class Category(val id: String, val name: String, val icon: String, val color: Int) {
    companion object {
        const val NONE_ID = "none"

        /** Built-in and always available. New expenses start with this one. */
        val NONE = Category(NONE_ID, "No category", "🏷️", 0xFF9AA3B5.toInt())

        /** Shown for an expense whose category can't be found. */
        val UNKNOWN = NONE
    }
}

data class Expense(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val paymentMethod: PaymentMethod,
    val categoryId: String,
    val dateTime: LocalDateTime,
    val isUnnecessary: Boolean
)

/** Raw user input, before validation. */
data class ExpenseDraft(
    val title: String,
    val amount: Double?,
    val paymentMethod: PaymentMethod,
    val categoryId: String?,
    val dateTime: LocalDateTime,
    val isUnnecessary: Boolean
)

enum class ExpenseField { TITLE, AMOUNT, CATEGORY }

sealed interface AddExpenseResult {
    data class Success(val id: Long) : AddExpenseResult
    data class Invalid(val errors: Set<ExpenseField>) : AddExpenseResult
}

data class ExpenseSummary(
    val month: YearMonth,
    val expenses: List<Expense>,
    val total: Double,
    val totalsByPayment: Map<PaymentMethod, Double>,
    val unnecessaryTotal: Double
) {
    fun totalFor(method: PaymentMethod) = totalsByPayment[method] ?: 0.0
}

data class CategoryTotal(val categoryId: String, val amount: Double)

data class StatsData(
    val month: YearMonth,
    val monthTotal: Double,
    val categoryTotals: List<CategoryTotal>,   // biggest first
    val recentMonths: List<MonthTotal>         // oldest to newest, last one is the current month
)

enum class BudgetLevel { SAFE, WARNING, DANGER }

data class BudgetStatus(
    val limit: Double?,
    val spent: Double,
    val carriedFrom: YearMonth? = null      // set when this month has no budget of its own
) {
    val isSet: Boolean get() = (limit ?: 0.0) > 0.0
    val ratio: Double get() = limit?.takeIf { it > 0.0 }?.let { spent / it } ?: 0.0
    val remaining: Double get() = (limit ?: 0.0) - spent

    /** Green under 70%, orange 70-90%, red over 90%. */
    val level: BudgetLevel
        get() = when {
            ratio > 0.9 -> BudgetLevel.DANGER
            ratio >= 0.7 -> BudgetLevel.WARNING
            else -> BudgetLevel.SAFE
        }
}

data class MonthTotal(
    val month: YearMonth,
    val total: Double,
    val budget: Double? = null,
    val expenseCount: Int = 0
) {
    val hasBudget: Boolean get() = (budget ?: 0.0) > 0.0
    val isOverBudget: Boolean get() = hasBudget && total > (budget ?: 0.0)
}

data class MonthDetail(val summary: MonthTotal, val expenses: List<Expense>)

enum class BudgetInputError { INVALID, TOO_LARGE }

sealed interface SetBudgetResult {
    object Success : SetBudgetResult
    data class Invalid(val reason: BudgetInputError) : SetBudgetResult
}

/** This month's spending against its budget, split by payment method. Paytm is counted under UPI. */
data class SpendingOverview(
    val month: YearMonth,
    val budget: BudgetStatus,
    val upi: Double,
    val cash: Double,
    val creditCard: Double
) {
    val spent: Double get() = budget.spent
}

data class NewCategoryDraft(val name: String, val icon: String)

enum class CategoryInputError { EMPTY_NAME, NAME_TOO_LONG, DUPLICATE_NAME }

sealed interface AddCategoryResult {
    data class Success(val category: Category) : AddCategoryResult
    data class Invalid(val error: CategoryInputError) : AddCategoryResult
}