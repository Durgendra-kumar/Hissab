package com.project.hissab.domain.usecase

import com.project.hissab.domain.model.AddExpenseResult
import com.project.hissab.domain.model.Expense
import com.project.hissab.domain.model.ExpenseDraft
import com.project.hissab.domain.repository.ExpenseRepository

class AddExpenseUseCase(
    private val repository: ExpenseRepository,
    private val validator: ExpenseValidator
) {
    suspend operator fun invoke(draft: ExpenseDraft): AddExpenseResult {
        val errors = validator.validate(draft)
        val expense = draft.toExpenseOrNull()
        if (errors.isNotEmpty() || expense == null) return AddExpenseResult.Invalid(errors)
        return AddExpenseResult.Success(repository.addExpense(expense))
    }
}