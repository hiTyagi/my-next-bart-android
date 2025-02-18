package com.rst.mynextbart.network

import com.squareup.moshi.JsonClass

// Remove sealed class and use a regular data class with nullable fields
@JsonClass(generateAdapter = true)
data class Message(
    val warning: String? = null,
    val error: Error? = null,
    val text: String? = null,
    val sched_num: String? = null
) {
    @JsonClass(generateAdapter = true)
    data class Error(
        val text: String? = null,
        val details: String? = null
    )

    companion object {
        fun fromString(str: String) = Message(text = str)
    }
} 