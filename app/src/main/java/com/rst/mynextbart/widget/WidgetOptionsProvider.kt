package com.rst.mynextbart.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews

object WidgetOptionsProvider {
    fun getOptionsIntent(context: Context, appWidgetId: Int, isRouteWidget: Boolean): PendingIntent {
        val intent = Intent(context, WidgetSettingsActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(WidgetSettingsActivity.EXTRA_IS_ROUTE_WIDGET, isRouteWidget)
        }
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
} 