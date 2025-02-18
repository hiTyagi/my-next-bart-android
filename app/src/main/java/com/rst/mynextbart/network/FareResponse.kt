package com.rst.mynextbart.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FareResponse(
    val root: FareRoot
)

@JsonClass(generateAdapter = true)
data class FareRoot(
    val origin: String,
    val destination: String,
    val trip: FareTrip,
    val message: String
)

@JsonClass(generateAdapter = true)
data class FareTrip(
    val fare: String
) 