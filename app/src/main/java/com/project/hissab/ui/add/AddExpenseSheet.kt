package com.project.hissab.ui.add

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.project.hissab.R
import com.project.hissab.databinding.SheetAddExpenseBinding
import com.project.hissab.domain.model.Category
import com.project.hissab.domain.model.CategoryInputError
import com.project.hissab.domain.model.ExpenseField
import com.project.hissab.domain.model.PaymentMethod
import com.project.hissab.util.Formatters
import com.project.hissab.util.collectIn
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.LocalDateTime
import android.content.Context
import android.view.inputmethod.InputMethodManager

/** Add mode by default; pass an expense id to open it in Edit mode. */
class AddExpenseSheet : BottomSheetDialogFragment() {

    private var _binding: SheetAddExpenseBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddExpenseViewModel by viewModels { AddExpenseViewModel.Factory }

    private val categoryAdapter = CategoryTileAdapter(
        onSelect = { dismissKeyboard(); viewModel.onCategorySelected(it) },
        onAdd = { dismissKeyboard(); showNewCategoryDialog() }
    )

    private var newCategoryDialog: NewCategoryDialog? = null
    private var scrollToSelected = false      // bring the selected tile into view after the next update
    private var initialScrollDone = false     // do that once when the sheet opens

    private val chipIds = mapOf(
        PaymentMethod.UPI to R.id.chipUpi,
        PaymentMethod.CASH to R.id.chipCash,
        PaymentMethod.CREDIT_CARD to R.id.chipCard
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        SheetAddExpenseBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindInputs()
        viewModel.uiState.collectIn(viewLifecycleOwner, ::render)
        viewModel.events.collectIn(viewLifecycleOwner) { event ->
            when (event) {
                is AddExpenseEvent.Saved -> {
                    toast(if (event.isEdit) "Expense updated" else "Expense added")
                    dismiss()
                }
                AddExpenseEvent.NotFound -> { toast("This expense no longer exists"); dismiss() }
                AddExpenseEvent.CategoryAdded -> newCategoryDialog?.dismiss()
                is AddExpenseEvent.CategoryRejected -> {
                    scrollToSelected = false
                    newCategoryDialog?.showError(event.error.message())
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (view?.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun bindInputs() = with(binding) {
        root.isFocusableInTouchMode = true
        root.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        etName.doAfterTextChanged { viewModel.onTitleChanged(it?.toString().orEmpty()) }
        etAmount.doAfterTextChanged { viewModel.onAmountChanged(it?.toString().orEmpty()) }
        chipGroupPay.setOnCheckedStateChangeListener { _, ids ->
            chipIds.entries.firstOrNull { it.value == ids.firstOrNull() }?.let { viewModel.onPaymentSelected(it.key) }
        }
        btnDateTime.setOnClickListener {
            dismissKeyboard()
            pickDateTime(viewModel.uiState.value.dateTime)
        }
        chipIds.values.forEach { id -> root.findViewById<View>(id).setOnClickListener { dismissKeyboard() } }
        switchUnnecessary.setOnClickListener { dismissKeyboard() }

        switchUnnecessary.setOnCheckedChangeListener { _, checked -> viewModel.onUnnecessaryToggled(checked) }
        btnSave.setOnClickListener { viewModel.onSaveClicked() }

        // Category picker: 3 rows, scrolls sideways
        rvCategories.layoutManager = GridLayoutManager(requireContext(), CATEGORY_ROWS, GridLayoutManager.HORIZONTAL, false)
        rvCategories.adapter = categoryAdapter
        rvCategories.itemAnimator = null
    }

    private fun render(state: AddExpenseUiState) = with(binding) {
        tvSheetTitle.text = if (state.isEditing) "Edit Expense" else "Add New Expense"
        btnSave.text = if (state.isEditing) "Save changes" else "Add expense"
        btnSave.isEnabled = !state.isSaving

        // Only write to the fields when the text really differs, so typing never jumps the cursor.
        if (etName.text?.toString() != state.title) { etName.setText(state.title); etName.setSelection(state.title.length) }
        if (etAmount.text?.toString() != state.amountText) { etAmount.setText(state.amountText); etAmount.setSelection(state.amountText.length) }
        etName.error = if (ExpenseField.TITLE in state.errors) "Enter a merchant note" else null
        etAmount.error = if (ExpenseField.AMOUNT in state.errors) "Enter an amount above 0" else null

        btnDateTime.text = "🗓  " + Formatters.dateTime(state.dateTime)
        chipIds[state.paymentMethod]?.let { if (chipGroupPay.checkedChipId != it) chipGroupPay.check(it) }
        if (switchUnnecessary.isChecked != state.isUnnecessary) switchUnnecessary.isChecked = state.isUnnecessary

        renderCategories(state)
    }

    private fun renderCategories(state: AddExpenseUiState) {
        val tiles = state.categories.map { CategoryTile.Item(it, it.id == state.selectedCategoryId) } + CategoryTile.Add

        // When the sheet opens on an expense with a category further along the grid, scroll to it once.
        if (!initialScrollDone && state.selectedCategoryId != Category.NONE_ID) {
            initialScrollDone = true
            scrollToSelected = true
        }

        categoryAdapter.submitList(tiles) {
            if (scrollToSelected) {
                val index = tiles.indexOfFirst { it is CategoryTile.Item && it.selected }
                if (index >= 0) {
                    scrollToSelected = false
                    _binding?.rvCategories?.smoothScrollToPosition(index)
                }
            }
        }
    }

    private fun showNewCategoryDialog() {
        newCategoryDialog?.dismiss()
        newCategoryDialog = NewCategoryDialog(requireContext()) { name, icon ->
            scrollToSelected = true       // the new category becomes selected; show it
            viewModel.onAddCategory(name, icon)
        }.also { it.show() }
    }

    private fun CategoryInputError.message() = when (this) {
        CategoryInputError.EMPTY_NAME -> "Enter a name"
        CategoryInputError.NAME_TOO_LONG -> "That name is too long"
        CategoryInputError.DUPLICATE_NAME -> "A category with this name already exists"
    }

    private fun pickDateTime(current: LocalDateTime) {
        DatePickerDialog(requireContext(), { _, y, m, d ->
            TimePickerDialog(requireContext(), { _, h, min ->
                viewModel.onDateTimeChanged(LocalDateTime.of(y, m + 1, d, h, min))
            }, current.hour, current.minute, false).show()
        }, current.year, current.monthValue - 1, current.dayOfMonth).show()
    }

    private fun toast(text: String) = Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        newCategoryDialog?.dismiss()
        newCategoryDialog = null
        initialScrollDone = false
        scrollToSelected = false
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "AddExpenseSheet"
        private const val CATEGORY_ROWS = 2

        fun newInstance(expenseId: Long? = null) = AddExpenseSheet().apply {
            arguments = expenseId?.let { bundleOf(AddExpenseViewModel.ARG_EXPENSE_ID to it) }
        }
    }
    private fun dismissKeyboard() {
        val root = _binding?.root ?: return
        root.requestFocus()      // the sheet itself takes focus, so no text field gets it back
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(root.windowToken, 0)
    }
}