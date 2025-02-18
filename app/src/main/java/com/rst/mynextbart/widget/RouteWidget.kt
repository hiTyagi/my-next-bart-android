package com.rst.mynextbart.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.widget.RemoteViews
import com.rst.mynextbart.MainActivity
import com.rst.mynextbart.R
import com.rst.mynextbart.repository.BartRepository
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
                                    .take(3)

                                val departuresList = filteredDepartures.joinToString("\n") { etd ->
                                    "${etd.destination}: ${etd.estimate.first().minutes} min"
                                }
                                setTextViewText(R.id.departures_list, departuresList)
                            } else {
                                setTextViewText(R.id.departures_list, "No trains scheduled")
                            }
                        } catch (e: Exception) {
                            Log.e("RouteWidget", "Error fetching departures", e)
                            setTextViewText(R.id.departures_list, "Unable to fetch departures")
                        }

                        setTextViewText(R.id.last_updated,
                            "Updated: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}")

                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId,
                            Intent(context, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    Log.e("RouteWidget", "Error updating widget", e)
                    updateWidgetError(context, appWidgetManager, appWidgetId, e.message)
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