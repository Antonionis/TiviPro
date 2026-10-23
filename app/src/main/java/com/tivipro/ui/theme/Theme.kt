package com.tivipro.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PolTiviDarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = TextPrimary,
    primaryContainer = SurfaceDarkVariant,
    onPrimaryContainer = BlueLight,
    secondary = BlueSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = SurfaceDarkCard,
    onSecondaryContainer = TextSecondary,
    tertiary = BlueAccent,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDarkCard,
    onSurfaceVariant = TextSecondary,
    error = StatusError,
    onError = TextPrimary
)

@Composable
fun PolTiviTheme(
    darkTheme: Boolean = true, // Force dark POLTIVI theme by default
    content: @Composable () -> Unit
) {
    val colorScheme = PolTiviDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
