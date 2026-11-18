package com.graffitistudio.core.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

enum class BackgroundCategory(val displayName: String, val folderName: String) {
    CONCRETE_WALL("Concrete Wall", "concrete"),
    BRICK_WALL("Brick Wall", "brick"),
    BLACKBOOK_PAPER("Blackbook Paper", "blackbook"),
    GRUNGE_TEXTURE("Grunge Texture", "grunge"),
    PAINT_SPLATTER("Paint Splatter", "splatter"),
    ABSTRACT_URBAN("Abstract Urban", "abstract"),
    NEON_STREET("Neon Street", "neon"),
    MINIMALIST_CANVAS("Minimalist Canvas", "minimalist")
}

@Serializable
data class BackgroundAsset(
    val id: String,
    val name: String,
    val category: String,
    val width: Int,
    val height: Int,
    val source: String,
    val license: String,
    val attribution: String,
    val assetPath: String,
    val previewPath: String
)

@Serializable
enum class BackgroundScaleMode(val displayName: String) {
    FIT("Fit"),
    FILL("Fill"),
    ORIGINAL_SIZE("Original"),
    CROP("Crop")
}

@Serializable
data class DocumentBackground(
    val assetId: String? = null,
    val assetPath: String? = null,
    val baseColorArgb: Int = 0xFF14161C.toInt(),
    val scaleMode: BackgroundScaleMode = BackgroundScaleMode.FILL,
    val rotationDegrees: Float = 0f,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val opacity: Float = 1f,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f
) {
    /**
     * Computes the base scale factor for rendering an image of size ([srcW], [srcH])
     * into a document of size ([docW], [docH]) according to [scaleMode] and [scale].
     */
    fun computeEffectiveScale(srcW: Int, srcH: Int, docW: Int, docH: Int): Float {
        if (srcW <= 0 || srcH <= 0 || docW <= 0 || docH <= 0) return scale
        val baseScale = when (scaleMode) {
            BackgroundScaleMode.FIT -> min(docW / srcW.toFloat(), docH / srcH.toFloat())
            BackgroundScaleMode.FILL -> max(docW / srcW.toFloat(), docH / srcH.toFloat())
            BackgroundScaleMode.ORIGINAL_SIZE -> 1f
            BackgroundScaleMode.CROP -> {
                val croppedW = max(1f, srcW * (cropRight - cropLeft).coerceIn(0.1f, 1f))
                val croppedH = max(1f, srcH * (cropBottom - cropTop).coerceIn(0.1f, 1f))
                max(docW / croppedW, docH / croppedH)
            }
        }
        return baseScale * scale.coerceIn(0.2f, 5f)
    }
}

enum class BackgroundFilterTab(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    RECENT("Recent")
}

object BackgroundCatalogFilter {
    fun filter(
        assets: List<BackgroundAsset>,
        tab: BackgroundFilterTab,
        selectedCategory: BackgroundCategory?,
        query: String,
        favoriteIds: Set<String>,
        recentIds: List<String>
    ): List<BackgroundAsset> {
        val base = when (tab) {
            BackgroundFilterTab.ALL -> assets
            BackgroundFilterTab.FAVORITES -> assets.filter { it.id in favoriteIds }
            BackgroundFilterTab.RECENT -> {
                val byId = assets.associateBy { it.id }
                recentIds.mapNotNull { byId[it] }
            }
        }

        val byCategory = if (selectedCategory != null) {
            base.filter { it.category.equals(selectedCategory.displayName, ignoreCase = true) }
        } else {
            base
        }

        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return byCategory
        return byCategory.filter { asset ->
            asset.name.contains(trimmedQuery, ignoreCase = true) ||
                asset.category.contains(trimmedQuery, ignoreCase = true) ||
                asset.source.contains(trimmedQuery, ignoreCase = true)
        }
    }
}
