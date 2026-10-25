package com.project.hissab.data

import com.project.hissab.domain.model.Category
import com.project.hissab.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultCategoryRepository : CategoryRepository {
    private val categories = MutableStateFlow(
        listOf(
            Category("grocery", "Grocery", "🛒", 0xFF2F8F5B.toInt()),
            Category("transport", "Transport", "🚌", 0xFF3B82F6.toInt()),
            Category("dining", "Dining", "🍴", 0xFF8B5CF6.toInt()),
            Category("shopping", "Shopping", "🛍️", 0xFFEC4899.toInt()),
            Category("fuel", "Fuel", "⛽", 0xFFEF4444.toInt()),
            Category("bills", "Bills", "🧾", 0xFFF59E0B.toInt())
        )
    )

    override fun observeCategories(): Flow<List<Category>> = categories.asStateFlow()
}