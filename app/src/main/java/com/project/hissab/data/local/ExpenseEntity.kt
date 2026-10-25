package com.project.hissab.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row of the "expenses" table. Storage shape only; the app uses the domain Expense. */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "date_time") val dateTime: Long,          // seconds, see ExpenseMapper
    @ColumnInfo(name = "is_unnecessary") val isUnnecessary: Boolean
)