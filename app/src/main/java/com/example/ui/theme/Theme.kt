package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EcoDarkColorScheme = darkColorScheme(
    primary = EcoPrimary,
    onPrimary = Color.Black,
    primaryContainer = EcoPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = EcoSecondary,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = TextPrimary,
    tertiary = EcoTertiary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceElevated,
    outlineVariant = EcoFocusGlow
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Smart TV interface is always dark-themed for living room viewing and energy awareness
    MaterialTheme(
        colorScheme = EcoDarkColorScheme,
        typography = Typography,
        content = content
    )
}

