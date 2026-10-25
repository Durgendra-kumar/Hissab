package com.project.hissab.domain.usecase

import com.project.hissab.domain.model.AddExpenseResult
import com.project.hissab.domain.model.ExpenseDraft
import com.project.hissab.domain.repository.ExpenseRepository

class UpdateExpenseUseCase(
    private val repository: ExpenseRepository,
    private val validator: ExpenseValidator
) {
    suspend operator fun invoke(id: Long, draft: ExpenseDraft): AddExpenseResult {
        val errors = validator.validate(draft)
        val expense = draft.toExpenseOrNull(id)
        if (errors.isNotEmpty() || expense == null) return AddExpenseResult.Invalid(errors)
        repository.updateExpense(expense)
        return AddExpenseResult.Success(id)
    }
}