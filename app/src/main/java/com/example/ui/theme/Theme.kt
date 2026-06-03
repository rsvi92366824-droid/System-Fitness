package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val SoloDarkColorScheme = darkColorScheme(
    primary = SystemHoloCyan,
    secondary = SystemElectricBlue,
    tertiary = SystemGoldAccent,
    background = SystemDarkBlack,
    surface = SystemOffBlack,
    onPrimary = SystemDarkBlack,
    onSecondary = SystemTextLight,
    onBackground = SystemTextLight,
    onSurface = SystemTextLight,
    error = SystemWarningRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark mode for Solo Leveling system authenticity
    dynamicColor: Boolean = false, // Dissuade generic pastel system colors
    content: @Composable () -> Unit
) {
    val colorScheme = SoloDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SystemDarkBlack.toArgb()
            window.navigationBarColor = SystemDarkBlack.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
