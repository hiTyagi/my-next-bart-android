package com.rst.mynextbart.utils

import android.util.Log
import androidx.compose.ui.graphics.Color

object ColorUtils {
    fun parseHexColor(hexColor: String?): Color {
        if (hexColor.isNullOrBlank()) return Color.Gray
        
        return try {
            val colorString = hexColor.trim().removePrefix("#")
            when (colorString.length) {
                3 -> { // Convert RGB to RRGGBB format
                    val r = colorString[0].toString().repeat(2)
                    val g = colorString[1].toString().repeat(2)
                    val b = colorString[2].toString().repeat(2)
                    Color(
                        red = r.toInt(16) / 255f,
                        green = g.toInt(16) / 255f,
                        blue = b.toInt(16) / 255f
                    )
                }
                6 -> {
                    Color(
                        red = colorString.substring(0, 2).toInt(16) / 255f,
                        green = colorString.substring(2, 4).toInt(16) / 255f,
                        blue = colorString.substring(4, 6).toInt(16) / 255f
                    )
                }
                else -> {
                    Log.w("ColorUtils", "Invalid hex color format: $hexColor")
                    Color.Gray
                }
            }
        } catch (e: Exception) {
            Log.e("ColorUtils", "Error parsing color: $hexColor", e)
            Color.Gray
        }
    }
} 