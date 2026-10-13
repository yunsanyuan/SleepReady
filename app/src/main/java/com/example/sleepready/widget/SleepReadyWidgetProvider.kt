package com.example.sleepready.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.sleepready.R

/**
 * Static AppWidget. `updatePeriodMillis` is 0 (per Requirement 21.4) so the
 * platform never polls in the background — the only update path is the user's tap,
 * which goes through [WidgetReceiver].
 */
class SleepReadyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_sleep_ready)
            views.setOnClickPendingIntent(
                R.id.widget_quick_start_button,
                buildQuickStartPendingIntent(context, appWidgetId)
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {
        const val ACTION_QUICK_START = "com.example.sleepready.widget.QUICK_START"

        /** Convenience for callers that want to refresh all widgets after config save. */
        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, SleepReadyWidgetProvider::class.java)
            val ids = mgr.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, SleepReadyWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }

        private fun buildQuickStartPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, WidgetReceiver::class.java).apply {
                action = ACTION_QUICK_START
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getBroadcast(context, appWidgetId, intent, flags)
        }
    }
}
