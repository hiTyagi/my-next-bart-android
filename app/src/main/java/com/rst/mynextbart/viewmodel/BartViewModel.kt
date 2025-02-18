package com.rst.mynextbart.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rst.mynextbart.network.BartApiResponse
import com.rst.mynextbart.repository.BartRepository
import com.rst.mynextbart.data.FavoritesDataStore
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import com.rst.mynextbart.data.FavoriteRoute
import com.rst.mynextbart.data.FavoriteStation
import com.rst.mynextbart.network.Root
import com.rst.mynextbart.network.Station
import android.util.Log
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

class BartViewModel(
    private val favoritesDataStore: FavoritesDataStore
) : ViewModel() {
    private val repository = BartRepository()
    
    val stations = repository.stations
    
    var selectedStation by mutableStateOf(repository.stations.first())
        private set
        
    var departuresState by mutableStateOf<DeparturesState>(DeparturesState.Loading)
        private set
        
    var favoriteStations by mutableStateOf<List<FavoriteStation>>(emptyList())
        private set
    
    var favoriteRoutes by mutableStateOf<List<FavoriteRoute>>(emptyList())
        private set
    
    // Add a map to store departure information for each route
    private var routeDepartures = mutableMapOf<String, DeparturesState>()
    var routeDeparturesState by mutableStateOf<Map<String, DeparturesState>>(emptyMap())
        private set
    
    // Add this map of routes and their final destinations
    private val routeDestinations = mapOf(
        "Dublin/Pleasanton" to listOf("Daly City", "SFO/Millbrae"),
        "Berryessa" to listOf("Richmond", "Daly City"),
        "Richmond" to listOf("Millbrae", "Berryessa"),
        "Antioch" to listOf("SFO", "SFIA"),
        "SFO" to listOf("Antioch", "Richmond"),
        "Millbrae" to listOf("Richmond"),
        "Daly City" to listOf("Dublin/Pleasanton", "Berryessa")
    )
    
    // Add these properties
    private var routeFromStation: Pair<String, String>? = null
    
    // Add new state variable for station departures
    private var stationDepartures = mutableMapOf<String, DeparturesState>()
    var stationDeparturesState by mutableStateOf<Map<String, DeparturesState>>(emptyMap())
        private set
    
    // Add these properties
    private var _lastRouteRefreshTime by mutableStateOf(0L)
    var lastRouteRefreshTime by mutableStateOf(0L)
        private set

    private var _lastStationRefreshTime by mutableStateOf(0L)
    var lastStationRefreshTime by mutableStateOf(0L)
        private set
    
    init {
        fetchDepartures(selectedStation.first)
        viewModelScope.launch {
            favoritesDataStore.favoriteStations.collect { stations ->
                Log.d("BartViewModel", "Received ${stations.size} favorite stations, ${stations.count { it.isPinned }} pinned")
                favoriteStations = stations
                // Refresh departures for pinned stations when the list changes
                stations.filter { it.isPinned }.forEach { station ->
                    Log.d("BartViewModel", "Auto-refreshing departures for pinned station: ${station.name}")
                    fetchStationDepartures(station)
                }
            }
        }
        viewModelScope.launch {
            favoritesDataStore.favoriteRoutes.collect { routes ->
                favoriteRoutes = routes
                // Refresh departures whenever routes change
                routes.filter { it.isPinned }.forEach { route ->
                    fetchRouteDetails(route)
                }
            }
        }
    }
    
    fun selectStation(stationCode: String, stationName: String) {
        selectedStation = stationCode to stationName
        fetchDepartures(stationCode)
    }
    
    fun toggleFavorite(stationCode: String) {
        viewModelScope.launch {
            val station = stations.find { it.first == stationCode }
            if (station != null) {
                val isCurrentlyFavorite = favoriteStations.any { it.code == stationCode }
                if (isCurrentlyFavorite) {
                    favoritesDataStore.removeFavorite(stationCode)
                } else {
                    val wasPreviouslyPinned = favoriteStations.find { it.code == stationCode }?.isPinned ?: false
                    val newFavorite = FavoriteStation(
                        code = station.first,
                        name = station.second,
                        isPinned = wasPreviouslyPinned
                    )
                    favoritesDataStore.addFavorite(newFavorite)
                }
            }
        }
    }
    
    private fun fetchDepartures(stationCode: String) {
        viewModelScope.launch {
            departuresState = DeparturesState.Loading
            try {
                val response = repository.getDepartures(stationCode)
                departuresState = DeparturesState.Success(response)
            } catch (e: Exception) {
                departuresState = DeparturesState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    fun addFavoriteRoute(fromStation: Pair<String, String>, toStation: Pair<String, String>) {
        viewModelScope.launch {
            val route = FavoriteRoute(
                fromStation = fromStation.first,
                fromStationName = fromStation.second,
                toStation = toStation.first,
                toStationName = toStation.second
            )
            favoritesDataStore.addFavoriteRoute(route)
        }
    }
    
    fun removeFavoriteRoute(route: FavoriteRoute) {
        viewModelScope.launch {
            favoritesDataStore.removeFavoriteRoute(route)
        }
    }
    
    fun togglePinRoute(route: FavoriteRoute) {
        viewModelScope.launch {
            favoritesDataStore.removeFavoriteRoute(route)
            val updatedRoute = route.copy(isPinned = !route.isPinned)
            favoritesDataStore.addFavoriteRoute(updatedRoute)
            
            // If we're pinning the route, fetch its details immediately
            if (!route.isPinned) {
                fetchRouteDetails(updatedRoute)
            } else {
                // If we're unpinning, remove from departures
                val routeKey = "${route.fromStation}_${route.toStation}"
                routeDepartures = routeDepartures.toMutableMap().apply {
                    remove(routeKey)
                }
                routeDeparturesState = routeDepartures
            }
        }
    }
    
    fun toggleStationPin(station: FavoriteStation) {
        viewModelScope.launch {
            Log.d("BartViewModel", "Toggling pin for station: ${station.name}")
            favoritesDataStore.toggleStationPin(station)
            
            // If we're pinning the station, fetch its departures immediately
            if (!station.isPinned) {
                Log.d("BartViewModel", "Station was unpinned, fetching departures after pin")
                fetchStationDepartures(station.copy(isPinned = true))
            } else {
                // If we're unpinning, remove from departures
                Log.d("BartViewModel", "Station was pinned, removing departures after unpin")
                stationDepartures = stationDepartures.toMutableMap().apply {
                    remove(station.code)
                }
                stationDeparturesState = stationDepartures
            }
        }
    }
    
    fun fetchRouteDetails(route: FavoriteRoute) {
        if (!route.isPinned) {
            Log.d("BartViewModel", "Skipping fetch for unpinned route: ${route.fromStationName} to ${route.toStationName}")
            return
        }

        viewModelScope.launch {
            _lastRouteRefreshTime = System.currentTimeMillis()
            lastRouteRefreshTime = _lastRouteRefreshTime
            val routeKey = "${route.fromStation}_${route.toStation}"
            Log.d("BartViewModel", "Starting fetch for route: $routeKey")
            
            routeDepartures = routeDepartures.toMutableMap().apply {
                put(routeKey, DeparturesState.Loading)
            }
            routeDeparturesState = routeDepartures

            try {
                // Get all routes that serve these stations
                val possibleRoutes = repository.findRoutesForStations(route.fromStation, route.toStation)
                Log.d("BartViewModel", "Found ${possibleRoutes.size} routes serving this station pair")
                
                // Log the routes for debugging
                possibleRoutes.forEach { routeInfo ->
                    Log.d("BartViewModel", "Route ${routeInfo.name} stations: ${routeInfo.stations.joinToString()}")
                }
                
                val response = repository.getDepartures(route.fromStation)
                
                val filteredDepartures = response.root.station.firstOrNull()?.let { station ->
                    val relevantEtds = station.etd.filter { etd ->
                        // Get the destination station code
                        val destinationCode = repository.stations.find { (_, name) ->
                            name.trim().equals(etd.destination.trim(), ignoreCase = true)
                        }?.first
                        
                        if (destinationCode == null) {
                            Log.d("BartViewModel", "Could not find station code for destination: ${etd.destination}")
                            false
                        } else {
                            // Check if this train is going in the right direction on any of our possible routes
                            possibleRoutes.any { routeInfo ->
                                val stations = routeInfo.stations
                                val fromIndex = stations.indexOf(route.fromStation)
                                val toIndex = stations.indexOf(route.toStation)
                                val destIndex = stations.indexOf(destinationCode)
                                
                                // Train is relevant if:
                                // 1. All stations are found on this route
                                // 2. The destination is after our origin
                                // 3. The destination is after or at our target station
                                val isRelevant = fromIndex != -1 && 
                                    toIndex != -1 && 
                                    destIndex != -1 &&
                                    destIndex > fromIndex && 
                                    destIndex >= toIndex
                                    
                                Log.d("BartViewModel", """
                                    Checking train to ${etd.destination} on route ${routeInfo.name}:
                                    From index: $fromIndex
                                    To index: $toIndex
                                    Destination index: $destIndex
                                    Is relevant: $isRelevant
                                """.trimIndent())
                                
                                isRelevant
                            }
                        }
                    }

                    if (relevantEtds.isNotEmpty()) {
                        BartApiResponse(
                            root = Root(
                                station = listOf(
                                    Station(
                                        name = station.name,
                                        etd = relevantEtds
                                    )
                                )
                            )
                        )
                    } else {
                        null
                    }
                }

                routeDepartures = routeDepartures.toMutableMap().apply {
                    if (filteredDepartures != null) {
                        put(routeKey, DeparturesState.Success(filteredDepartures))
                    } else {
                        put(routeKey, DeparturesState.Error("No trains found serving this route"))
                    }
                }
                routeDeparturesState = routeDepartures
                
            } catch (e: Exception) {
                Log.e("BartViewModel", "Error fetching route details", e)
                routeDepartures = routeDepartures.toMutableMap().apply {
                    put(routeKey, DeparturesState.Error(e.message ?: "Unknown error occurred"))
                }
                routeDeparturesState = routeDepartures
            }
        }
    }

    // Update fetchStationDepartures with more logging
    fun fetchStationDepartures(station: FavoriteStation) {
        if (!station.isPinned) {
            Log.d("BartViewModel", "Skipping fetch for unpinned station: ${station.name}")
            return
        }

        viewModelScope.launch {
            _lastStationRefreshTime = System.currentTimeMillis()
            lastStationRefreshTime = _lastStationRefreshTime
            Log.d("BartViewModel", "Starting fetch for station: ${station.name}")
            
            stationDepartures = stationDepartures.toMutableMap().apply {
                put(station.code, DeparturesState.Loading)
            }
            stationDeparturesState = stationDepartures
            Log.d("BartViewModel", "Set loading state for station: ${station.code}")

            try {
                Log.d("BartViewModel", "Fetching departures for station: ${station.code}")
                val response = repository.getDepartures(station.code)
                Log.d("BartViewModel", "Got response for station ${station.code}: ${response.root.station.firstOrNull()?.etd?.size ?: 0} departures")
                
                stationDepartures = stationDepartures.toMutableMap().apply {
                    put(station.code, DeparturesState.Success(response))
                }
                stationDeparturesState = stationDepartures
                Log.d("BartViewModel", "Updated departures state for station: ${station.code}")
            } catch (e: Exception) {
                Log.e("BartViewModel", "Error fetching departures for station ${station.code}", e)
                stationDepartures = stationDepartures.toMutableMap().apply {
                    put(station.code, DeparturesState.Error(e.message ?: "Unknown error occurred"))
                }
                stationDeparturesState = stationDepartures
            }
        }
    }

    // Remove refreshPinnedItems since we want separate refreshes
    fun refreshPinnedRoutes() {
        viewModelScope.launch {
            Log.d("BartViewModel", "Starting refresh of pinned routes")
            val pinnedRoutes = favoriteRoutes.filter { it.isPinned }
            Log.d("BartViewModel", "Found ${pinnedRoutes.size} pinned routes")
            
            pinnedRoutes.forEach { route ->
                Log.d("BartViewModel", "Fetching details for route: ${route.fromStationName} to ${route.toStationName}")
                fetchRouteDetails(route)
            }
        }
    }

    fun refreshPinnedStations() {
        viewModelScope.launch {
            Log.d("BartViewModel", "Starting refresh of pinned stations")
            val pinnedStations = favoriteStations.filter { it.isPinned }
            Log.d("BartViewModel", "Found ${pinnedStations.size} pinned stations")
            
            pinnedStations.forEach { station ->
                Log.d("BartViewModel", "Fetching departures for station: ${station.name}")
                fetchStationDepartures(station)
            }
        }
    }

    fun getStationAddress(stationCode: String): String {
        // TODO: Add station addresses to your data
        return "123 Station Street, City, CA"  // Replace with actual address
    }
    
    suspend fun getStationRouteColors(stationCode: String): List<String> {
        try {
            val response = repository.getDepartures(stationCode)
            return response.root.station
                .firstOrNull()
                ?.etd
                ?.flatMap { etd -> 
                    etd.estimate.map { it.hexColor }
                }
                ?.filterNotNull()
                ?.distinct()
                ?: emptyList()
        } catch (e: Exception) {
            Log.e("BartViewModel", "Error getting station colors", e)
            return emptyList()
        }
    }
    
    fun setRouteFromStation(code: String, name: String) {
        routeFromStation = code to name
    }
    
    fun getRouteFromStation(): Pair<String, String>? {
        return routeFromStation
    }

    fun clearRouteFromStation() {
        routeFromStation = null
    }
}

// Create a factory for the ViewModel
class BartViewModelFactory(private val favoritesDataStore: FavoritesDataStore) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BartViewModel(favoritesDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class DeparturesState {
    data object Loading : DeparturesState()
    data class Success(val data: BartApiResponse) : DeparturesState()
    data class Error(val message: String) : DeparturesState()
} 