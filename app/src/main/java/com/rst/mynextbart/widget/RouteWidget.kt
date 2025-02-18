package com.rst.mynextbart.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.text.Html
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import com.rst.mynextbart.MainActivity
import com.rst.mynextbart.R
import com.rst.mynextbart.repository.BartRepository
import com.rst.mynextbart.utils.TimeUtils.formatDepartureTime
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class RouteWidget : AppWidgetProvider() {
    @Inject
    lateinit var repository: BartRepository

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == "com.rst.mynextbart.action.REFRESH_ROUTE_WIDGET") {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 
                AppWidgetManager.INVALID_APPWIDGET_ID)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            
            // Use application context for Toast
            val applicationContext = context.applicationContext
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, "Refreshing route data...", Toast.LENGTH_SHORT).show()
            }
            
            val routeInfo = RouteWidgetConfig.getWidgetRoute(context, appWidgetId)
            if (routeInfo != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    updateWidget(context, appWidgetManager, appWidgetId, routeInfo, repository)
                    Toast.makeText(applicationContext, "Route data refreshed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            appWidgetIds.forEach { appWidgetId ->
                val routeInfo = RouteWidgetConfig.getWidgetRoute(context, appWidgetId)
                if (routeInfo != null) {
                    updateWidget(context, appWidgetManager, appWidgetId, routeInfo, repository)
                } else {
                    updateWidgetError(context, appWidgetManager, appWidgetId, "No route selected")
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            RouteWidgetConfig.removeWidgetRoute(context, widgetId)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            routeInfo: RouteInfo,
            repository: BartRepository
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val isDarkMode = when (routeInfo.appearance.theme) {
                        WidgetTheme.SYSTEM -> (context.resources.configuration.uiMode and 
                            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                        WidgetTheme.LIGHT -> false
                        WidgetTheme.DARK -> true
                    }

                    val views = RemoteViews(context.packageName, R.layout.route_widget).apply {
                        setInt(R.id.widget_container, "setBackgroundResource",
                            if (isDarkMode) R.drawable.widget_background_dark
                            else R.drawable.widget_background_light)
                        
                        // Set alpha
                        setFloat(R.id.widget_container, "setAlpha", routeInfo.appearance.backgroundAlpha)

                        // Set text colors based on theme
                        val textColor = if (isDarkMode) "#FFFFFF" else "#000000"
                        val secondaryTextColor = if (isDarkMode) "#B3FFFFFF" else "#666666"

                        setTextColor(R.id.from_station, android.graphics.Color.parseColor(textColor))
                        setTextColor(R.id.to_station, android.graphics.Color.parseColor(textColor))
                        setTextColor(R.id.departures_list, android.graphics.Color.parseColor(textColor))
                        setTextColor(R.id.last_updated, android.graphics.Color.parseColor(secondaryTextColor))

                        setTextViewText(R.id.from_station, routeInfo.fromStationName)
                        setTextViewText(R.id.to_station, routeInfo.toStationName)

                        try {
                            val departures = repository.getDepartures(routeInfo.fromStation)
                            val stationData = departures.root.station.firstOrNull()

                            if (stationData?.etd != null && stationData.etd.isNotEmpty()) {
                                val filteredDepartures = stationData.etd
                                    .filter { etd ->
                                        // Filter departures that go to or through the destination
                                        val possibleRoutes = repository.findRoutesForStations(
                                            routeInfo.fromStation,
                                            routeInfo.toStation
                                        )
                                        possibleRoutes.any { route ->
                                            val stations = route.stations
                                            val fromIndex = stations.indexOf(routeInfo.fromStation)
                                            val toIndex = stations.indexOf(routeInfo.toStation)
                                            val destIndex = stations.indexOf(
                                                repository.stations.find { it.second == etd.destination }?.first
                                            )
                                            fromIndex != -1 && toIndex != -1 && destIndex != -1 &&
                                                destIndex > fromIndex && destIndex >= toIndex
                                        }
                                    }
                                    .groupBy { it.destination }
                                    .toList()
                                    .take(3)
                                    .map { (destination, etds) ->
                                        val times = etds.flatMap { etd ->
                                            etd.estimate.take(2).map { estimate ->
                                                "<font color='${estimate.hexColor}'>●</font> ${formatDepartureTime(estimate.minutes, true)}"
                                            }
                                        }
                                        .take(2)
                                        .joinToString(", ")
                                        
                                        "<b>$destination</b>: $times"
                                    }
                                    .joinToString("<br>")
                                
                                // Set text as HTML
                                setTextViewText(R.id.departures_list, Html.fromHtml(filteredDepartures, Html.FROM_HTML_MODE_COMPACT))
                            } else {
                                setTextViewText(R.id.departures_list, "No trains scheduled")
                            }
                        } catch (e: Exception) {
                            Log.e("RouteWidget", "Error fetching departures", e)
                            setTextViewText(R.id.departures_list, "Unable to fetch departures")
                        }

                        // Set up refresh button
                        val refreshIntent = Intent(context, RouteWidget::class.java).apply {
                            action = "com.rst.mynextbart.action.REFRESH_ROUTE_WIDGET"
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        val refreshPendingIntent = PendingIntent.getBroadcast(
                            context,
                            appWidgetId,
                            refreshIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setImageViewResource(R.id.refresh_button, R.drawable.ic_refresh)
                        setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)

                        // Set up widget click to open app
                        val openAppIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("screen", "explore")
                            putExtra("station_code", routeInfo.fromStation)
                            putExtra("station_name", routeInfo.fromStationName)
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId,
                            openAppIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                        setTextViewText(R.id.last_updated,
                            "Updated: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}")
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    Log.e("RouteWidget", "Error updating widget", e)
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
            
            val views = RemoteViews(context.packageName, R.layout.route_widget).apply {
                setInt(R.id.widget_container, "setBackgroundResource",
                    if (isDarkMode) R.drawable.widget_background_dark
                    else R.drawable.widget_background_light)

                val textColor = if (isDarkMode) "#FFFFFF" else "#000000"
                val errorColor = if (isDarkMode) "#FF5252" else "#B71C1C"

                setTextColor(R.id.from_station, android.graphics.Color.parseColor(textColor))
                setTextViewText(R.id.from_station, "Error")
                setTextViewText(R.id.to_station, "")
                setTextColor(R.id.departures_list, android.graphics.Color.parseColor(errorColor))
                setTextViewText(R.id.departures_list, error ?: "Unknown error")
                setTextViewText(R.id.last_updated, "")
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
} 