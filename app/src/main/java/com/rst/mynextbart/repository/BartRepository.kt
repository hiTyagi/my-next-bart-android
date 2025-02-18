package com.rst.mynextbart.repository

import android.util.Log
import com.rst.mynextbart.network.BartApiResponse
import com.rst.mynextbart.network.RetrofitClient
import com.rst.mynextbart.network.Route
import com.rst.mynextbart.network.RouteDetails
import com.rst.mynextbart.network.RoutesResponse
import com.rst.mynextbart.network.RouteInfoResponse
import com.rst.mynextbart.network.Station
import com.rst.mynextbart.network.Root

class BartRepository {
    private val bartService = RetrofitClient.bartService
    
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
        try {
            val response = bartService.getRealTimeEstimates(station = stationCode)
            
            // Check for API error message
            if (response.root.message?.warning?.contains("No data matched your criteria") == true) {
                // Return a valid response with null ETD
                return BartApiResponse(
                    root = Root(
                        station = listOf(
                            Station(
                                name = stations.find { it.first == stationCode }?.second ?: stationCode,
                                abbr = stationCode,
                                etd = emptyList()
                            )
                        ),
                        date = response.root.date,
                        time = response.root.time
                    )
                )
            }
            
            return response
        } catch (e: Exception) {
            Log.e("BartRepository", "Error fetching departures", e)
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
                bartService.getRouteInfo(route = number).root.routeWrapper.route
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
} 