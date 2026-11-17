package com.graffitistudio.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class UrbanColors(
    val backgroundDeep: Color = Color(0xFF0D0E12),
    val surfacePrimary: Color = Color(0xFF14161C),
    val surfaceElevated: Color = Color(0xFF1C1F27),
    val surfaceHighlight: Color = Color(0xFF252934),
    val borderSubtle: Color = Color(0xFF2C313D),
    val textPrimary: Color = Color(0xFFF2F4F8),
    val textSecondary: Color = Color(0xFFA0A7B8),
    val textMuted: Color = Color(0xFF666D7E),
    val accentLime: Color = Color(0xFFB8FF3B),
    val accentCyan: Color = Color(0xFF2EE6D6),
    val accentCoral: Color = Color(0xFFFF4D6D),
    val accentAmber: Color = Color(0xFFFFB703),
    val onAccent: Color = Color(0xFF0D0E12)
)

val LocalUrbanColors = staticCompositionLocalOf { UrbanColors() }

object UrbanPalette {
    val studioSwatches = listOf(
        0xFFB8FF3B.toInt(), // Electric Lime
        0xFF2EE6D6.toInt(), // Urban Cyan
        0xFFFF4D6D.toInt(), // Signal Coral
        0xFFFFB703.toInt(), // Caution Amber
        0xFFF2F4F8.toInt(), // Chalk White
        0xFF9B5DE5.toInt(), // Street Violet
        0xFFFF6B35.toInt(), // Aerosol Orange
        0xFF1A1D24.toInt()  // Asphalt Black
    )

    val canvasBackgrounds = listOf(
        0xFF14161C.toInt(), // Dark Concrete
        0xFF232730.toInt(), // Slate Wall
        0xFFF4F4F0.toInt(), // Raw Paper
        0xFF000000.toInt()  // Pure Black
    )
}
