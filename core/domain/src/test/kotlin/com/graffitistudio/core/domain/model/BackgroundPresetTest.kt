package com.graffitistudio.core.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundPresetTest {

    @Test
    fun `builtIn presets cover all BackgroundSurfaceType entries`() {
        val surfaces = BackgroundPresets.builtIn.map { it.surface }.toSet()
        assertEquals(BackgroundSurfaceType.entries.toSet(), surfaces)
    }

    @Test
    fun `artwork document preserves backgroundSurface in serialization`() {
        val doc = ArtworkDocument(
            projectId = "p-bg",
            name = "Brick Piece",
            width = 1080,
            height = 1080,
            backgroundColorArgb = 0xFF5A2E26.toInt(),
            backgroundSurface = BackgroundSurfaceType.BRICK_WALL
        )

        val json = Json.encodeToString(doc)
        val decoded = Json.decodeFromString<ArtworkDocument>(json)
        assertEquals(doc, decoded)
    }
}
