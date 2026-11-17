package com.graffitistudio.core.domain.model

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ExportFormat(val extension: String, val mimeType: String) {
    PNG("png", "image/png"),
    JPEG("jpg", "image/jpeg")
}

enum class ExportScale(val factor: Float, val label: String) {
    HALF(0.5f, "0.5x"),
    ORIGINAL(1f, "1x"),
    DOUBLE(2f, "2x")
}

data class ExportConfig(
    val format: ExportFormat = ExportFormat.PNG,
    val scale: ExportScale = ExportScale.ORIGINAL,
    val transparentBackground: Boolean = false,
    val jpegQuality: Int = 92
) {
    fun resolveDimensions(docWidth: Int, docHeight: Int, maxEdgePx: Int = 4096): Pair<Int, Int> {
        val rawW = max(1, (docWidth * scale.factor).roundToInt())
        val rawH = max(1, (docHeight * scale.factor).roundToInt())
        val largest = max(rawW, rawH)
        if (largest <= maxEdgePx) return rawW to rawH
        val clampScale = maxEdgePx / largest.toFloat()
        return max(1, (rawW * clampScale).roundToInt()) to max(1, (rawH * clampScale).roundToInt())
    }

    val effectiveTransparent: Boolean
        get() = format == ExportFormat.PNG && transparentBackground
}
