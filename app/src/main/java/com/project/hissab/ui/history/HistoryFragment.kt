package com.project.hissab.ui.history

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.hissab.R
import com.project.hissab.databinding.FragmentHistoryBinding
import com.project.hissab.util.collectIn
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels { HistoryViewModel.Factory }
    private val adapter = MonthHistoryAdapter(onClick = ::openMonth, onLongClick = ::confirmDeleteMonth)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        viewModel.uiState.collectIn(viewLifecycleOwner) { state ->
            adapter.submitList(state.items)
            binding.tvEmptyHistory.isVisible = state.isLoaded && state.items.isEmpty()
        }
        viewModel.events.collectIn(viewLifecycleOwner) { event ->
            if (event is HistoryEvent.MonthDeleted) {
                val what = if (event.count == 1) "1 expense" else "${event.count} expenses"
                Toast.makeText(requireContext(), "Deleted $what from ${event.monthTitle}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openMonth(item: MonthItemUi) =
        startActivity(MonthDetailActivity.intent(requireContext(), item.monthKey))

    private fun confirmDeleteMonth(item: MonthItemUi) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete ${item.title}?")
            .setMessage(
                "This permanently deletes ${item.countText} (${item.spentText}) from ${item.title}. " +
                        "The budget for that month is kept. This can't be undone."
            )
            .setPositiveButton("Delete month") { _, _ -> viewModel.onDeleteMonthConfirmed(item.monthKey) }
            .setNegativeButton("Keep it", null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}