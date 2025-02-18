package com.rst.mynextbart.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.util.Log
import android.widget.RemoteViews
import com.rst.mynextbart.MainActivity
import com.rst.mynextbart.R
import com.rst.mynextbart.repository.BartRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import android.content.res.Configuration
import android.graphics.Color
import android.text.Html
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.Toast

@AndroidEntryPoint
class StationWidget : AppWidgetProvider() {
    @Inject
    lateinit var repository: BartRepository
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            appWidgetIds.forEach { appWidgetId ->
                val stationCode = StationWidgetConfig.getWidgetStation(context, appWidgetId)
                if (stationCode != null) {
                    updateWidget(context, appWidgetManager, appWidgetId, stationCode, repository)
                } else {
                    updateWidgetError(context, appWidgetManager, appWidgetId, "No station selected")
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // Clean up the preferences when widgets are deleted
        appWidgetIds.forEach { widgetId ->
            StationWidgetConfig.removeWidgetStation(context, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == "com.rst.mynextbart.action.REFRESH_WIDGET") {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 
                AppWidgetManager.INVALID_APPWIDGET_ID)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            
            // Show toast using broadcast
            context.sendBroadcast(Intent("com.rst.mynextbart.action.SHOW_TOAST")
                .putExtra("message", "Refreshing station data..."))
            
            val stationInfo = StationWidgetConfig.getWidgetStation(context, appWidgetId)
            if (stationInfo != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    updateWidget(context, appWidgetManager, appWidgetId, stationInfo, repository)
                    context.sendBroadcast(Intent("com.rst.mynextbart.action.SHOW_TOAST")
                        .putExtra("message", "Station data refreshed"))
                }
            }
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            stationInfo: StationInfo,
            repository: BartRepository
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val isDarkMode = when (stationInfo.appearance.theme) {
                        WidgetTheme.SYSTEM -> (context.resources.configuration.uiMode and 
                            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                        WidgetTheme.LIGHT -> false
                        WidgetTheme.DARK -> true
                    }

                    val views = RemoteViews(context.packageName, R.layout.station_widget)
                    
                    // Set background and alpha
                    views.setInt(R.id.widget_container, "setBackgroundResource",
                        if (isDarkMode) R.drawable.widget_background_dark
                        else R.drawable.widget_background_light)
                    views.setFloat(R.id.widget_container, "setAlpha", stationInfo.appearance.backgroundAlpha)

                    // Set text colors based on theme
                    val textColor = if (isDarkMode) "#FFFFFF" else "#000000"
                    val secondaryTextColor = if (isDarkMode) "#B3FFFFFF" else "#666666"

                    views.setTextColor(R.id.station_name, Color.parseColor(textColor))
                    views.setTextColor(R.id.departures_list, Color.parseColor(textColor))
                    views.setTextColor(R.id.last_updated, Color.parseColor(secondaryTextColor))

                    // Set station name
                    views.setTextViewText(R.id.station_name, stationInfo.name)

                    try {
                        val response = repository.getDepartures(stationInfo.code)
                        
                        if (response.root.station.firstOrNull()?.etd != null && 
                            response.root.station.first().etd!!.isNotEmpty()) {
                            val departuresList = response.root.station.first().etd!!
                                .groupBy { it.destination }
                                .toList()
                                .take(3)
                                .map { (destination, etds) ->
                                    val times = etds.flatMap { etd ->
                                        etd.estimate.take(2).map { estimate ->
                                            "<font color='${estimate.hexColor}'>●</font> ${estimate.minutes} min"
                                        }
                                    }
                                    .take(2)
                                    .joinToString(", ")
                                    
                                    "<b>$destination</b>: $times"
                                }
                                .joinToString("<br>")
                            
                            views.setTextViewText(R.id.departures_list, 
                                Html.fromHtml(departuresList, Html.FROM_HTML_MODE_COMPACT))
                        } else {
                            views.setTextViewText(R.id.departures_list, "No trains scheduled")
                        }

                        // Set last updated time
                        views.setTextViewText(R.id.last_updated, 
                            "Updated: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}")

                        // Set up refresh button
                        val refreshIntent = Intent(context, StationWidget::class.java).apply {
                            action = "com.rst.mynextbart.action.REFRESH_WIDGET"
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        val refreshPendingIntent = PendingIntent.getBroadcast(
                            context,
                            appWidgetId,
                            refreshIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setImageViewResource(R.id.refresh_button, R.drawable.ic_refresh)
                        views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)

                        // Set up widget click
                        val openAppIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("screen", "explore")
                            putExtra("station_code", stationInfo.code)
                            putExtra("station_name", stationInfo.name)
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId,
                            openAppIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    } catch (e: Exception) {
                        Log.e("StationWidget", "Error updating widget", e)
                        updateWidgetError(context, appWidgetManager, appWidgetId, "Unable to update widget")
                    }
                } catch (e: Exception) {
                    Log.e("StationWidget", "Error setting up widget", e)
                    updateWidgetError(context, appWidgetManager, appWidgetId, "Unable to set up widget")
                }
            }
        }

        private fun updateWidgetError(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            error: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.station_widget)
            views.setTextViewText(R.id.station_name, "Error")
            views.setTextViewText(R.id.departures_list, error)
            views.setTextViewText(R.id.last_updated, "")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
} 