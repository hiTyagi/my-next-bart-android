package com.rst.mynextbart.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),      // Lighter Blue
    onPrimary = Color(0xFF002171),
    primaryContainer = Color(0xFF1976D2),
    onPrimaryContainer = Color(0xFFE3F2FD),

    secondary = Color(0xFF4DD0E1),    // Lighter Cyan
    secondaryContainer = Color(0xFF00838F),
    onSecondaryContainer = Color(0xFFE0F7FA),

    surface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFF26282B),
    onSurface = Color(0xFFE1E3E6),
    onSurfaceVariant = Color(0xFFBEC2C6),

    background = Color(0xFF121212),
    onBackground = Color(0xFFE1E3E6),

    error = Color(0xFFEF5350),
    errorContainer = Color(0xFF442726),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3),      // Bright Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),  // Light Blue background
    onPrimaryContainer = Color(0xFF004B91),

    secondary = Color(0xFF00BCD4),    // Cyan
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF006064),

    surface = Color(0xFFFAFAFA),      // Almost white with very slight blue tint
    surfaceVariant = Color(0xFFF0F4F8),  // Light blue-grey
    onSurface = Color(0xFF1A1A1A),    // Almost black
    onSurfaceVariant = Color(0xFF4A5568),  // Medium grey

    background = Color(0xFFF8FAFC),    // Very light blue-grey
    onBackground = Color(0xFF1A1A1A),

    error = Color(0xFFE53935),        // Bright red
    errorContainer = Color(0xFFFFEBEE),
    onError = Color.White
)

@Composable
fun MyNextBARTTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}