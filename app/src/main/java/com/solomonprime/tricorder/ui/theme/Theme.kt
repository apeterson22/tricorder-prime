package com.solomonprime.tricorder.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LcarsColorScheme = darkColorScheme(
    primary = LcarsOrange,
    onPrimary = LcarsBlack,
    secondary = LcarsBlue,
    onSecondary = LcarsBlack,
    tertiary = LcarsPink,
    background = LcarsBlack,
    onBackground = LcarsWhite,
    surface = LcarsBlack,
    onSurface = LcarsWhite,
    error = LcarsRed,
    onError = LcarsBlack
)


@Composable
fun TricorderPrimeTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LcarsColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
