package com.graffitistudio.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class BrushType {
    BASIC_BRUSH,
    ERASER
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
    val type: BrushType = BrushType.BASIC_BRUSH,
    val colorArgb: Int = 0xFFB8FF3B.toInt(),
    val sizePx: Float = 24f,
    val opacity: Float = 1f,
    val hardness: Float = 0.85f,
    val stabilizerLevel: Float = 0.35f
)

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
data class LayerData(
    val id: String,
    val name: String,
    val isVisible: Boolean = true,
    val opacity: Float = 1f,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val strokes: List<StrokeData> = emptyList()
)

@Serializable
data class ArtworkDocument(
    val projectId: String,
    val name: String,
    val width: Int,
    val height: Int,
    val backgroundColorArgb: Int = 0xFF14161C.toInt(),
    val layers: List<LayerData> = emptyList()
)

data class ProjectMetadata(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val backgroundColorArgb: Int,
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
