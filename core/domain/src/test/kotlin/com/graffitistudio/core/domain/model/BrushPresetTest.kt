package com.graffitistudio.core.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrushPresetTest {

    @Test
    fun `builtIn presets cover all non-eraser brush types`() {
        val types = BrushPresets.builtIn.map { it.brush.type }.toSet()
        assertTrue(BrushType.PENCIL in types)
        assertTrue(BrushType.PEN in types)
        assertTrue(BrushType.MARKER in types)
        assertTrue(BrushType.AIRBRUSH in types)
        assertTrue(BrushType.SPRAY_PAINT in types)
        assertTrue(BrushType.TEXTURE_BRUSH in types)
    }

    @Test
    fun `brush config serialization round-trip preserves seed and dynamics`() {
        val original = BrushConfig(
            type = BrushType.SPRAY_PAINT,
            colorArgb = 0xFF2EE6D6.toInt(),
            sizePx = 48f,
            opacity = 0.85f,
            flow = 0.7f,
            hardness = 0.4f,
            stabilizerLevel = 0.5f,
            scatter = 0.65f,
            seed = 987654321L
        )
        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<BrushConfig>(json)
        assertEquals(original, decoded)
    }
}
