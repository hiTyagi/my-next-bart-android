package com.rst.mynextbart.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StationInfoResponse(
    val root: StationInfoRoot
)

@JsonClass(generateAdapter = true)
data class StationInfoRoot(
    val stations: StationInfoWrapper
)

@JsonClass(generateAdapter = true)
data class StationInfoWrapper(
    val station: StationInfo
)

@JsonClass(generateAdapter = true)
data class StationInfo(
    val name: String,
    val abbr: String,
    val address: String,
    val city: String,
    @Json(name = "state") val state: String,
    @Json(name = "zipcode") val zipCode: String,
    @Json(name = "gtfs_latitude") val latitude: String,
    @Json(name = "gtfs_longitude") val longitude: String,
    val county: String? = null,
    @Json(name = "north_routes") val northRoutes: Routes? = null,
    @Json(name = "south_routes") val southRoutes: Routes? = null,
    @Json(name = "north_platforms") val northPlatforms: Any? = null,
    @Json(name = "south_platforms") val southPlatforms: Any? = null,
    @Json(name = "platform_info") val platformInfo: String? = null,
    val intro: CDataWrapper? = null,
    @Json(name = "cross_street") val crossStreet: CDataWrapper? = null,
    val food: CDataWrapper? = null,
    val shopping: CDataWrapper? = null,
    val attraction: CDataWrapper? = null,
    val link: CDataWrapper? = null
) {
    fun getNorthPlatforms(): List<String> {
        return when (northPlatforms) {
            is String -> if (northPlatforms.isBlank()) emptyList() else listOf(northPlatforms)
            is Platforms -> (northPlatforms as Platforms).getPlatforms()
            else -> emptyList()
        }
    }
}

@JsonClass(generateAdapter = true)
data class Routes(
    val route: Any? = null  // Can be either String or List<String>
) {
    fun getRoutes(): List<String> {
        return when (route) {
            is String -> listOf(route)
            is List<*> -> route.filterIsInstance<String>()
            else -> emptyList()
        }
    }
}

@JsonClass(generateAdapter = true)
data class Platforms(
    val platform: Any? = null  // Can be either String or List<String>
) {
    fun getPlatforms(): List<String> {
        return when (platform) {
            is String -> if (platform.isBlank()) emptyList() else listOf(platform)
            is List<*> -> platform.filterIsInstance<String>()
            else -> emptyList()
        }
    }
}

@JsonClass(generateAdapter = true)
data class CDataWrapper(
    @Json(name = "#cdata-section") val content: String
)