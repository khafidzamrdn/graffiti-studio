package com.graffitistudio.engine.drawing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import com.graffitistudio.core.domain.model.GraffitiTextElement
import com.graffitistudio.core.domain.model.GraffitiTextStyle
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Renders graffiti typography elements onto an Android [Canvas] with support for
 * bubble lettering, outlines, drop shadows, 3D extrusion, gradient fills, and chrome reflection.
 */
class GraffitiTextRenderer {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
    }

    fun drawText(canvas: Canvas, element: GraffitiTextElement) {
        if (element.text.isBlank()) return

        val typeface = when (element.style) {
            GraffitiTextStyle.BUBBLE -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            GraffitiTextStyle.CHROME,
            GraffitiTextStyle.BLOCK_3D,
            GraffitiTextStyle.STANDARD -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
        }

        fillPaint.typeface = typeface
        strokePaint.typeface = typeface
        fillPaint.textSize = element.fontSizePx
        strokePaint.textSize = element.fontSizePx
        fillPaint.letterSpacing = element.letterSpacing
        strokePaint.letterSpacing = element.letterSpacing

        val effectiveOutline = when (element.style) {
            GraffitiTextStyle.BUBBLE -> max(element.outlineWidthPx, element.fontSizePx * 0.14f)
            else -> element.outlineWidthPx
        }

        val effectiveExtrusion = when {
            element.style == GraffitiTextStyle.BLOCK_3D -> max(12f, element.extrusionDepthPx)
            else -> element.extrusionDepthPx
        }

        // 1. Drop shadow
        if (element.shadowEnabled && element.shadowOffsetPx > 0f) {
            val shadowDist = element.shadowOffsetPx + effectiveExtrusion * 0.5f
            fillPaint.shader = null
            fillPaint.color = Color.argb(140, 0, 0, 0)
            canvas.drawText(element.text, element.x + shadowDist, element.y + shadowDist, fillPaint)
        }

        // 2. 3D stepped extrusion
        if (effectiveExtrusion > 0f) {
            val steps = effectiveExtrusion.roundToInt().coerceIn(1, 48)
            strokePaint.color = element.extrusionColorArgb
            strokePaint.strokeWidth = effectiveOutline
            fillPaint.shader = null
            fillPaint.color = element.extrusionColorArgb
            for (step in steps downTo 1) {
                val dx = step.toFloat()
                val dy = step.toFloat() * 0.85f
                if (effectiveOutline > 0f) {
                    canvas.drawText(element.text, element.x + dx, element.y + dy, strokePaint)
                }
                canvas.drawText(element.text, element.x + dx, element.y + dy, fillPaint)
            }
        }

        // 3. Bubble / outer outline
        if (effectiveOutline > 0f) {
            strokePaint.color = element.outlineColorArgb
            strokePaint.strokeWidth = effectiveOutline
            canvas.drawText(element.text, element.x, element.y, strokePaint)
        }

        // 4. Fill (Chrome, Gradient, or Solid)
        val topY = element.y - element.fontSizePx * 0.85f
        val bottomY = element.y + element.fontSizePx * 0.2f

        fillPaint.shader = when {
            element.style == GraffitiTextStyle.CHROME -> LinearGradient(
                element.x,
                topY,
                element.x,
                bottomY,
                intArrayOf(
                    0xFFEAF6FF.toInt(),
                    0xFF6BCBFF.toInt(),
                    0xFFFFFFFF.toInt(),
                    0xFF1F2633.toInt(),
                    0xFFFFB703.toInt(),
                    0xFFF2F4F8.toInt()
                ),
                floatArrayOf(0f, 0.28f, 0.48f, 0.52f, 0.78f, 1f),
                Shader.TileMode.CLAMP
            )

            element.useGradientFill -> LinearGradient(
                element.x,
                topY,
                element.x,
                bottomY,
                element.primaryColorArgb,
                element.secondaryColorArgb,
                Shader.TileMode.CLAMP
            )

            else -> null
        }

        if (fillPaint.shader == null) {
            fillPaint.color = element.primaryColorArgb
        }
        canvas.drawText(element.text, element.x, element.y, fillPaint)
        fillPaint.shader = null

        // Subtle inner specular highlight for Bubble & Chrome styles
        if (element.style == GraffitiTextStyle.BUBBLE || element.style == GraffitiTextStyle.CHROME) {
            strokePaint.strokeWidth = max(1.5f, element.fontSizePx * 0.02f)
            strokePaint.color = Color.argb(110, 255, 255, 255)
            canvas.drawText(element.text, element.x - 1.5f, element.y - 2f, strokePaint)
        }
    }
}
