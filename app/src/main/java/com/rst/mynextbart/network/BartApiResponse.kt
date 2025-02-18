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
    val date: String? = null,
    val time: String? = null
)

@JsonClass(generateAdapter = true)
data class Message(
    val warning: String? = null
)

@JsonClass(generateAdapter = true)
data class Station(
    val name: String,
    val abbr: String? = null,
    val etd: List<Etd>? = null  // Make etd nullable with default value
)

@JsonClass(generateAdapter = true)
data class Etd(
    val destination: String,
    val estimate: List<Estimate>
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