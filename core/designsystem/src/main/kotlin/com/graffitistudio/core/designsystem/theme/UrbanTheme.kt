package com.graffitistudio.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun GraffitiStudioTheme(
    content: @Composable () -> Unit
) {
    val urbanColors = UrbanColors()
    val colorScheme = darkColorScheme(
        primary = urbanColors.accentLime,
        onPrimary = urbanColors.onAccent,
        secondary = urbanColors.accentCyan,
        onSecondary = urbanColors.onAccent,
        error = urbanColors.accentCoral,
        background = urbanColors.backgroundDeep,
        onBackground = urbanColors.textPrimary,
        surface = urbanColors.surfacePrimary,
        onSurface = urbanColors.textPrimary,
        surfaceVariant = urbanColors.surfaceElevated,
        onSurfaceVariant = urbanColors.textSecondary,
        outline = urbanColors.borderSubtle
    )

    CompositionLocalProvider(LocalUrbanColors provides urbanColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = UrbanTypography,
            content = content
        )
    }
}
