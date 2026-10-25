package com.project.hissab.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.project.hissab.HissabApp
import com.project.hissab.domain.model.Category
import com.project.hissab.domain.repository.CategoryRepository
import com.project.hissab.domain.usecase.ObserveMonthDetailUseCase
import com.project.hissab.ui.home.ExpenseItemUi
import com.project.hissab.ui.home.HomeUiMapper
import com.project.hissab.util.Formatters
import java.time.YearMonth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.project.hissab.ui.home.HomeRow

data class MonthDetailUiState(
    val title: String = "",
    val amountsText: String = "",
    val status: SpendStatus = SpendStatus.NO_BUDGET,
    val items: List<HomeRow> = emptyList(),
    val isLoaded: Boolean = false
)

class MonthDetailViewModel(
    month: YearMonth,
    observeDetail: ObserveMonthDetailUseCase,
    categoryRepository: CategoryRepository,
    historyMapper: HistoryUiMapper,
    itemMapper: HomeUiMapper          // same row formatting as the Home list
) : ViewModel() {

    val uiState: StateFlow<MonthDetailUiState> =
        combine(observeDetail(month), categoryRepository.observeCategories()) { detail, categories ->
            MonthDetailUiState(
                title = Formatters.month(month),
                amountsText = historyMapper.amountsText(detail.summary),
                status = historyMapper.statusOf(detail.summary),
                items = itemMapper.rowsFor(detail.expenses, categories),
                isLoaded = true
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthDetailUiState(title = Formatters.month(month)))

    companion object {
        const val ARG_MONTH = "month"

        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as HissabApp).container
                val raw = createSavedStateHandle().get<String>(ARG_MONTH)
                val month = raw?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: YearMonth.now()
                MonthDetailViewModel(month, c.observeMonthDetail, c.categoryRepository, HistoryUiMapper(), HomeUiMapper())
            }
        }
    }
}