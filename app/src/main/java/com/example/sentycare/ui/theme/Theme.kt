package com.example.sentycare.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SentyCareColorScheme = lightColorScheme(
    primary = DarkBlue,
    onPrimary = BluishWhite,
    primaryContainer = MediumBlue,
    onPrimaryContainer = BluishWhite,
    secondary = AccentGreen,
    onSecondary = BluishWhite,
    background = BluishWhite,
    onBackground = DarkGray,
    surface = BluishWhite,
    onSurface = DarkGray,
    error = androidx.compose.ui.graphics.Color(0xFFB00020),
    onError = BluishWhite
)

@Composable
fun SentyCareTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBlue.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = SentyCareColorScheme,
        typography = Typography,
        content = content
    )
}