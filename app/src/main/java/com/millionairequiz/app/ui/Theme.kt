package com.millionairequiz.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Palette {
    val Navy = Color(0xFF070B34)
    val Purple = Color(0xFF1C0F55)
    val Panel = Color(0xFF0E1A5C)
    val DialogBg = Color(0xFF121C5E)
    val Gold = Color(0xFFF5C542)
    val Silver = Color(0xFFC9D1E8)
    val Orange = Color(0xFFF39C12)
    val Green = Color(0xFF1FA55A)
    val Red = Color(0xFFC0392B)
    val SoftRed = Color(0xFFFF8A80)
    val SoftGreen = Color(0xFF7CF0A8)
    val White = Color.White

    val background = Brush.verticalGradient(listOf(Navy, Purple, Navy))
}

private val colors = darkColorScheme(
    primary = Palette.Gold,
    onPrimary = Palette.Navy,
    secondary = Palette.Orange,
    onSecondary = Palette.Navy,
    background = Palette.Navy,
    onBackground = Palette.White,
    surface = Palette.DialogBg,
    onSurface = Palette.White,
    onSurfaceVariant = Palette.Silver,
    surfaceContainerHigh = Palette.DialogBg,
    surfaceContainer = Palette.DialogBg,
    outline = Palette.Silver,
)

@Composable
fun MillionaireTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
