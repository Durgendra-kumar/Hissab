package com.project.hissab.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.project.hissab.HissabApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpendingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) =
        refresh(context, appWidgetIds)

    // Called when the user resizes the widget, so the bar is redrawn at the new width.
    override fun onAppWidgetOptionsChanged(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle
    ) = refresh(context, intArrayOf(appWidgetId))

    private fun refresh(context: Context, ids: IntArray) {
        val pending = goAsync()      // keeps the receiver alive while the database is read
        val updater = (context.applicationContext as HissabApp).container.spendingWidgetUpdater
        CoroutineScope(Dispatchers.Default).launch {
            try { updater.refreshNow(ids) } finally { pending.finish() }
        }
    }
}