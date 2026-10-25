package com.project.hissab.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.project.hissab.domain.model.SpendingOverview
import com.project.hissab.domain.usecase.ObserveSpendingOverviewUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SpendingWidgetUpdater(
    private val context: Context,
    private val observeOverview: ObserveSpendingOverviewUseCase,
    private val renderer: SpendingWidgetRenderer = SpendingWidgetRenderer(context)
) {
    private val manager: AppWidgetManager get() = AppWidgetManager.getInstance(context)

    private fun widgetIds(): IntArray =
        manager.getAppWidgetIds(ComponentName(context, SpendingWidgetProvider::class.java))

    /** Redraws the given widgets once with the latest data. */
    suspend fun refreshNow(ids: IntArray = widgetIds()) {
        if (ids.isEmpty()) return
        draw(observeOverview().first(), ids)
    }

    /** Redraws every placed widget whenever an expense or budget changes (while the app process is alive). */
    fun keepInSync(scope: CoroutineScope) = scope.launch {
        observeOverview().distinctUntilChanged().collect { overview ->
            val ids = widgetIds()
            if (ids.isNotEmpty()) draw(overview, ids)
        }
    }

    private fun draw(overview: SpendingOverview, ids: IntArray) {
        ids.forEach { id ->
            val widthDp = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            manager.updateAppWidget(id, renderer.render(overview, widthDp))
        }
    }
}