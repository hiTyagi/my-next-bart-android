package com.rst.mynextbart.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")
private val json = Json { 
    ignoreUnknownKeys = true 
    coerceInputValues = true
    prettyPrint = true
    isLenient = true
}

class FavoritesDataStore(private val context: Context) {
    private val favoritesKey = stringSetPreferencesKey("favorite_stations")
    private val pinnedStationsKey = stringSetPreferencesKey("pinned_stations")
    private val routesKey = stringSetPreferencesKey("favorite_routes")

    val favoriteStations: Flow<List<FavoriteStation>> = context.dataStore.data
        .catch { exception ->
            Log.e("FavoritesDataStore", "Error reading favorites", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            try {
                val stationCodes = preferences[favoritesKey] ?: emptySet()
                val pinnedStations = preferences[pinnedStationsKey] ?: emptySet()
                
                stationCodes.map { stationJson ->
                    try {
                        // Decode the station from JSON
                        val station = json.decodeFromString<FavoriteStation>(stationJson)
                        // Check if this station is in the pinned set
                        station.copy(isPinned = pinnedStations.contains(station.code))
                    } catch (e: Exception) {
                        // If we have a simple string (old format), create a new FavoriteStation
                        FavoriteStation(
                            code = stationJson,
                            name = stationJson,
                            isPinned = pinnedStations.contains(stationJson)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("FavoritesDataStore", "Error mapping stations", e)
                emptyList()
            }
        }

    val favoriteRoutes: Flow<List<FavoriteRoute>> = context.dataStore.data
        .catch { exception ->
            Log.e("FavoritesDataStore", "Error reading routes", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            try {
                val routesJson = preferences[routesKey] ?: emptySet()
                routesJson.mapNotNull { routeJson ->
                    try {
                        json.decodeFromString<FavoriteRoute>(routeJson)
                    } catch (e: Exception) {
                        Log.e("FavoritesDataStore", "Error decoding route: $routeJson", e)
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("FavoritesDataStore", "Error mapping routes", e)
                emptyList()
            }
        }

    val pinnedStations: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            Log.e("FavoritesDataStore", "Error reading pinned stations", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            preferences[pinnedStationsKey] ?: emptySet()
        }

    suspend fun toggleStationPin(station: FavoriteStation) {
        try {
            context.dataStore.edit { preferences ->
                // Get current pinned stations set
                val pinnedStations = preferences[pinnedStationsKey] ?: emptySet()
                
                // Update pinned stations set based on the new pin state
                preferences[pinnedStationsKey] = if (station.isPinned) {
                    // If station is being pinned, add to pinned set
                    pinnedStations + station.code
                } else {
                    // If station is being unpinned, remove from pinned set
                    pinnedStations - station.code
                }

                // Update the station in favorites
                val currentFavorites = preferences[favoritesKey]?.toMutableSet() ?: mutableSetOf()
                
                // Remove old version of the station
                currentFavorites.removeAll { 
                    try {
                        json.decodeFromString<FavoriteStation>(it).code == station.code
                    } catch (e: Exception) {
                        false
                    }
                }
                
                // Add updated station
                currentFavorites.add(json.encodeToString(station))
                preferences[favoritesKey] = currentFavorites

                Log.d("FavoritesDataStore", "Updated pinned stations: ${preferences[pinnedStationsKey]}")
                Log.d("FavoritesDataStore", "Updated favorites: ${preferences[favoritesKey]}")
            }
            Log.d("FavoritesDataStore", "Station pin toggled successfully: ${station.name}, isPinned: ${station.isPinned}")
        } catch (e: Exception) {
            Log.e("FavoritesDataStore", "Error toggling station pin", e)
            throw e
        }
    }

    suspend fun addFavorite(station: FavoriteStation) {
        try {
            context.dataStore.edit { preferences ->
                val currentFavorites = preferences[favoritesKey] ?: emptySet()
                val stationJson = json.encodeToString(station)
                preferences[favoritesKey] = currentFavorites + stationJson
                
                // If station is pinned, also add to pinned stations
                if (station.isPinned) {
                    val pinnedStations = preferences[pinnedStationsKey] ?: emptySet()
                    preferences[pinnedStationsKey] = pinnedStations + station.code
                }
            }
            Log.d("FavoritesDataStore", "Station added successfully: ${station.code}")
        } catch (e: Exception) {
            Log.e("FavoritesDataStore", "Error adding station", e)
        }
    }

    suspend fun removeFavorite(stationCode: String) {
        try {
            context.dataStore.edit { preferences ->
                val currentFavorites = preferences[favoritesKey] ?: emptySet()
                preferences[favoritesKey] = currentFavorites.filterNot { 
                    try {
                        json.decodeFromString<FavoriteStation>(it).code == stationCode
                    } catch (e: Exception) {
                        it == stationCode
                    }
                }.toSet()
                
                // Also remove from pinned stations
                val pinnedStations = preferences[pinnedStationsKey] ?: emptySet()
                preferences[pinnedStationsKey] = pinnedStations - stationCode
            }
            Log.d("FavoritesDataStore", "Station removed successfully: $stationCode")
        } catch (e: Exception) {
            Log.e("FavoritesDataStore", "Error removing station", e)
        }
    }

    suspend fun addFavoriteRoute(route: FavoriteRoute) {
        try {
            context.dataStore.edit { preferences ->
                val currentRoutes = preferences[routesKey] ?: emptySet()
                val routeJson = json.encodeToString(route)
                
                // Remove any existing version of this route
                val updatedRoutes = currentRoutes.filterNot { existingRouteJson ->
                    try {
                        val existingRoute = json.decodeFromString<FavoriteRoute>(existingRouteJson)
                        existingRoute.fromStation == route.fromStation && 
                            existingRoute.toStation == route.toStation
                    } catch (e: Exception) {
                        false
                    }
                }.toMutableSet()
                
                // Add the new version
                updatedRoutes.add(routeJson)
                preferences[routesKey] = updatedRoutes
            }
            Log.d("FavoritesDataStore", "Route added/updated successfully: ${route.fromStation} to ${route.toStation}")
        } catch (e: Exception) {
            Log.e("FavoritesDataStore", "Error adding/updating route", e)
        }
    }

    suspend fun removeFavoriteRoute(route: FavoriteRoute) {
        try {
            context.dataStore.edit { preferences ->
                val currentRoutes = preferences[routesKey] ?: emptySet()
                
                // Remove the route regardless of its current state
                preferences[routesKey] = currentRoutes.filterNot { routeJson ->
                    try {
                        val existingRoute = json.decodeFromString<FavoriteRoute>(routeJson)
                        existingRoute.fromStation == route.fromStation && 
                            existingRoute.toStation == route.toStation
                    } catch (e: Exception) {
                        Log.e("FavoritesDataStore", "Error decoding route during removal", e)
                        false
                    }
                }.toSet()
            }
            Log.d("FavoritesDataStore", "Route removed successfully: ${route.fromStation} to ${route.toStation}")
        } catch (e: Exception) {
            Log.e("FavoritesDataStore", "Error removing route", e)
        }
    }
} 