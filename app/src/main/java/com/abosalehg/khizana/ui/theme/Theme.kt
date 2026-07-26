package com.abosalehg.khizana.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.abosalehg.khizana.domain.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = WoodBase,
    onPrimary = Parchment,
    primaryContainer = WoodLight,
    onPrimaryContainer = Parchment,
    secondary = Gold,
    onSecondary = Ink,
    secondaryContainer = GoldSoft,
    onSecondaryContainer = Ink,
    tertiary = WoodLight,
    onTertiary = Parchment,
    background = Parchment,
    onBackground = Ink,
    surface = ParchmentHigh,
    onSurface = Ink,
    surfaceVariant = ParchmentDim,
    onSurfaceVariant = InkSoft,
    outline = WoodLight,
    error = ErrorRed,
    onError = Parchment
)

private val DarkColorScheme = darkColorScheme(
    primary = GoldMuted,
    onPrimary = Ink,
    primaryContainer = WoodCavity,
    onPrimaryContainer = Parchment,
    secondary = GoldSoftMuted,
    onSecondary = Ink,
    secondaryContainer = WoodCavity,
    onSecondaryContainer = GoldSoftMuted,
    tertiary = WoodLight,
    onTertiary = Parchment,
    background = WoodDeep,
    onBackground = Parchment,
    surface = WoodNight,
    onSurface = Parchment,
    surfaceVariant = WoodCavity,
    onSurfaceVariant = ParchmentSoft,
    outline = WoodLight,
    error = ErrorRedNight,
    onError = Ink
)

@Composable
fun KhizanaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val woodTokens = if (darkTheme) DarkWoodTokens else LightWoodTokens

    CompositionLocalProvider(LocalWoodTokens provides woodTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KhizanaTypography,
            content = content
        )
    }
}
