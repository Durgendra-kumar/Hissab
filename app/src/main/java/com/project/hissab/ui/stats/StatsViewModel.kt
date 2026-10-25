package com.project.hissab.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.project.hissab.HissabApp
import com.project.hissab.domain.model.Category
import com.project.hissab.domain.model.StatsData
import com.project.hissab.domain.repository.CategoryRepository
import com.project.hissab.domain.usecase.ObserveStatsUseCase
import com.project.hissab.util.Formatters
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SliceUi(
    val name: String, val icon: String, val color: Int,
    val amount: Float, val amountText: String, val percentText: String
)

enum class BarStatus { WITHIN_BUDGET, OVER_BUDGET, NO_BUDGET }
data class BarUi(val label: String, val value: Float, val status: BarStatus)

data class StatsUiState(
    val monthText: String = "",
    val totalText: String = "₹0",
    val slices: List<SliceUi> = emptyList(),
    val bars: List<BarUi> = emptyList()
)

/** Domain numbers in, display values out. */
class StatsUiMapper {
    fun map(data: StatsData, categories: List<Category>): StatsUiState {
        val byId = categories.associateBy { it.id }
        val slices = data.categoryTotals.map { t ->
            val c = byId[t.categoryId] ?: Category.UNKNOWN
            val percent = if (data.monthTotal > 0) t.amount / data.monthTotal * 100 else 0.0
            SliceUi(c.name, c.icon, c.color, t.amount.toFloat(), Formatters.rupees(t.amount), "${percent.roundToInt()}%")
        }
        val bars = data.recentMonths.map {
            BarUi(
                label = Formatters.shortMonth(it.month),
                value = it.total.toFloat(),
                status = when {
                    !it.hasBudget -> BarStatus.NO_BUDGET
                    it.isOverBudget -> BarStatus.OVER_BUDGET
                    else -> BarStatus.WITHIN_BUDGET
                }
            )
        }
        return StatsUiState(Formatters.month(data.month), Formatters.rupees(data.monthTotal, false), slices, bars)
    }
}

class StatsViewModel(
    observeStats: ObserveStatsUseCase,
    categoryRepository: CategoryRepository,
    mapper: StatsUiMapper
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> =
        combine(observeStats(), categoryRepository.observeCategories(), mapper::map)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as HissabApp).container
                StatsViewModel(c.observeStats, c.categoryRepository, StatsUiMapper())
            }
        }
    }
}