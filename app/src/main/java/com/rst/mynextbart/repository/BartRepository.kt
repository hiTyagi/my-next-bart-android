package com.rst.mynextbart.repository

import android.content.Context
import android.util.Log
import com.rst.mynextbart.R
import com.rst.mynextbart.network.BartApiResponse
import com.rst.mynextbart.network.RetrofitClient
import com.rst.mynextbart.network.Route
import com.rst.mynextbart.network.RouteDetails
import com.rst.mynextbart.network.RoutesResponse
import com.rst.mynextbart.network.RouteInfoResponse
import com.rst.mynextbart.network.Station
import com.rst.mynextbart.network.Root
import com.rst.mynextbart.data.FavoritesDataStore
import com.rst.mynextbart.data.FavoriteStation
import com.rst.mynextbart.network.BartService
import com.rst.mynextbart.network.StationInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class BartRepository @Inject constructor(
    private val bartService: BartService,
    private val favoritesDataStore: FavoritesDataStore,
) {
    private var routeInfoCache = mutableMapOf<String, RouteDetails>()
    private var routesCache: List<RouteDetails>? = null
    
    // Add route number mapping
    private val routeNumbers = mapOf(
        "ROUTE 1" to "1", // Richmond - Millbrae
        "ROUTE 2" to "2", // Millbrae/SFO - Antioch
        "ROUTE 3" to "3", // Richmond - Berryessa/North San Jose
        "ROUTE 4" to "4", // Berryessa/North San Jose - Millbrae
        "ROUTE 5" to "5", // Dublin/Pleasanton - Daly City
        "ROUTE 6" to "6", // Daly City - Dublin/Pleasanton
        "ROUTE 7" to "7", // Millbrae - Daly City
        "ROUTE 8" to "8"  // Daly City - Richmond
    )
    
    suspend fun getDepartures(stationCode: String): BartApiResponse {
        return try {
            bartService.getDepartures(
                orig = stationCode,
            )
        } catch (e: Exception) {
            Log.e("BartRepository", "Error fetching departures for station $stationCode", e)
            throw e
        }
    }
    
    // List of BART stations for the dropdown
    val stations = listOf(
        "12TH" to "12th St. Oakland City Center",
        "16TH" to "16th St. Mission",
        "24TH" to "24th St. Mission",
        "ASHB" to "Ashby",
        "BALB" to "Balboa Park",
        "BAYF" to "Bay Fair",
        "CAST" to "Castro Valley",
        "CIVC" to "Civic Center/UN Plaza",
        "COLM" to "Colma",
        "DALY" to "Daly City",
        "DBRK" to "Downtown Berkeley",
        "DUBL" to "Dublin/Pleasanton",
        "EMBR" to "Embarcadero",
        "FRMT" to "Fremont",
        "FTVL" to "Fruitvale",
        "GLEN" to "Glen Park",
        "HAYW" to "Hayward",
        "LAKE" to "Lake Merritt",
        "MCAR" to "MacArthur",
        "MLBR" to "Millbrae",
        "MONT" to "Montgomery St.",
        "NBRK" to "North Berkeley",
        "NCON" to "North Concord/Martinez",
        "OAKL" to "Oakland Int'l Airport",
        "PITT" to "Pittsburg/Bay Point",
        "PLZA" to "El Cerrito Plaza",
        "POWL" to "Powell St.",
        "RICH" to "Richmond",
        "ROCK" to "Rockridge",
        "SBRN" to "San Bruno",
        "SFIA" to "San Francisco Int'l Airport",
        "SANL" to "San Leandro",
        "SHAY" to "South Hayward",
        "SSAN" to "South San Francisco",
        "UCTY" to "Union City",
        "WARM" to "Warm Springs/South Fremont",
        "WCRK" to "Walnut Creek",
        "WDUB" to "West Dublin/Pleasanton",
        "WOAK" to "West Oakland"
    )
    
    suspend fun getRoutes(): List<Route> {
        return try {
            bartService.getRoutes().root.routesWrapper.routes
        } catch (e: Exception) {
            Log.e("BartRepository", "Error fetching routes", e)
            emptyList()
        }
    }
    
    suspend fun getRouteInfo(routeId: String): RouteDetails {
        return routeInfoCache.getOrPut(routeId) {
            try {
                // Convert ROUTE XX format to just the number
                val number = routeNumbers[routeId] ?: routeId.split(" ").lastOrNull() ?: routeId
                Log.d("BartRepository", "Fetching route info for route number: $number")
                bartService.getRouteInfo(routeNumber = number).root.routeWrapper.route
            } catch (e: Exception) {
                Log.e("BartRepository", "Error fetching route info for $routeId", e)
                throw e
            }
        }
    }
    
    suspend fun findRoutesForStations(fromStation: String, toStation: String): List<RouteDetails> {
        val routes = getRoutes()
        Log.d("BartRepository", "Found ${routes.size} total routes")
        
        routes.forEach { route ->
            Log.d("BartRepository", "Route: ${route.name} (${route.routeId})")
        }

        return routes.mapNotNull { route ->
            try {
                val routeInfo = getRouteInfo(route.routeId)
                val stations = routeInfo.stations
                
                if (stations == null) {
                    Log.d("BartRepository", "No stations found for route ${route.name}")
                    null
                } else {
                    Log.d("BartRepository", "Route ${route.name} stations: ${stations.joinToString()}")
                    
                    // Check if both stations are on this route
                    if (stations.contains(fromStation) && stations.contains(toStation)) {
                        // Check if destination comes after origin
                        val fromIndex = stations.indexOf(fromStation)
                        val toIndex = stations.indexOf(toStation)
                        if (fromIndex < toIndex) {
                            Log.d("BartRepository", "Found valid route: ${route.name}")
                            routeInfo
                        } else {
                            Log.d("BartRepository", "Stations in wrong order on route ${route.name}")
                            null
                        }
                    } else {
                        Log.d("BartRepository", "Stations not found on route ${route.name}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("BartRepository", "Error processing route ${route.routeId}", e)
                null
            }
        }
    }

    suspend fun getPinnedStations(): List<FavoriteStation> {
        return favoritesDataStore.favoriteStations.first().filter { it.isPinned }
    }

    suspend fun getStationInfo(stationCode: String): StationInfo {
        return bartService.getStationInfo(stationCode).root.stations.station
    }

    // Cache for station info
    private val stationInfoCache = mutableMapOf<String, StationInfo>()

    suspend fun getStationAddress(stationCode: String): String {
        return try {
            // Check cache first
            val cachedInfo = stationInfoCache[stationCode]
            if (cachedInfo != null) {
                formatAddress(cachedInfo)
            } else {
                val stationInfo = getStationInfo(stationCode)
                stationInfoCache[stationCode] = stationInfo
                formatAddress(stationInfo)
            }
        } catch (e: Exception) {
            Log.e("BartRepository", "Error fetching station info", e)
            "Address not available"
        }
    }

    private fun formatAddress(stationInfo: StationInfo): String {
        return "${stationInfo.address}, ${stationInfo.city}, ${stationInfo.state} ${stationInfo.zipCode}"
    }
} 