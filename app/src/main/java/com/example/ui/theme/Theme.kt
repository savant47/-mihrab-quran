package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val UnifiedMihrabiColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = RoyalNavyBg,
    primaryContainer = RoyalNavyContainer,
    onPrimaryContainer = GoldLight,
    secondary = EmeraldLight,
    onSecondary = TextLight,
    background = RoyalNavyBg,
    onBackground = TextLight,
    surface = RoyalNavyDark,
    onSurface = TextLight,
    surfaceVariant = RoyalNavyContainer,
    onSurfaceVariant = GoldLight
)

@Composable
fun KhatmaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = UnifiedMihrabiColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = RoyalNavyBg.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
