package com.whattocook.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.whattocook.app.domain.model.ThemeMode

private val DarkScheme = darkColorScheme(
    primary = Ember,
    onPrimary = White,
    primaryContainer = DarkAppColors.accentSoft,
    onPrimaryContainer = Cream,
    secondary = Cream,
    onSecondary = Ink,
    background = Black,
    onBackground = Cream,
    surface = Charcoal,
    onSurface = Cream,
    surfaceVariant = CharcoalRaised,
    onSurfaceVariant = Stone,
    surfaceContainer = Charcoal,
    surfaceContainerHigh = CharcoalRaised,
    surfaceContainerHighest = CharcoalRaised,
    surfaceContainerLow = Charcoal,
    outline = CharcoalBorder,
    outlineVariant = CharcoalBorder,
    error = Coral,
    onError = White,
    inverseSurface = Cream,
    inverseOnSurface = Ink,
)

private val LightScheme = lightColorScheme(
    primary = Ember,
    onPrimary = White,
    primaryContainer = LightAppColors.accentSoft,
    onPrimaryContainer = Ink,
    secondary = Ink,
    onSecondary = Cream,
    background = OffWhite,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = InkSoft,
    surfaceContainer = White,
    surfaceContainerHigh = Sand,
    surfaceContainerHighest = Sand,
    surfaceContainerLow = White,
    outline = SandBorder,
    outlineVariant = SandBorder,
    error = Coral,
    onError = White,
    inverseSurface = Ink,
    inverseOnSurface = Cream,
)

private val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

/**
 * Largest system font scale the app follows. At 1.0 the layout looks the same even when
 * the phone's font size is maxed out; smaller-than-default font sizes are still honoured.
 * Raise it (e.g. to 1.15f) if you want to allow some growth for accessibility.
 */
const val MAX_FONT_SCALE = 1.0f

@Composable
fun isAppInDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun WhatToCookTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val density = LocalDensity.current
    val cappedDensity = remember(density) {
        Density(density = density.density, fontScale = density.fontScale.coerceAtMost(MAX_FONT_SCALE))
    }
    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalDensity provides cappedDensity,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Access app-specific colors: `AppTheme.colors.accent`. */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}
