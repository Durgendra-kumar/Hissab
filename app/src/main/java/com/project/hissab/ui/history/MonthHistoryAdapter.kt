package com.project.hissab.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.hissab.R
import com.project.hissab.databinding.ItemMonthBinding

/** Green when within budget, red when over, grey when the month had no budget. */
internal fun SpendStatus.colorRes() = when (this) {
    SpendStatus.WITHIN_BUDGET -> R.color.green
    SpendStatus.OVER_BUDGET -> R.color.red
    SpendStatus.NO_BUDGET -> R.color.mute
}

class MonthHistoryAdapter(
    private val onClick: (MonthItemUi) -> Unit,
    private val onLongClick: (MonthItemUi) -> Unit
) : ListAdapter<MonthItemUi, MonthHistoryAdapter.ViewHolder>(Diff) {

    class ViewHolder(val binding: ItemMonthBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemMonthBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvMonthTitle.text = item.title
            tvMonthCount.text = item.countText
            tvMonthAmounts.text = item.amountsText
            tvMonthAmounts.setTextColor(root.context.getColor(item.status.colorRes()))
            root.setOnClickListener { onClick(item) }
            root.setOnLongClickListener { onLongClick(item); true }
        }
    }

    private object Diff : DiffUtil.ItemCallback<MonthItemUi>() {
        override fun areItemsTheSame(a: MonthItemUi, b: MonthItemUi) = a.monthKey == b.monthKey
        override fun areContentsTheSame(a: MonthItemUi, b: MonthItemUi) = a == b
    }
}