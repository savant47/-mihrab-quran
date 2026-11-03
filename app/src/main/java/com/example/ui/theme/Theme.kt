package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = DeepCharcoal,
    primaryContainer = RoyalNavyContainer,
    onPrimaryContainer = GoldLight,
    secondary = EmeraldLight,
    onSecondary = TextLight,
    background = DeepCharcoal,
    onBackground = TextLight,
    surface = RoyalNavyDark,
    onSurface = TextLight,
    surfaceVariant = RoyalNavyContainer,
    onSurfaceVariant = GoldLight
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalNavyDark,
    onPrimary = TextLight,
    primaryContainer = GoldLight,
    onPrimaryContainer = RoyalNavyDark,
    secondary = EmeraldGreen,
    onSecondary = TextLight,
    background = AgedParchment,
    onBackground = TextDark,
    surface = ParchmentCard,
    onSurface = TextDark,
    surfaceVariant = AgedParchment,
    onSurfaceVariant = TextDark
)

@Composable
fun KhatmaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
