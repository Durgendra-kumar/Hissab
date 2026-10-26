package com.project.hissab.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per CUSTOM category. The built-in ones live in code. */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val color: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long
)