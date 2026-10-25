package com.project.hissab.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.project.hissab.HissabApp
import com.project.hissab.domain.model.BudgetInputError
import com.project.hissab.domain.model.BudgetLevel
import com.project.hissab.domain.model.BudgetStatus
import com.project.hissab.domain.model.SetBudgetResult
import com.project.hissab.domain.usecase.ObserveBudgetStatusUseCase
import com.project.hissab.domain.usecase.SetMonthlyBudgetUseCase
import com.project.hissab.util.Formatters
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetUiState(
    val isSet: Boolean = false,
    val limitText: String = "Not set",
    val statusText: String = "Set a monthly limit to track your spending",
    val spentText: String = "₹0",
    val remainingLabel: String = "Remaining",
    val remainingText: String = "–",
    val progress: Int = 0,
    val level: BudgetLevel = BudgetLevel.SAFE,
    val input: String = "",
    val inputError: String? = null,
    val noteText: String? = null
)

sealed interface BudgetEvent { object Saved : BudgetEvent }

/** Domain numbers in, display strings out. */
class BudgetUiMapper {
    fun map(status: BudgetStatus, input: String, error: BudgetInputError?): BudgetUiState {
        val message = when (error) {
            BudgetInputError.INVALID -> "Enter a limit above ₹0"
            BudgetInputError.TOO_LARGE -> "That limit is too large"
            null -> null
        }
        val spent = Formatters.rupees(status.spent, false)
        val limit = status.limit
        if (!status.isSet || limit == null) return BudgetUiState(spentText = spent, input = input, inputError = message)

        val percent = (status.ratio * 100).roundToInt()
        val over = status.remaining < 0
        return BudgetUiState(
            isSet = true,
            limitText = Formatters.rupees(limit, false),
            statusText = if (over) "Over budget, $percent% used" else "$percent% of your budget used",
            spentText = spent,
            remainingLabel = if (over) "Over by" else "Remaining",
            remainingText = Formatters.rupees(abs(status.remaining), false),
            progress = percent.coerceIn(0, 100),
            level = status.level,
            input = input,
            inputError = message,
            noteText = status.carriedFrom?.let { "Carried over from ${Formatters.month(it)}" }
        )
    }
}

class BudgetViewModel(
    observeStatus: ObserveBudgetStatusUseCase,
    private val setBudget: SetMonthlyBudgetUseCase,
    mapper: BudgetUiMapper
) : ViewModel() {

    private val input = MutableStateFlow("")
    private val error = MutableStateFlow<BudgetInputError?>(null)

    private val _events = Channel<BudgetEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<BudgetUiState> =
        combine(observeStatus(), input, error, mapper::map)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    init {
        // Fill the field with the saved limit once, so the user can edit it.
        viewModelScope.launch {
            observeStatus().first().limit?.let { input.value = it.toLong().toString() }
        }
    }

    fun onInputChanged(text: String) { input.value = text; error.value = null }

    fun onSaveClicked() {
        val amount = input.value.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
        viewModelScope.launch {
            when (val result = setBudget(amount)) {
                SetBudgetResult.Success -> _events.send(BudgetEvent.Saved)
                is SetBudgetResult.Invalid -> error.value = result.reason
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as HissabApp).container
                BudgetViewModel(c.observeBudgetStatus, c.setMonthlyBudget, BudgetUiMapper())
            }
        }
    }
}