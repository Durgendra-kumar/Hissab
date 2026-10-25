package com.project.hissab.ui.budgets

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.project.hissab.R
import com.project.hissab.databinding.FragmentBudgetsBinding
import com.project.hissab.domain.model.BudgetLevel
import com.project.hissab.util.collectIn

class BudgetsFragment : Fragment(R.layout.fragment_budgets) {

    private var _binding: FragmentBudgetsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BudgetViewModel by viewModels { BudgetViewModel.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBudgetsBinding.bind(view)

        binding.etBudget.doAfterTextChanged { viewModel.onInputChanged(it?.toString().orEmpty()) }
        binding.etBudget.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) revealFieldAboveKeyboard() }
        binding.etBudget.setOnClickListener { revealFieldAboveKeyboard() }
        binding.etBudget.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { viewModel.onSaveClicked(); true } else false
        }
        binding.btnSaveBudget.setOnClickListener { viewModel.onSaveClicked() }

        viewModel.uiState.collectIn(viewLifecycleOwner, ::render)
        viewModel.events.collectIn(viewLifecycleOwner) { event ->
            if (event is BudgetEvent.Saved) {
                Toast.makeText(requireContext(), "Budget saved", Toast.LENGTH_SHORT).show()
                binding.etBudget.clearFocus()
                WindowInsetsControllerCompat(requireActivity().window, view).hide(WindowInsetsCompat.Type.ime())
            }
        }
    }

    /** After the keyboard finishes opening, scroll so the field and the Save button are both on screen. */
    private fun revealFieldAboveKeyboard() {
        binding.btnSaveBudget.postDelayed({
            _binding?.btnSaveBudget?.let { button ->
                button.requestRectangleOnScreen(Rect(0, 0, button.width, button.height), true)
            }
        }, 300)
    }

    private fun render(state: BudgetUiState) = with(binding) {
        tvBudgetLimit.text = state.limitText
        tvBudgetStatus.text = state.statusText
        tvBudgetNote.text = state.noteText
        tvBudgetNote.isVisible = state.noteText != null
        tvSpent.text = state.spentText
        tvRemainingLabel.text = state.remainingLabel
        tvRemaining.text = state.remainingText
        progressBudget.setProgressCompat(state.progress, true)
        progressBudget.setIndicatorColor(requireContext().getColor(colorFor(state.level)))
        tilBudget.error = state.inputError
        // Only write when the text really differs, so typing never jumps the cursor.
        if (etBudget.text?.toString() != state.input) {
            etBudget.setText(state.input)
            etBudget.setSelection(state.input.length)
        }
    }

    private fun colorFor(level: BudgetLevel) = when (level) {
        BudgetLevel.SAFE -> R.color.green
        BudgetLevel.WARNING -> R.color.orange
        BudgetLevel.DANGER -> R.color.red
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}