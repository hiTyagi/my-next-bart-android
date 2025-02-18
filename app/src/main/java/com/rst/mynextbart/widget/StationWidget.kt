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
            
            val stationInfo = StationWidgetConfig.getWidgetStation(context, appWidgetId)
            if (stationInfo != null) {
                updateWidget(context, appWidgetManager, appWidgetId, stationInfo, repository)
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

                    val station = repository.stations.find { it.first == stationInfo.code }
                    if (station == null) {
                        updateWidgetError(context, appWidgetManager, appWidgetId, "Station not found")
                        return@launch
                    }

                    val views = RemoteViews(context.packageName, R.layout.station_widget).apply {
                        setInt(R.id.widget_container, "setBackgroundResource",
                            if (isDarkMode) R.drawable.widget_background_dark
                            else R.drawable.widget_background_light)
                        
                        // Set alpha
                        setFloat(R.id.widget_container, "setAlpha", stationInfo.appearance.backgroundAlpha)

                        // Set text colors based on theme
                        val textColor = if (isDarkMode) "#FFFFFF" else "#000000"
                        val secondaryTextColor = if (isDarkMode) "#B3FFFFFF" else "#666666"

                        setTextColor(R.id.station_name, android.graphics.Color.parseColor(textColor))
                        setTextColor(R.id.departures_list, android.graphics.Color.parseColor(textColor))
                        setTextColor(R.id.last_updated, android.graphics.Color.parseColor(secondaryTextColor))

                        setTextViewText(R.id.station_name, station.second)
                        
                        try {
                            val departures = repository.getDepartures(stationInfo.code)
                            val stationData = departures.root.station.firstOrNull()
                            
                            if (stationData?.etd != null && stationData.etd.isNotEmpty()) {
                                val departuresList = stationData.etd
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
                                
                                // Set text as HTML
                                setTextViewText(R.id.departures_list, Html.fromHtml(departuresList, Html.FROM_HTML_MODE_COMPACT))
                            } else {
                                setTextViewText(R.id.departures_list, "No trains scheduled")
                            }
                        } catch (e: Exception) {
                            Log.e("StationWidget", "Error fetching departures", e)
                            setTextViewText(R.id.departures_list, "Unable to fetch departures")
                        }
                        
                        setTextViewText(R.id.last_updated, 
                            "Updated: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}")
                        
                        // Set up refresh button click
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
                        setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)
                        
                        // Set up widget click to open app
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
                        setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                    }
                    
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    Log.e("StationWidget", "Error updating widget", e)
                    updateWidgetError(context, appWidgetManager, appWidgetId, "Unable to update widget")
                }
            }
        }

        private fun updateWidgetError(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            error: String?
        ) {
            val isDarkMode = (context.resources.configuration.uiMode and 
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            
            val views = RemoteViews(context.packageName, R.layout.station_widget).apply {
                setInt(R.id.widget_container, "setBackgroundResource",
                    if (isDarkMode) R.drawable.widget_background_dark
                    else R.drawable.widget_background_light)

                val textColor = if (isDarkMode) "#FFFFFF" else "#000000"
                val errorColor = if (isDarkMode) "#FF5252" else "#B71C1C"

                setTextColor(R.id.station_name, android.graphics.Color.parseColor(textColor))
                setTextColor(R.id.departures_list, android.graphics.Color.parseColor(errorColor))
                setTextColor(R.id.last_updated, android.graphics.Color.parseColor(textColor))

                setTextViewText(R.id.station_name, "Error")
                setTextViewText(R.id.departures_list, error ?: "Unknown error")
                setTextViewText(R.id.last_updated, "")
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
} 