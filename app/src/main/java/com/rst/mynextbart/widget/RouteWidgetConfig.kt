package com.rst.mynextbart.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.routeWidgetConfigDataStore by preferencesDataStore(name = "route_widget_config")

@Serializable
data class RouteInfo(
    val fromStation: String,
    val fromStationName: String,
    val toStation: String,
    val toStationName: String,
    val appearance: WidgetAppearance = WidgetAppearance()
)

object RouteWidgetConfig {
    private val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = true
    }

    fun saveWidgetRoute(context: Context, widgetId: Int, routeInfo: RouteInfo) {
        runBlocking {
            context.routeWidgetConfigDataStore.edit { preferences ->
                preferences[stringPreferencesKey(widgetId.toString())] = json.encodeToString(routeInfo)
            }
        }
    }

    fun getWidgetRoute(context: Context, widgetId: Int): RouteInfo? {
        return runBlocking {
            context.routeWidgetConfigDataStore.data.map { preferences ->
                preferences[stringPreferencesKey(widgetId.toString())]?.let {
                    json.decodeFromString<RouteInfo>(it)
                }
            }.first()
        }
    }

    fun removeWidgetRoute(context: Context, widgetId: Int) {
        runBlocking {
            context.routeWidgetConfigDataStore.edit { preferences ->
                preferences.remove(stringPreferencesKey(widgetId.toString()))
            }
        }
    }
} 