package com.project.hissab.domain.usecase

import com.project.hissab.domain.repository.ExpenseRepository

class DeleteExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteExpense(id)
}