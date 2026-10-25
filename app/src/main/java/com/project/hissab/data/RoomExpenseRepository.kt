package com.project.hissab.data

import com.project.hissab.data.local.ExpenseDao
import com.project.hissab.data.local.toDomain
import com.project.hissab.data.local.toEntity
import com.project.hissab.domain.model.Expense
import com.project.hissab.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import com.project.hissab.data.local.toEpochSecondRange

/** Same contract as the in-memory version, backed by the database. */
class RoomExpenseRepository(private val dao: ExpenseDao) : ExpenseRepository {

    override fun observeExpenses(): Flow<List<Expense>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getExpense(id: Long): Expense? = dao.getById(id)?.toDomain()
    override suspend fun addExpense(expense: Expense): Long = dao.insert(expense.toEntity())
    override suspend fun updateExpense(expense: Expense) = dao.update(expense.toEntity())
    override suspend fun deleteExpense(id: Long) = dao.deleteById(id)

    override suspend fun deleteExpensesInMonth(month: YearMonth): Int {
        val (start, end) = month.toEpochSecondRange()
        return dao.deleteBetween(start, end)
    }

}