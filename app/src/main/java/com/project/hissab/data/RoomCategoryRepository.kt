package com.project.hissab.data

import com.project.hissab.data.local.CategoryDao
import com.project.hissab.data.local.toDomain
import com.project.hissab.data.local.toEntity
import com.project.hissab.domain.model.BuiltInCategories
import com.project.hissab.domain.model.Category
import com.project.hissab.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCategoryRepository(private val dao: CategoryDao) : CategoryRepository {

    /** Built-in categories first, then the user's custom ones. */
    override fun observeCategories(): Flow<List<Category>> =
        dao.observeAll().map { rows -> BuiltInCategories.ALL + rows.map { it.toDomain() } }

    override suspend fun addCustomCategory(category: Category) =
        dao.insert(category.toEntity(createdAt = System.currentTimeMillis()))
}