package com.graffitistudio.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class BrushType(val displayName: String) {
    PENCIL("Pencil"),
    PEN("Pen"),
    MARKER("Marker"),
    AIRBRUSH("Airbrush"),
    SPRAY_PAINT("Spray"),
    TEXTURE_BRUSH("Texture"),
    ERASER("Eraser")
}

@Serializable
enum class BlendMode {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    DARKEN,
    LIGHTEN
}

@Serializable
data class BrushConfig(
    val type: BrushType = BrushType.PEN,
    val colorArgb: Int = 0xFFB8FF3B.toInt(),
    val sizePx: Float = 24f,
    val opacity: Float = 1f,
    val flow: Float = 0.85f,
    val hardness: Float = 0.85f,
    val stabilizerLevel: Float = 0.35f,
    val scatter: Float = 0.45f,
    val seed: Long = 0L
)

@Serializable
data class BrushPreset(
    val id: String,
    val name: String,
    val brush: BrushConfig,
    val isCustom: Boolean = false
)

object BrushPresets {
    val builtIn: List<BrushPreset> = listOf(
        BrushPreset(
            id = "builtin_spray_fat",
            name = "Fat Cap Spray",
            brush = BrushConfig(
                type = BrushType.SPRAY_PAINT,
                sizePx = 56f,
                opacity = 0.9f,
                flow = 0.8f,
                scatter = 0.65f,
                stabilizerLevel = 0.25f
            )
        ),
        BrushPreset(
            id = "builtin_tag_pen",
            name = "Tag Pen",
            brush = BrushConfig(
                type = BrushType.PEN,
                sizePx = 14f,
                opacity = 1f,
                flow = 1f,
                stabilizerLevel = 0.45f
            )
        ),
        BrushPreset(
            id = "builtin_chisel_marker",
            name = "Chisel Marker",
            brush = BrushConfig(
                type = BrushType.MARKER,
                sizePx = 36f,
                opacity = 0.78f,
                flow = 0.9f,
                stabilizerLevel = 0.35f
            )
        ),
        BrushPreset(
            id = "builtin_airbrush",
            name = "Soft Airbrush",
            brush = BrushConfig(
                type = BrushType.AIRBRUSH,
                sizePx = 64f,
                opacity = 0.75f,
                flow = 0.55f,
                hardness = 0.2f,
                stabilizerLevel = 0.3f
            )
        ),
        BrushPreset(
            id = "builtin_pencil",
            name = "Sketch Pencil",
            brush = BrushConfig(
                type = BrushType.PENCIL,
                sizePx = 8f,
                opacity = 0.85f,
                flow = 0.75f,
                stabilizerLevel = 0.2f
            )
        ),
        BrushPreset(
            id = "builtin_texture",
            name = "Concrete Grit",
            brush = BrushConfig(
                type = BrushType.TEXTURE_BRUSH,
                sizePx = 42f,
                opacity = 0.85f,
                flow = 0.7f,
                scatter = 0.4f,
                stabilizerLevel = 0.25f
            )
        )
    )
}

@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val timestampMs: Long = 0L
)

@Serializable
data class StrokeData(
    val id: String,
    val layerId: String,
    val brush: BrushConfig,
    val points: List<StrokePoint>
)

@Serializable
enum class GraffitiTextStyle(val displayName: String) {
    STANDARD("Standard"),
    BUBBLE("Bubble"),
    CHROME("Chrome"),
    BLOCK_3D("3D Block")
}

@Serializable
data class GraffitiTextElement(
    val id: String,
    val layerId: String,
    val text: String,
    val x: Float,
    val y: Float,
    val fontSizePx: Float = 140f,
    val fontFamily: String = "sans-serif",
    val letterSpacing: Float = 0.06f,
    val style: GraffitiTextStyle = GraffitiTextStyle.BUBBLE,
    val primaryColorArgb: Int = 0xFFB8FF3B.toInt(),
    val secondaryColorArgb: Int = 0xFF2EE6D6.toInt(),
    val useGradientFill: Boolean = true,
    val outlineColorArgb: Int = 0xFF0D0E12.toInt(),
    val outlineWidthPx: Float = 14f,
    val shadowEnabled: Boolean = true,
    val shadowOffsetPx: Float = 12f,
    val extrusionDepthPx: Float = 18f,
    val extrusionColorArgb: Int = 0xFF1C1F27.toInt()
)

@Serializable
enum class EffectType(val displayName: String) {
    SPRAY_TEXTURE("Spray Texture"),
    PAINT_SPLATTER("Paint Splatter"),
    GLOW("Glow"),
    BLUR("Blur"),
    NOISE("Noise"),
    HALFTONE("Halftone")
}

@Serializable
data class LayerEffect(
    val id: String,
    val type: EffectType,
    val intensity: Float = 0.6f,
    val radiusPx: Float = 16f,
    val colorArgb: Int = 0xFFB8FF3B.toInt(),
    val seed: Long = 0L
)

@Serializable
data class LayerData(
    val id: String,
    val name: String,
    val isVisible: Boolean = true,
    val opacity: Float = 1f,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val strokes: List<StrokeData> = emptyList(),
    val textElements: List<GraffitiTextElement> = emptyList(),
    val effects: List<LayerEffect> = emptyList()
)

@Serializable
enum class BackgroundSurfaceType(val displayName: String) {
    SOLID("Solid Color"),
    BRICK_WALL("Brick Wall"),
    CONCRETE_WALL("Raw Concrete"),
    METAL_SHUTTER("Metal Shutter"),
    SUBWAY_TILE("Subway Tile"),
    BLACKBOOK_PAPER("Sketch Paper")
}

data class BackgroundPreset(
    val surface: BackgroundSurfaceType,
    val defaultColorArgb: Int,
    val subtitle: String
)

object BackgroundPresets {
    val builtIn: List<BackgroundPreset> = listOf(
        BackgroundPreset(BackgroundSurfaceType.SOLID, 0xFF14161C.toInt(), "Clean dark studio"),
        BackgroundPreset(BackgroundSurfaceType.BRICK_WALL, 0xFF5A2E26.toInt(), "Weathered street brick"),
        BackgroundPreset(BackgroundSurfaceType.CONCRETE_WALL, 0xFF3A3E47.toInt(), "Poured urban concrete"),
        BackgroundPreset(BackgroundSurfaceType.METAL_SHUTTER, 0xFF2B323D.toInt(), "Corrugated shop roll"),
        BackgroundPreset(BackgroundSurfaceType.SUBWAY_TILE, 0xFFD9DDD8.toInt(), "Metro station tile"),
        BackgroundPreset(BackgroundSurfaceType.BLACKBOOK_PAPER, 0xFFEAE6DC.toInt(), "Heavy sketch grain")
    )
}

@Serializable
data class ArtworkDocument(
    val projectId: String,
    val name: String,
    val width: Int,
    val height: Int,
    val backgroundColorArgb: Int = 0xFF14161C.toInt(),
    val backgroundSurface: BackgroundSurfaceType = BackgroundSurfaceType.SOLID,
    val background: DocumentBackground? = null,
    val layers: List<LayerData> = emptyList()
)

data class ProjectMetadata(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val backgroundColorArgb: Int,
    val backgroundSurface: BackgroundSurfaceType = BackgroundSurfaceType.SOLID,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val gspFilePath: String,
    val thumbnailPath: String? = null
)

data class CanvasPreset(
    val label: String,
    val width: Int,
    val height: Int
) {
    companion object {
        val defaults = listOf(
            CanvasPreset("Square 1:1", 1080, 1080),
            CanvasPreset("Wall 16:9", 1920, 1080),
            CanvasPreset("Poster 3:4", 1440, 1920),
            CanvasPreset("Studio HD", 2048, 2048)
        )
    }
}
