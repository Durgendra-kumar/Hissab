package com.project.hissab.ui.home

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.hissab.R
import com.project.hissab.databinding.ItemDateHeaderBinding
import com.project.hissab.databinding.ItemExpenseBinding

/** Shows day headers and expense rows. Shared by Home and the month detail screen. */
class ExpenseAdapter(
    private val onLongClick: (ExpenseItemUi) -> Unit
) : ListAdapter<HomeRow, RecyclerView.ViewHolder>(Diff) {

    class ItemHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)
    class HeaderHolder(val binding: ItemDateHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is HomeRow.Header -> TYPE_HEADER
        is HomeRow.Item -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) HeaderHolder(ItemDateHeaderBinding.inflate(inflater, parent, false))
        else ItemHolder(ItemExpenseBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is HomeRow.Header -> (holder as HeaderHolder).binding.tvDateHeader.text = row.label
            is HomeRow.Item -> bindItem((holder as ItemHolder).binding, row.item)
        }
    }

    private fun bindItem(b: ItemExpenseBinding, item: ExpenseItemUi) = with(b) {
        tvIcon.text = item.icon
        tvIcon.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(item.iconColor, 0x55))
        tvName.text = item.title
        tvTime.text = item.dateText
        tvAmount.text = item.amountText
        tvBadge.text = item.paymentLabel
        rowRoot.setBackgroundResource(if (item.isUnnecessary) R.drawable.bg_unnecessary else 0)
        rowRoot.setOnLongClickListener { onLongClick(item); true }
    }

    private object Diff : DiffUtil.ItemCallback<HomeRow>() {
        override fun areItemsTheSame(a: HomeRow, b: HomeRow) = when {
            a is HomeRow.Header && b is HomeRow.Header -> a.key == b.key
            a is HomeRow.Item && b is HomeRow.Item -> a.item.id == b.item.id
            else -> false
        }
        override fun areContentsTheSame(a: HomeRow, b: HomeRow) = a == b
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }
}