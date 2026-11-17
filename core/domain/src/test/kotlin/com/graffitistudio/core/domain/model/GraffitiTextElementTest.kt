package com.graffitistudio.core.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class GraffitiTextElementTest {

    @Test
    fun `text element serializes inside LayerData`() {
        val layer = LayerData(
            id = "layer-1",
            name = "Text Layer",
            textElements = listOf(
                GraffitiTextElement(
                    id = "txt-1",
                    layerId = "layer-1",
                    text = "URBAN",
                    x = 540f,
                    y = 540f,
                    style = GraffitiTextStyle.CHROME
                )
            )
        )

        val encoded = Json.encodeToString(layer)
        val decoded = Json.decodeFromString<LayerData>(encoded)
        assertEquals(layer, decoded)
    }
}
