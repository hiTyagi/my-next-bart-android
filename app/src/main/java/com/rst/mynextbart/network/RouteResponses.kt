package com.rst.mynextbart.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RoutesResponse(
    val root: RoutesRoot
)

@JsonClass(generateAdapter = true)
data class RoutesRoot(
    @Json(name = "routes") val routesWrapper: RoutesWrapper
)

@JsonClass(generateAdapter = true)
data class RoutesWrapper(
    @Json(name = "route") val routes: List<Route>
)

@JsonClass(generateAdapter = true)
data class Route(
    val name: String = "",
    val number: String = "",
    @Json(name = "routeID") val routeId: String = "",
    val origin: String? = null,
    val destination: String? = null,
    val direction: String? = null,
    @Json(name = "hex_color") val hexColor: String? = null,
    val color: String? = null
)

@JsonClass(generateAdapter = true)
data class RouteInfoResponse(
    val root: RouteInfoRoot
)

@JsonClass(generateAdapter = true)
data class RouteInfoRoot(
    @Json(name = "routes") val routeWrapper: RouteWrapper
)

@JsonClass(generateAdapter = true)
data class RouteWrapper(
    @Json(name = "route") val route: RouteDetails
)

@JsonClass(generateAdapter = true)
data class RouteDetails(
    val name: String = "",
    val number: String = "",
    @Json(name = "routeID") val routeId: String = "",
    val origin: String = "",
    val destination: String = "",
    val direction: String? = null,
    @Json(name = "hex_color") val hexColor: String? = null,
    val color: String? = null,
    @Json(name = "config") val config: StationConfig? = null
) {
    val stations: List<String>
        get() = config?.station ?: emptyList()
}

@JsonClass(generateAdapter = true)
data class StationConfig(
    @Json(name = "station") val station: List<String>
) 