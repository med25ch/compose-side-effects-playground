package com.sideeffects.playground.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val primaryColor = Color(0xFF6650A4)
private val badColor = Color(0xFFB00020)
private val goodColor = Color(0xFF1B5E20)

private val LightColors = lightColorScheme(
    primary = primaryColor,
    onPrimary = Color.White,
    secondary = Color(0xFF625B71),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    secondary = Color(0xFFCCC2DC),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
)

val BadRed = Color(0xFFFFEBEE)
val BadRedBorder = Color(0xFFEF9A9A)
val GoodGreen = Color(0xFFE8F5E9)
val GoodGreenBorder = Color(0xFFA5D6A7)
val InfoBlue = Color(0xFFE3F2FD)
val InfoBlueBorder = Color(0xFF90CAF9)
val WarningAmber = Color(0xFFFFF8E1)
val WarningAmberBorder = Color(0xFFFFCC02)
val CodeBackground = Color(0xFF1E1E2E)
val LogBackground = Color(0xFF0D1117)

@Composable
fun PlaygroundTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
