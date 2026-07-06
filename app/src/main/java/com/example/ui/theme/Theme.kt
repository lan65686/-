package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BiankyRoyalBlue,
    onPrimary = BiankyWhite,
    secondary = BiankyDeepBlue,
    onSecondary = BiankyWhite,
    background = BiankyTextDark,
    onBackground = BiankyWhite,
    surface = BiankyTextDark,
    onSurface = BiankyWhite,
    error = BiankyError
)

private val LightColorScheme = lightColorScheme(
    primary = BiankyDeepBlue,
    onPrimary = BiankyWhite,
    secondary = BiankyRoyalBlue,
    onSecondary = BiankyWhite,
    background = BiankyWhite,
    onBackground = BiankyTextDark,
    surface = BiankyWhite,
    onSurface = BiankyTextDark,
    error = BiankyError,
    surfaceVariant = BiankyLightBlue,
    onSurfaceVariant = BiankyDeepBlue
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled to enforce the requested branding
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
