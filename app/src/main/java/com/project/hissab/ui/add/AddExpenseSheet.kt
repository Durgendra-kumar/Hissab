package com.project.hissab.ui.add

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.project.hissab.R
import com.project.hissab.databinding.ItemCategoryBinding
import com.project.hissab.databinding.SheetAddExpenseBinding
import com.project.hissab.domain.model.ExpenseField
import com.project.hissab.domain.model.PaymentMethod
import com.project.hissab.util.Formatters
import com.project.hissab.util.collectIn
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.LocalDateTime

/** Add mode by default; pass an expense id to open it in Edit mode. */
class AddExpenseSheet : BottomSheetDialogFragment() {

    private var _binding: SheetAddExpenseBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddExpenseViewModel by viewModels { AddExpenseViewModel.Factory }

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
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (view?.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun bindInputs() = with(binding) {
        etName.doAfterTextChanged { viewModel.onTitleChanged(it?.toString().orEmpty()) }
        etAmount.doAfterTextChanged { viewModel.onAmountChanged(it?.toString().orEmpty()) }
        chipGroupPay.setOnCheckedStateChangeListener { _, ids ->
            chipIds.entries.firstOrNull { it.value == ids.firstOrNull() }?.let { viewModel.onPaymentSelected(it.key) }
        }
        btnDateTime.setOnClickListener { pickDateTime(viewModel.uiState.value.dateTime) }
        switchUnnecessary.setOnCheckedChangeListener { _, checked -> viewModel.onUnnecessaryToggled(checked) }
        btnSave.setOnClickListener { viewModel.onSaveClicked() }
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
        val container = binding.categoryContainer
        container.removeAllViews()
        state.categories.forEach { category ->
            val tile = ItemCategoryBinding.inflate(layoutInflater, container, false)
            val selected = category.id == state.selectedCategoryId
            tile.tvCatIcon.text = category.icon
            tile.tvCatIcon.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(category.color, 0x55))
            tile.tvCatName.text = category.name
            tile.tvCatName.setTextColor(requireContext().getColor(if (selected) R.color.text else R.color.mute))
            if (selected) tile.root.setBackgroundResource(R.drawable.bg_category_selected)
            tile.root.setOnClickListener { viewModel.onCategorySelected(category.id) }
            container.addView(tile.root)
        }
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
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "AddExpenseSheet"
        fun newInstance(expenseId: Long? = null) = AddExpenseSheet().apply {
            arguments = expenseId?.let { bundleOf(AddExpenseViewModel.ARG_EXPENSE_ID to it) }
        }
    }
}