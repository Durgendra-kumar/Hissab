package com.project.hissab.domain.repository

import com.project.hissab.domain.model.Category
import com.project.hissab.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

interface ExpenseRepository {
    fun observeExpenses(): Flow<List<Expense>>
    suspend fun getExpense(id: Long): Expense?
    suspend fun addExpense(expense: Expense): Long
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(id: Long)
    suspend fun deleteExpensesInMonth(month: YearMonth): Int
}

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
}

interface BudgetRepository {
    /** Budgets the user set explicitly, by month. Months without an entry carry the latest earlier one. */
    fun observeBudgets(): Flow<Map<YearMonth, Double>>
    suspend fun setBudget(month: YearMonth, amount: Double)
}