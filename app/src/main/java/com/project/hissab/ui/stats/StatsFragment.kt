package com.project.hissab.ui.stats

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.project.hissab.R
import com.project.hissab.databinding.FragmentStatsBinding
import com.project.hissab.databinding.ItemLegendBinding
import com.project.hissab.util.Formatters
import com.project.hissab.util.collectIn
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

class StatsFragment : Fragment(R.layout.fragment_stats) {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatsViewModel by viewModels { StatsViewModel.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatsBinding.bind(view)
        setupPieChart()
        setupBarChart()
        viewModel.uiState.collectIn(viewLifecycleOwner, ::render)
    }

    // ---------- one-time chart styling ----------
    private fun setupPieChart() = with(binding.pieChart) {
        description.isEnabled = false
        legend.isEnabled = false
        setTouchEnabled(false)               // lets the page scroll over the chart
        setDrawEntryLabels(false)
        isDrawHoleEnabled = true
        holeRadius = 62f
        setHoleColor(android.graphics.Color.TRANSPARENT)
        setTransparentCircleAlpha(0)
        setCenterTextColor(color(R.color.text))
        setCenterTextSize(16f)
    }

    private fun setupBarChart() = with(binding.barChart) {
        description.isEnabled = false
        legend.isEnabled = false
        setTouchEnabled(false)
        axisRight.isEnabled = false
        setExtraTopOffset(16f)
        axisLeft.apply {
            textColor = color(R.color.mute)
            gridColor = color(R.color.glass)
            setDrawAxisLine(false)
            axisMinimum = 0f
            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?) =
                    if (value == 0f) "0" else Formatters.compactRupees(value.toDouble())
            }
        }
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = color(R.color.text)
            setDrawGridLines(false)
            setDrawAxisLine(false)
            granularity = 1f
        }
    }

    // ---------- drawing ----------
    private fun render(state: StatsUiState) {
        binding.tvStatsMonth.text = state.monthText
        renderPie(state)
        renderBars(state)
    }

    private fun renderPie(state: StatsUiState) {
        val hasData = state.slices.isNotEmpty()
        binding.pieChart.isVisible = hasData
        binding.legendContainer.isVisible = hasData
        binding.tvEmptyPie.isVisible = !hasData
        if (!hasData) return

        val dataSet = PieDataSet(state.slices.map { PieEntry(it.amount, it.name) }, "").apply {
            colors = state.slices.map { it.color }
            sliceSpace = 2f
            setDrawValues(false)
        }
        binding.pieChart.apply {
            data = PieData(dataSet)
            centerText = "${state.totalText}\nthis month"
            invalidate()
        }

        binding.legendContainer.removeAllViews()
        state.slices.forEach { slice ->
            val row = ItemLegendBinding.inflate(layoutInflater, binding.legendContainer, false)
            row.legendDot.backgroundTintList = ColorStateList.valueOf(slice.color)
            row.tvLegendName.text = "${slice.icon} ${slice.name}"
            row.tvLegendPercent.text = slice.percentText
            row.tvLegendAmount.text = slice.amountText
            binding.legendContainer.addView(row.root)
        }
    }

    private fun renderBars(state: StatsUiState) {
        val dataSet = BarDataSet(state.bars.mapIndexed { i, b -> BarEntry(i.toFloat(), b.value) }, "").apply {
            colors = state.bars.map {
                color(
                    when (it.status) {
                        BarStatus.WITHIN_BUDGET -> R.color.green
                        BarStatus.OVER_BUDGET -> R.color.red
                        BarStatus.NO_BUDGET -> R.color.mute
                    }
                )
            }
            valueTextColor = color(R.color.text)
            valueTextSize = 11f
        }
        binding.barChart.apply {
            // An all-zero chart would have a zero-height axis, so give it a default range.
            if (state.bars.all { it.value <= 0f }) axisLeft.axisMaximum = 1000f else axisLeft.resetAxisMaximum()
            xAxis.valueFormatter = IndexAxisValueFormatter(state.bars.map { it.label })
            xAxis.labelCount = state.bars.size
            data = BarData(dataSet).apply {
                barWidth = 0.55f
                setValueFormatter(object : ValueFormatter() {
                    override fun getBarLabel(barEntry: BarEntry?) = Formatters.compactRupees(barEntry?.y?.toDouble() ?: 0.0)
                })
            }
            setFitBars(true)
            invalidate()
        }
    }

    private fun color(id: Int) = requireContext().getColor(id)

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}