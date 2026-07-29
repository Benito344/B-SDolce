package com.boulangerie.pro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette inspirée d\'une boulangerie artisanale : tons chauds, doré, brun.
private val Amber = Color(0xFFD98E2B)
private val AmberDark = Color(0xFFB57A1F)
private val Crust = Color(0xFF8B5E34)
private val Cream = Color(0xFFFDF6EC)
private val Charcoal = Color(0xFF1F1B16)

private val LightColors = lightColorScheme(
    primary = Amber,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5D9A8),
    onPrimaryContainer = Color(0xFF3A2A0F),
    secondary = Crust,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8D5BE),
    onSecondaryContainer = Color(0xFF2E2014),
    tertiary = Color(0xFF6B8E23),
    onTertiary = Color.White,
    background = Cream,
    onBackground = Charcoal,
    surface = Color(0xFFFFFBF5),
    onSurface = Charcoal,
    surfaceVariant = Color(0xFFF2E6D3),
    onSurfaceVariant = Color(0xFF52443A),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF847468),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF5C074),
    onPrimary = Color(0xFF4A330F),
    primaryContainer = AmberDark,
    onPrimaryContainer = Color(0xFFF5D9A8),
    secondary = Color(0xFFD0B896),
    onSecondary = Color(0xFF3E2D1C),
    secondaryContainer = Color(0xFF553F2A),
    onSecondaryContainer = Color(0xFFE8D5BE),
    tertiary = Color(0xFFB7D18B),
    onTertiary = Color(0xFF233608),
    background = Color(0xFF17120C),
    onBackground = Color(0xFFEDE0CC),
    surface = Color(0xFF211A12),
    onSurface = Color(0xFFEDE0CC),
    surfaceVariant = Color(0xFF52443A),
    onSurfaceVariant = Color(0xFFD6C3B4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    outline = Color(0xFF9E8D80),
)

@Composable
fun BoulangerieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BoulangerieTypography,
        content = content,
    )
}
