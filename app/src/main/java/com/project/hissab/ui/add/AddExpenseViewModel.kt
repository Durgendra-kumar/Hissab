package com.project.hissab.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.project.hissab.HissabApp
import com.project.hissab.domain.model.AddCategoryResult
import com.project.hissab.domain.model.AddExpenseResult
import com.project.hissab.domain.model.Category
import com.project.hissab.domain.model.CategoryInputError
import com.project.hissab.domain.model.ExpenseDraft
import com.project.hissab.domain.model.ExpenseField
import com.project.hissab.domain.model.NewCategoryDraft
import com.project.hissab.domain.model.PaymentMethod
import com.project.hissab.domain.repository.CategoryRepository
import com.project.hissab.domain.usecase.AddCategoryUseCase
import com.project.hissab.domain.usecase.AddExpenseUseCase
import com.project.hissab.domain.usecase.GetExpenseUseCase
import com.project.hissab.domain.usecase.UpdateExpenseUseCase
import java.time.LocalDateTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddExpenseUiState(
    val title: String = "",
    val amountText: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String = Category.NONE_ID,      // new expenses start as "No category"
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val isUnnecessary: Boolean = false,
    val errors: Set<ExpenseField> = emptySet(),
    val isSaving: Boolean = false,
    val isEditing: Boolean = false
)

sealed interface AddExpenseEvent {
    data class Saved(val isEdit: Boolean) : AddExpenseEvent
    object NotFound : AddExpenseEvent
    object CategoryAdded : AddExpenseEvent
    data class CategoryRejected(val error: CategoryInputError) : AddExpenseEvent
}

/** expenseId == null means "add a new expense"; otherwise the expense with that id is edited. */
class AddExpenseViewModel(
    private val expenseId: Long?,
    private val addExpense: AddExpenseUseCase,
    private val updateExpense: UpdateExpenseUseCase,
    private val getExpense: GetExpenseUseCase,
    private val addCategory: AddCategoryUseCase,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState(isEditing = expenseId != null))
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddExpenseEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        if (expenseId != null) loadExpense(expenseId)
    }

    private fun loadExpense(id: Long) = viewModelScope.launch {
        val e = getExpense(id)
        if (e == null) { _events.send(AddExpenseEvent.NotFound); return@launch }
        _uiState.update {
            it.copy(
                title = e.title,
                amountText = if (e.amount % 1.0 == 0.0) e.amount.toLong().toString() else e.amount.toString(),
                selectedCategoryId = e.categoryId,
                paymentMethod = e.paymentMethod,
                dateTime = e.dateTime,
                isUnnecessary = e.isUnnecessary
            )
        }
    }

    fun onTitleChanged(value: String) = _uiState.update { it.copy(title = value, errors = it.errors - ExpenseField.TITLE) }
    fun onAmountChanged(value: String) = _uiState.update { it.copy(amountText = value, errors = it.errors - ExpenseField.AMOUNT) }
    fun onPaymentSelected(method: PaymentMethod) = _uiState.update { it.copy(paymentMethod = method) }
    fun onCategorySelected(id: String) = _uiState.update { it.copy(selectedCategoryId = id, errors = it.errors - ExpenseField.CATEGORY) }
    fun onDateTimeChanged(value: LocalDateTime) = _uiState.update { it.copy(dateTime = value) }
    fun onUnnecessaryToggled(value: Boolean) = _uiState.update { it.copy(isUnnecessary = value) }

    /** Creates a custom category and selects it. Rejections go back to the pop-up as an event. */
    fun onAddCategory(name: String, icon: String) {
        viewModelScope.launch {
            when (val result = addCategory(NewCategoryDraft(name, icon))) {
                is AddCategoryResult.Success -> {
                    _uiState.update { it.copy(selectedCategoryId = result.category.id) }
                    _events.send(AddExpenseEvent.CategoryAdded)
                }
                is AddCategoryResult.Invalid -> _events.send(AddExpenseEvent.CategoryRejected(result.error))
            }
        }
    }

    fun onSaveClicked() {
        val s = _uiState.value
        if (s.isSaving) return
        val draft = ExpenseDraft(
            title = s.title,
            amount = s.amountText.replace(",", "").toDoubleOrNull(),
            paymentMethod = s.paymentMethod,
            categoryId = s.selectedCategoryId,
            dateTime = s.dateTime,
            isUnnecessary = s.isUnnecessary
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = if (expenseId == null) addExpense(draft) else updateExpense(expenseId, draft)
            when (result) {
                is AddExpenseResult.Success -> _events.send(AddExpenseEvent.Saved(isEdit = expenseId != null))
                is AddExpenseResult.Invalid -> _uiState.update { it.copy(errors = result.errors, isSaving = false) }
            }
        }
    }

    companion object {
        const val ARG_EXPENSE_ID = "expenseId"

        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as HissabApp).container
                val id = createSavedStateHandle().get<Long>(ARG_EXPENSE_ID)
                AddExpenseViewModel(id, c.addExpense, c.updateExpense, c.getExpense, c.addCategory, c.categoryRepository)
            }
        }
    }
}