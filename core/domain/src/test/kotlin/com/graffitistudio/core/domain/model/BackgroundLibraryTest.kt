package com.graffitistudio.core.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundLibraryTest {

    private val sampleAssets = listOf(
        BackgroundAsset(
            id = "concrete_1",
            name = "Raw Poured Concrete",
            category = "Concrete Wall",
            width = 512,
            height = 512,
            source = "Graffiti Studio",
            license = "CC0",
            attribution = "Graffiti Studio",
            assetPath = "backgrounds/concrete/concrete_1.png",
            previewPath = "backgrounds/previews/concrete_1.png"
        ),
        BackgroundAsset(
            id = "brick_1",
            name = "Downtown Red Brick",
            category = "Brick Wall",
            width = 512,
            height = 512,
            source = "Graffiti Studio",
            license = "CC0",
            attribution = "Graffiti Studio",
            assetPath = "backgrounds/brick/brick_1.png",
            previewPath = "backgrounds/previews/brick_1.png"
        )
    )

    @Test
    fun `category, favorites, recents and search filter accurately`() {
        val brickOnly = BackgroundCatalogFilter.filter(
            assets = sampleAssets,
            tab = BackgroundFilterTab.ALL,
            selectedCategory = BackgroundCategory.BRICK_WALL,
            query = "",
            favoriteIds = emptySet(),
            recentIds = emptyList()
        )
        assertEquals(listOf("brick_1"), brickOnly.map { it.id })

        val favs = BackgroundCatalogFilter.filter(
            assets = sampleAssets,
            tab = BackgroundFilterTab.FAVORITES,
            selectedCategory = null,
            query = "",
            favoriteIds = setOf("concrete_1"),
            recentIds = emptyList()
        )
        assertEquals(listOf("concrete_1"), favs.map { it.id })

        val searched = BackgroundCatalogFilter.filter(
            assets = sampleAssets,
            tab = BackgroundFilterTab.ALL,
            selectedCategory = null,
            query = "poured",
            favoriteIds = emptySet(),
            recentIds = emptyList()
        )
        assertEquals(listOf("concrete_1"), searched.map { it.id })
    }

    @Test
    fun `scale modes compute expected FIT FILL ORIGINAL and CROP factors`() {
        val fit = DocumentBackground(scaleMode = BackgroundScaleMode.FIT)
            .computeEffectiveScale(srcW = 500, srcH = 1000, docW = 1000, docH = 1000)
        assertEquals(1f, fit, 0.001f)

        val fill = DocumentBackground(scaleMode = BackgroundScaleMode.FILL)
            .computeEffectiveScale(srcW = 500, srcH = 1000, docW = 1000, docH = 1000)
        assertEquals(2f, fill, 0.001f)

        val original = DocumentBackground(scaleMode = BackgroundScaleMode.ORIGINAL_SIZE, scale = 1.5f)
            .computeEffectiveScale(srcW = 500, srcH = 1000, docW = 1000, docH = 1000)
        assertEquals(1.5f, original, 0.001f)

        val crop = DocumentBackground(
            scaleMode = BackgroundScaleMode.CROP,
            cropLeft = 0.25f,
            cropTop = 0.25f,
            cropRight = 0.75f,
            cropBottom = 0.75f
        ).computeEffectiveScale(srcW = 500, srcH = 500, docW = 1000, docH = 1000)
        assertEquals(4f, crop, 0.001f)
    }

    @Test
    fun `document background serializes separately from layers`() {
        val doc = ArtworkDocument(
            projectId = "proj-1",
            name = "Wall",
            width = 1080,
            height = 1080,
            background = DocumentBackground(
                assetId = "brick_1",
                assetPath = "backgrounds/brick/brick_1.png",
                scaleMode = BackgroundScaleMode.CROP,
                rotationDegrees = 15f,
                scale = 1.25f,
                offsetX = 40f,
                offsetY = -20f,
                opacity = 0.85f
            )
        )

        val encoded = Json.encodeToString(doc)
        val decoded = Json.decodeFromString<ArtworkDocument>(encoded)
        assertEquals(doc, decoded)
    }
}
