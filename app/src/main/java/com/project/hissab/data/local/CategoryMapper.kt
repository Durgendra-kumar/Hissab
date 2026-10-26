package com.project.hissab.data.local

import com.project.hissab.domain.model.Category

fun CategoryEntity.toDomain() = Category(id, name, icon, color)

fun Category.toEntity(createdAt: Long) = CategoryEntity(id, name, icon, color, createdAt)