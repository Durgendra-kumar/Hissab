package com.project.hissab.domain.usecase

import com.project.hissab.domain.model.ExpenseDraft
import com.project.hissab.domain.model.ExpenseField

/** Single job: decide which fields of a draft are invalid. */
class ExpenseValidator {
    fun validate(draft: ExpenseDraft): Set<ExpenseField> = buildSet {
        if (draft.title.isBlank()) add(ExpenseField.TITLE)
        val amount = draft.amount
        if (amount == null || amount <= 0.0 || amount.isNaN() || amount.isInfinite()) add(ExpenseField.AMOUNT)
        if (draft.categoryId == null) add(ExpenseField.CATEGORY)
    }
}