package com.waitaminutedigital.biblestudy

import androidx.compose.ui.graphics.Color

val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val CardSurfaceDark = Color(0xFF2A2A2A)
val GoldAccent = Color(0xFFFFC107)
val GoldText = Color(0xFFE5C158)
val White = Color(0xFFFFFFFF)
val LightGray = Color(0xFFB0B0B0)
val RedLetterColor = Color(0xFFEF5350)

fun parseHexColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    return try {
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(0xFF000000 or colorInt)
        } else if (cleanHex.length == 8) {
            Color(colorInt)
        } else {
            Color(0xFFFFC107)
        }
    } catch (_: Exception) {
        Color(0xFFFFC107)
    }
}
