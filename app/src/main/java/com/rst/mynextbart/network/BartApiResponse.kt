package com.rst.mynextbart.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BartApiResponse(
    val root: Root
)

@JsonClass(generateAdapter = true)
data class Root(
    val station: List<Station>
)

@JsonClass(generateAdapter = true)
data class Station(
    val name: String,
    val etd: List<Etd>
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