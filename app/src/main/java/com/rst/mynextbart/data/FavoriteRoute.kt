package com.rst.mynextbart.data

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteRoute(
    val fromStation: String,
    val fromStationName: String,
    val toStation: String,
    val toStationName: String,
    val isPinned: Boolean = false
) 