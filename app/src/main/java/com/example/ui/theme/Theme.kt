package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DojoCrimson,
    onPrimary = Color.White,
    primaryContainer = DojoCrimsonDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    secondaryContainer = DojoDarkSurfaceVariant,
    onSecondaryContainer = TextPrimaryDark,
    background = DojoBlack,
    onBackground = TextPrimaryDark,
    surface = DojoDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DojoDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF374151)
)

// Stitch Authoritative Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = DojoCrimson,
    onPrimary = Color.White,
    primaryContainer = DojoCrimsonDark,
    onPrimaryContainer = Color.White,
    secondary = DojoCrimsonDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEE2E2),
    onSecondaryContainer = DojoCrimsonDark,
    background = DojoWarmCanvas,               // #F7F6F3 Tatami warm ground canvas
    onBackground = DojoTextPrimary,            // #20232B Primary high-contrast text
    surface = DojoCardSurface,                 // #FFFFFF Pure white card surfaces
    onSurface = DojoTextPrimary,               // #20232B Card content ink
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = DojoTextSecondary,      // #777D87 Secondary text / captions
    outline = DojoBorder,                      // #E6E6E8 Fine 1px card boundary
    outlineVariant = DojoBorderSubtle
)

@Composable
fun DojoFlowTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
