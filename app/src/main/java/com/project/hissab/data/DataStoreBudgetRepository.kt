package com.project.hissab.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.project.hissab.domain.repository.BudgetRepository
import java.io.IOException
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.budgetDataStore by preferencesDataStore(name = "budget")

/** One entry per month, stored as "budget_2026-10" = amount. */
class DataStoreBudgetRepository(context: Context) : BudgetRepository {
    private val store = context.applicationContext.budgetDataStore

    override fun observeBudgets(): Flow<Map<YearMonth, Double>> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs.asMap().mapNotNull { (key, value) ->
                val month = key.name.takeIf { it.startsWith(PREFIX) }
                    ?.removePrefix(PREFIX)
                    ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
                val amount = value as? Double
                if (month != null && amount != null) month to amount else null
            }.toMap()
        }

    override suspend fun setBudget(month: YearMonth, amount: Double) {
        store.edit { it[doublePreferencesKey(PREFIX + month)] = amount }
    }

    private companion object { const val PREFIX = "budget_" }
}