package com.graffitistudio.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportModelsTest {

    @Test
    fun `resolveDimensions scales and clamps to maxEdgePx`() {
        val half = ExportConfig(scale = ExportScale.HALF).resolveDimensions(1080, 1920)
        assertEquals(540 to 960, half)

        val doubled = ExportConfig(scale = ExportScale.DOUBLE).resolveDimensions(1080, 1080)
        assertEquals(2160 to 2160, doubled)

        val clamped = ExportConfig(scale = ExportScale.DOUBLE).resolveDimensions(3000, 2000, maxEdgePx = 4096)
        assertEquals(4096 to 2731, clamped)
    }

    @Test
    fun `effectiveTransparent is only true for PNG`() {
        assertTrue(ExportConfig(ExportFormat.PNG, transparentBackground = true).effectiveTransparent)
        assertFalse(ExportConfig(ExportFormat.JPEG, transparentBackground = true).effectiveTransparent)
    }
}
