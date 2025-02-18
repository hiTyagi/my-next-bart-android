package com.rst.mynextbart.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteStation(
    @SerialName("code")
    val code: String,
    @SerialName("name")
    val name: String,
    @SerialName("is_pinned")
    val isPinned: Boolean = false,
    @SerialName("added_at")
    val addedAt: Long = System.currentTimeMillis(),
    @SerialName("pinned_at")
    val pinnedAt: Long? = null
) 