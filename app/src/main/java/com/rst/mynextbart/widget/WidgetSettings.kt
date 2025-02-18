package com.rst.mynextbart.widget

import kotlinx.serialization.Serializable

@Serializable
enum class WidgetTheme {
    SYSTEM,
    LIGHT,
    DARK
}

@Serializable
data class WidgetAppearance(
    val theme: WidgetTheme = WidgetTheme.SYSTEM,
    val backgroundAlpha: Float = 1.0f
) 