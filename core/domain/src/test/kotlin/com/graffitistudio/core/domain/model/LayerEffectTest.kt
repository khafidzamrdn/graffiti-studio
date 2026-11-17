package com.graffitistudio.core.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class LayerEffectTest {

    @Test
    fun `layer effects serialize cleanly inside LayerData`() {
        val layer = LayerData(
            id = "layer-fx",
            name = "FX Layer",
            effects = EffectType.entries.mapIndexed { index, type ->
                LayerEffect(
                    id = "fx-$index",
                    type = type,
                    intensity = 0.75f,
                    radiusPx = 18f,
                    seed = 12345L + index
                )
            }
        )

        val json = Json.encodeToString(layer)
        val decoded = Json.decodeFromString<LayerData>(json)
        assertEquals(layer, decoded)
    }
}
