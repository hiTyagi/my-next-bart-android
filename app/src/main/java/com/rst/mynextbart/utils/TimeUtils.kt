package com.rst.mynextbart.utils

import java.time.LocalTime
import java.time.format.DateTimeFormatter

object TimeUtils {
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    fun getArrivalTime(minutes: String): String {
        if (minutes == "Leaving") return "Now"
        
        val mins = minutes.toIntOrNull() ?: return ""
        val now = LocalTime.now()
        val arrivalTime = now.plusMinutes(mins.toLong())
        return arrivalTime.format(timeFormatter)
    }

    fun formatDepartureTime(minutes: String): String {
        return when {
            minutes == "Leaving" -> "Leaving now"
            minutes == "1" -> "In 1 min"
            else -> "In $minutes mins"
        }
    }
} 