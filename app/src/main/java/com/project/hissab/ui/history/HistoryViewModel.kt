package com.project.hissab.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.project.hissab.HissabApp
import com.project.hissab.domain.model.MonthTotal
import com.project.hissab.domain.usecase.DeleteMonthUseCase
import com.project.hissab.domain.usecase.ObserveHistoryUseCase
import com.project.hissab.util.Formatters
import java.time.YearMonth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SpendStatus { WITHIN_BUDGET, OVER_BUDGET, NO_BUDGET }

data class MonthItemUi(
    val monthKey: String,        // "2026-09", used to open or delete the month
    val title: String,
    val spentText: String,
    val amountsText: String,     // "₹24,850 / ₹40,000"
    val countText: String,
    val status: SpendStatus
)

data class HistoryUiState(val items: List<MonthItemUi> = emptyList(), val isLoaded: Boolean = false)

sealed interface HistoryEvent {
    data class MonthDeleted(val monthTitle: String, val count: Int) : HistoryEvent
}

/** Domain numbers in, display strings out. Shared with the month detail screen. */
class HistoryUiMapper {
    fun mapMonth(m: MonthTotal) = MonthItemUi(
        monthKey = m.month.toString(),
        title = Formatters.month(m.month),
        spentText = money(m.total),
        amountsText = amountsText(m),
        countText = if (m.expenseCount == 1) "1 expense" else "${m.expenseCount} expenses",
        status = statusOf(m)
    )

    fun amountsText(m: MonthTotal): String {
        val budget = m.budget
        return if (m.hasBudget && budget != null) "${money(m.total)} / ${money(budget)}"
        else "${money(m.total)} / No budget"
    }

    fun statusOf(m: MonthTotal) = when {
        !m.hasBudget -> SpendStatus.NO_BUDGET
        m.isOverBudget -> SpendStatus.OVER_BUDGET
        else -> SpendStatus.WITHIN_BUDGET
    }

    private fun money(v: Double) = Formatters.rupees(v, decimals = v % 1.0 != 0.0)
}

class HistoryViewModel(
    observeHistory: ObserveHistoryUseCase,
    private val deleteMonth: DeleteMonthUseCase,
    private val mapper: HistoryUiMapper
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = observeHistory()
        .map { months -> HistoryUiState(months.map(mapper::mapMonth), isLoaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    private val _events = Channel<HistoryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onDeleteMonthConfirmed(monthKey: String) {
        val month = runCatching { YearMonth.parse(monthKey) }.getOrNull() ?: return
        viewModelScope.launch {
            val count = deleteMonth(month)
            _events.send(HistoryEvent.MonthDeleted(Formatters.month(month), count))
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as HissabApp).container
                HistoryViewModel(c.observeHistory, c.deleteMonth, HistoryUiMapper())
            }
        }
    }
}