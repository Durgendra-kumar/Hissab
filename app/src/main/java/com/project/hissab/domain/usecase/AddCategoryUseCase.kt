package com.project.hissab.domain.usecase

import com.project.hissab.domain.model.AddCategoryResult
import com.project.hissab.domain.model.Category
import com.project.hissab.domain.model.CategoryInputError
import com.project.hissab.domain.model.NewCategoryDraft
import com.project.hissab.domain.repository.CategoryRepository
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AddCategoryUseCase(
    private val repository: CategoryRepository,
    private val newId: () -> String = { "custom_${UUID.randomUUID()}" }
) {
    private val lock = Mutex()      // two quick taps can't both pass the duplicate check

    suspend operator fun invoke(draft: NewCategoryDraft): AddCategoryResult = lock.withLock {
        val name = draft.name.trim().replace(Regex("\\s+"), " ")
        if (name.isEmpty()) return@withLock AddCategoryResult.Invalid(CategoryInputError.EMPTY_NAME)
        if (name.length > MAX_NAME_LENGTH) return@withLock AddCategoryResult.Invalid(CategoryInputError.NAME_TOO_LONG)

        val existing = repository.observeCategories().first()
        if (existing.any { it.name.equals(name, ignoreCase = true) }) {
            return@withLock AddCategoryResult.Invalid(CategoryInputError.DUPLICATE_NAME)
        }

        val category = Category(
            id = newId(),
            name = name,
            icon = draft.icon.ifBlank { DEFAULT_ICON },
            color = PALETTE[existing.size % PALETTE.size]
        )
        repository.addCustomCategory(category)
        AddCategoryResult.Success(category)
    }

    companion object {
        const val MAX_NAME_LENGTH = 20
        private const val DEFAULT_ICON = "🏷️"
        private val PALETTE = listOf(
            0xFF14B8A6, 0xFFF97316, 0xFF6366F1, 0xFF84CC16, 0xFFE11D48, 0xFF06B6D4, 0xFFD946EF
        ).map { it.toInt() }
    }
}