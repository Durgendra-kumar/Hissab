package com.project.hissab.ui.history

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.hissab.databinding.ActivityMonthDetailBinding
import com.project.hissab.ui.home.ExpenseAdapter
import com.project.hissab.util.collectIn

/** Read-only list of one past month, using the same row layout as Home. */
class MonthDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonthDetailBinding
    private val viewModel: MonthDetailViewModel by viewModels { MonthDetailViewModel.Factory }
    private val adapter = ExpenseAdapter(onLongClick = {})

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonthDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.rvMonthExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvMonthExpenses.adapter = adapter

        viewModel.uiState.collectIn(this) { state ->
            binding.tvDetailTitle.text = state.title
            binding.tvDetailAmounts.text = state.amountsText
            binding.tvDetailAmounts.setTextColor(getColor(state.status.colorRes()))
            adapter.submitList(state.items)
            binding.tvEmptyDetail.isVisible = state.isLoaded && state.items.isEmpty()
        }
    }

    companion object {
        fun intent(context: Context, monthKey: String) =
            Intent(context, MonthDetailActivity::class.java)
                .putExtra(MonthDetailViewModel.ARG_MONTH, monthKey)
    }
}