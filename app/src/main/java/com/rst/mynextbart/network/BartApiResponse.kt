package com.rst.mynextbart.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BartApiResponse(
    val root: Root
)

@JsonClass(generateAdapter = true)
data class Root(
    val station: List<Station>,
    val message: Message? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "time") val time: String? = null
)

@JsonClass(generateAdapter = true)
data class Station(
    @Json(name = "name") val name: String,
    @Json(name = "abbr") val abbr: String? = null,
    @Json(name = "etd") val etd: List<Etd>? = null
)

@JsonClass(generateAdapter = true)
data class Etd(
    @Json(name = "destination") val destination: String,
    @Json(name = "estimate") val estimate: List<Estimate>
)

@JsonClass(generateAdapter = true)
data class Estimate(
    @Json(name = "minutes") val minutes: String,
    @Json(name = "platform") val platform: String,
    @Json(name = "direction") val direction: String,
    @Json(name = "length") val length: String,
    @Json(name = "color") val color: String,
    @Json(name = "hexcolor") val hexColor: String,
    @Json(name = "bikeflag") val bikeFlag: String,
    @Json(name = "delay") val delay: String
)