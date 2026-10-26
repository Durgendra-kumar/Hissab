package com.project.hissab.ui.add

import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.project.hissab.databinding.DialogNewCategoryBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** "New category" pop-up. It stays open until the category is accepted, so errors show inside the field. */
class NewCategoryDialog(
    private val context: Context,
    private val onSubmit: (name: String, icon: String) -> Unit
) {
    private var dialog: AlertDialog? = null
    private var binding: DialogNewCategoryBinding? = null

    fun show() {
        val b = DialogNewCategoryBinding.inflate(LayoutInflater.from(context))
        b.etCategoryName.doAfterTextChanged { b.tilCategoryName.error = null }

        val d = MaterialAlertDialogBuilder(context)
            .setTitle("New category")
            .setView(b.root)
            .setPositiveButton("Create", null)       // handled below, so the dialog doesn't close on errors
            .setNegativeButton("Cancel", null)
            .create()
        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        d.setOnShowListener {
            b.etCategoryName.requestFocus()
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val icon = b.chipGroupIcons.findViewById<Chip>(b.chipGroupIcons.checkedChipId)?.text?.toString().orEmpty()
                onSubmit(b.etCategoryName.text?.toString().orEmpty(), icon)
            }
        }
        d.setOnDismissListener { dialog = null; binding = null }
        binding = b
        dialog = d
        d.show()
    }

    fun showError(message: String) { binding?.tilCategoryName?.error = message }

    fun dismiss() { dialog?.dismiss() }
}