package com.project.hissab.ui.add

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.hissab.R
import com.project.hissab.databinding.ItemCategoryBinding
import com.project.hissab.domain.model.Category

/** One cell of the category grid: a real category, or the "+ Custom" button at the end. */
sealed interface CategoryTile {
    data class Item(val category: Category, val selected: Boolean) : CategoryTile
    object Add : CategoryTile
}

class CategoryTileAdapter(
    private val onSelect: (String) -> Unit,
    private val onAdd: () -> Unit
) : ListAdapter<CategoryTile, CategoryTileAdapter.Holder>(Diff) {

    class Holder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val b = holder.binding
        val context = b.root.context
        when (val tile = getItem(position)) {
            is CategoryTile.Item -> {
                val category = tile.category
                b.tvCatIcon.text = category.icon
                b.tvCatIcon.backgroundTintList =
                    ColorStateList.valueOf(ColorUtils.setAlphaComponent(category.color, 0x55))
                b.tvCatName.text = category.name
                b.tvCatName.setTextColor(context.getColor(if (tile.selected) R.color.text else R.color.mute))
                b.root.setBackgroundResource(if (tile.selected) R.drawable.bg_category_selected else 0)
                b.root.setOnClickListener { onSelect(category.id) }
            }
            CategoryTile.Add -> {
                b.tvCatIcon.text = "+"
                b.tvCatIcon.backgroundTintList =
                    ColorStateList.valueOf(ColorUtils.setAlphaComponent(context.getColor(R.color.teal), 0x88))
                b.tvCatName.text = "Custom"
                b.tvCatName.setTextColor(context.getColor(R.color.mute))
                b.root.setBackgroundResource(0)
                b.root.setOnClickListener { onAdd() }
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<CategoryTile>() {
        override fun areItemsTheSame(a: CategoryTile, b: CategoryTile) = when {
            a is CategoryTile.Item && b is CategoryTile.Item -> a.category.id == b.category.id
            else -> a === b
        }
        override fun areContentsTheSame(a: CategoryTile, b: CategoryTile) = a == b
    }
}