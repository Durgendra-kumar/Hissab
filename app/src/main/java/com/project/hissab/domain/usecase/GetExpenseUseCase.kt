package com.project.hissab.domain.usecase

import com.project.hissab.domain.model.Expense
import com.project.hissab.domain.repository.ExpenseRepository

class GetExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(id: Long): Expense? = repository.getExpense(id)
}