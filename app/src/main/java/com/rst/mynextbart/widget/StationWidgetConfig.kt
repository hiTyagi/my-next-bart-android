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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val Context.widgetConfigDataStore by preferencesDataStore(name = "widget_config")

@Serializable
data class StationInfo(
    val code: String,
    val name: String,
    val appearance: WidgetAppearance = WidgetAppearance()
)

object StationWidgetConfig {
    private val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = true
    }

    fun saveWidgetStation(context: Context, widgetId: Int, stationInfo: StationInfo) {
        runBlocking {
            context.widgetConfigDataStore.edit { preferences ->
                preferences[stringPreferencesKey(widgetId.toString())] = json.encodeToString(stationInfo)
            }
        }
    }

    fun getWidgetStation(context: Context, widgetId: Int): StationInfo? {
        return runBlocking {
            context.widgetConfigDataStore.data.map { preferences ->
                preferences[stringPreferencesKey(widgetId.toString())]?.let {
                    json.decodeFromString<StationInfo>(it)
                }
            }.first()
        }
    }

    fun removeWidgetStation(context: Context, widgetId: Int) {
        runBlocking {
            context.widgetConfigDataStore.edit { preferences ->
                preferences.remove(stringPreferencesKey(widgetId.toString()))
            }
        }
    }
} 