package com.project.hissab.domain.usecase

import com.project.hissab.domain.model.Expense
import com.project.hissab.domain.model.ExpenseDraft

/** Turns an already-validated draft into an Expense. Returns null if a required field is missing. */
internal fun ExpenseDraft.toExpenseOrNull(id: Long = 0): Expense? {
    val amount = amount ?: return null
    val categoryId = categoryId ?: return null
    return Expense(id, title.trim(), amount, paymentMethod, categoryId, dateTime, isUnnecessary)
}