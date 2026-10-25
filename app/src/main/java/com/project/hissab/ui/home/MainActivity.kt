package com.project.hissab.ui.home

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.transition.TransitionManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.hissab.R
import com.project.hissab.databinding.ActivityMainBinding
import com.project.hissab.domain.model.BudgetLevel
import com.project.hissab.ui.add.AddExpenseSheet
import com.project.hissab.util.collectIn
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private enum class Tab { HOME, STATS, HISTORY, BUDGETS }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: HomeViewModel by viewModels { HomeViewModel.Factory }
    private val adapter = ExpenseAdapter(onLongClick = ::showExpenseActions)
    private var lastCount = 0
    private var barFraction = 0f
    private var barLevel = BudgetLevel.SAFE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = adapter
        binding.fabAdd.setOnClickListener {
            AddExpenseSheet.newInstance().show(supportFragmentManager, AddExpenseSheet.TAG)
        }
        setupNavigation()
        hideNavigationWhileTyping()
        showTab(Tab.HOME)

        viewModel.uiState.collectIn(this, ::render)
        viewModel.events.collectIn(this) { event ->
            if (event is HomeEvent.ExpenseDeleted) Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun render(state: HomeUiState) = with(binding) {
        tvMonth.text = state.monthText
        tvTotal.text = state.totalText
        tvUpi.text = state.upiText
        tvCash.text = state.cashText
        tvCard.text = state.cardText
        tvUnnecessary.text = state.unnecessaryText
        adapter.submitList(state.rows) {
            if (state.expenseCount > lastCount) rvExpenses.scrollToPosition(0)   // show the new expense
            lastCount = state.expenseCount
        }
        barFraction = state.budgetFill
        barLevel = state.budgetLevel
        updateBudgetBar()
    }

    /** Left-edge bar: height = share of budget spent; green < 70%, orange 70-90%, red > 90%. */
    private fun updateBudgetBar() = with(binding) {
        if (!budgetTrack.isVisible) return@with          // applied again when Home is shown
        val color = getColor(
            when (barLevel) {
                BudgetLevel.SAFE -> R.color.green
                BudgetLevel.WARNING -> R.color.orange
                BudgetLevel.DANGER -> R.color.red
            }
        )
        budgetFill.backgroundTintList = ColorStateList.valueOf(color)
        budgetTrack.doOnLayout { track ->
            val target = (track.height * barFraction).toInt()
            ValueAnimator.ofInt(budgetFill.layoutParams.height, target).apply {
                duration = 500
                addUpdateListener {
                    budgetFill.layoutParams.height = it.animatedValue as Int
                    budgetFill.requestLayout()
                }
            }.start()
        }
    }

    /** Long-press menu: Edit or Delete. */
    private fun showExpenseActions(item: ExpenseItemUi) {
        MaterialAlertDialogBuilder(this)
            .setTitle(item.title)
            .setItems(arrayOf("Edit expense", "Delete expense")) { _, which ->
                if (which == 0) AddExpenseSheet.newInstance(item.id).show(supportFragmentManager, AddExpenseSheet.TAG)
                else confirmDelete(item)
            }
            .show()
    }

    private fun confirmDelete(item: ExpenseItemUi) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete this expense?")
            .setMessage("${item.title} (${item.amountText}) will be removed from your totals. This can't be undone.")
            .setPositiveButton("Delete expense") { _, _ -> viewModel.onDeleteConfirmed(item.id) }
            .setNegativeButton("Keep it", null)
            .show()
    }

    private fun setupNavigation() = with(binding) {
        navHome.setOnClickListener { showTab(Tab.HOME) }
        navStats.setOnClickListener { showTab(Tab.STATS) }
        navHistory.setOnClickListener { showTab(Tab.HISTORY) }
        navBudgets.setOnClickListener { showTab(Tab.BUDGETS) }
    }

    /** Home keeps the summary + ledger split; every other tab opens full screen. */
    private fun showTab(tab: Tab) = with(binding) {
        TransitionManager.beginDelayedTransition(mainContent)
        panelHome.isVisible = tab == Tab.HOME
        panelStats.isVisible = tab == Tab.STATS
        panelHistory.isVisible = tab == Tab.HISTORY
        panelBudgets.isVisible = tab == Tab.BUDGETS
        mapOf(lblHome to Tab.HOME, lblStats to Tab.STATS, lblHistory to Tab.HISTORY, lblBudgets to Tab.BUDGETS)
            .forEach { (label, t) -> label.setTextColor(getColor(if (t == tab) R.color.blue else R.color.mute)) }

        val home = tab == Tab.HOME
        budgetTrack.isVisible = home                     // left budget bar: Home only
        topSection.isVisible = home
        guideTop.setGuidelinePercent(if (home) HOME_SPLIT else 0f)
        when (tab) {
            Tab.HOME -> contentPanel.setBackgroundResource(R.drawable.bg_sheet)
            Tab.STATS, Tab.BUDGETS, Tab.HISTORY -> contentPanel.setBackgroundColor(Color.TRANSPARENT)   // shows the navy gradient
        }
        if (home) updateBudgetBar()
    }
    private fun hideNavigationWhileTyping() {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val keyboardOpen = ViewCompat.getRootWindowInsets(binding.root)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            if (binding.bottomAppBar.isVisible == keyboardOpen) {       // state is out of date
                binding.bottomAppBar.isVisible = !keyboardOpen
                binding.fabAdd.isVisible = !keyboardOpen
            }
        }
    }

    private companion object { const val HOME_SPLIT = 0.28f }   // matches guideTop in your XML
}