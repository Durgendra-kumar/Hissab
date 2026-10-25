package com.project.hissab.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.project.hissab.R
import com.project.hissab.domain.model.BudgetLevel
import com.project.hissab.domain.model.SpendingOverview
import com.project.hissab.ui.home.MainActivity
import com.project.hissab.util.Formatters
import kotlin.math.roundToInt

class SpendingWidgetRenderer(private val context: Context) {

    fun render(o: SpendingOverview, widthDp: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_spending)
        val limit = o.budget.limit

        views.setTextViewText(R.id.widgetMonth, Formatters.month(o.month))
        views.setTextViewText(
            R.id.widgetSpent,
            if (o.budget.isSet && limit != null) "${money(o.spent)} / ${money(limit)}" else money(o.spent)
        )
        if (o.budget.isSet) {
            views.setTextViewText(R.id.widgetPercent, "${(o.budget.ratio * 100).roundToInt()}% used")
            views.setTextColor(R.id.widgetPercent, context.getColor(levelColor(o.budget.level)))
        } else {
            views.setTextViewText(R.id.widgetPercent, "No budget set")
            views.setTextColor(R.id.widgetPercent, context.getColor(R.color.mute))
        }

        views.setTextViewText(R.id.widgetUpi, money(o.upi))
        views.setTextViewText(R.id.widgetCash, money(o.cash))
        views.setTextViewText(R.id.widgetCard, money(o.creditCard))

        val density = context.resources.displayMetrics.density
        val widthPx = ((widthDp - 2 * PADDING_DP) * density).toInt().coerceAtLeast(120)
        val heightPx = (BAR_HEIGHT_DP * density).toInt()
        val colors = BarColors(
            track = context.getColor(R.color.widget_track),
            upi = context.getColor(R.color.pay_upi),
            cash = context.getColor(R.color.pay_cash),
            creditCard = context.getColor(R.color.pay_card)
        )
        views.setImageViewBitmap(R.id.widgetBar, SpendingBarPainter.paint(widthPx, heightPx, o, colors))

        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widgetRoot, openApp)
        return views
    }

    private fun money(v: Double) = Formatters.rupees(v, decimals = false)

    private fun levelColor(level: BudgetLevel) = when (level) {
        BudgetLevel.SAFE -> R.color.green
        BudgetLevel.WARNING -> R.color.orange
        BudgetLevel.DANGER -> R.color.red
    }

    private companion object {
        const val PADDING_DP = 14      // must match the padding in widget_spending.xml
        const val BAR_HEIGHT_DP = 10
    }
}