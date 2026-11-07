package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val UnifiedLightMushafColorScheme = lightColorScheme(
    primary = EmeraldGreen,
    onPrimary = Color.White,
    primaryContainer = RoyalNavyContainer,
    onPrimaryContainer = DeepCharcoal,
    secondary = GoldAccent,
    onSecondary = Color.White,
    secondaryContainer = RoyalNavyDark,
    onSecondaryContainer = DeepCharcoal,
    background = AgedParchment,
    onBackground = DeepCharcoal,
    surface = ParchmentCard,
    onSurface = DeepCharcoal,
    surfaceVariant = RoyalNavyContainer,
    onSurfaceVariant = Color(0xFF4A3B2C),
    outline = GoldBorder
)

@Composable
fun KhatmaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = UnifiedLightMushafColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ParchmentCard.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
