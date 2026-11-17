package com.graffitistudio.engine.drawing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.graffitistudio.core.domain.model.BrushConfig
import com.graffitistudio.core.domain.model.BrushType
import com.graffitistudio.core.domain.model.StrokeData
import com.graffitistudio.core.domain.model.StrokePoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Multi-engine brush and eraser rasterizer supporting Pencil, Pen, Marker,
 * Airbrush, Spray Paint, Texture Brush, and Eraser with deterministic seeded
 * particle generation for reproducible undo/redo and project reload.
 */
class BrushRasterizer {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val eraserXfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    private val reusablePath = Path()
    private val reusableOval = RectF()

    // Cached airbrush shader to avoid per-dab allocations
    private var cachedAirbrushSize = -1f
    private var cachedAirbrushColor = 0
    private var cachedAirbrushShader: RadialGradient? = null

    fun drawStroke(canvas: Canvas, stroke: StrokeData) {
        drawPoints(canvas, stroke.brush, stroke.points)
    }

    fun drawLiveDot(canvas: Canvas, brush: BrushConfig, point: StrokePoint) {
        when (brush.type) {
            BrushType.PEN, BrushType.ERASER -> {
                configureFillPaint(fillPaint, brush)
                canvas.drawCircle(point.x, point.y, brush.sizePx * 0.5f, fillPaint)
            }
            else -> stampDab(canvas, brush, point.x, point.y, angleDeg = 0f, dabIndex = 0)
        }
    }

    fun drawLiveSegment(
        canvas: Canvas,
        brush: BrushConfig,
        from: StrokePoint,
        control: StrokePoint,
        to: StrokePoint,
        startDabIndex: Int
    ): Int {
        return when (brush.type) {
            BrushType.PEN, BrushType.ERASER -> {
                configurePathPaint(strokePaint, brush)
                reusablePath.reset()
                reusablePath.moveTo(from.x, from.y)
                reusablePath.quadTo(control.x, control.y, to.x, to.y)
                canvas.drawPath(reusablePath, strokePaint)
                startDabIndex + 1
            }
            else -> {
                val dist = hypot(to.x - from.x, to.y - from.y)
                val stepPx = spacingFor(brush)
                val steps = max(1, (dist / stepPx).toInt())
                val angleDeg = Math.toDegrees(atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble())).toFloat()
                var idx = startDabIndex
                for (i in 1..steps) {
                    val t = i / steps.toFloat()
                    val omt = 1f - t
                    val x = omt * omt * from.x + 2f * omt * t * control.x + t * t * to.x
                    val y = omt * omt * from.y + 2f * omt * t * control.y + t * t * to.y
                    stampDab(canvas, brush, x, y, angleDeg, idx++)
                }
                idx
            }
        }
    }

    fun drawPoints(canvas: Canvas, brush: BrushConfig, points: List<StrokePoint>) {
        if (points.isEmpty()) return
        if (points.size == 1) {
            drawLiveDot(canvas, brush, points[0])
            return
        }

        if (brush.type == BrushType.PEN || brush.type == BrushType.ERASER) {
            configurePathPaint(strokePaint, brush)
            reusablePath.reset()
            reusablePath.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size - 1) {
                val current = points[i]
                val next = points[i + 1]
                val midX = (current.x + next.x) * 0.5f
                val midY = (current.y + next.y) * 0.5f
                reusablePath.quadTo(current.x, current.y, midX, midY)
            }
            val last = points.last()
            reusablePath.lineTo(last.x, last.y)
            canvas.drawPath(reusablePath, strokePaint)
            return
        }

        var dabIndex = 0
        drawLiveDot(canvas, brush, points[0])
        dabIndex++
        var lastMid = points[0]
        for (i in 1 until points.size - 1) {
            val current = points[i]
            val next = points[i + 1]
            val mid = StrokePoint((current.x + next.x) * 0.5f, (current.y + next.y) * 0.5f)
            dabIndex = drawLiveSegment(canvas, brush, lastMid, current, mid, dabIndex)
            lastMid = mid
        }
        val last = points.last()
        drawLiveSegment(canvas, brush, lastMid, last, last, dabIndex)
    }

    fun computeDirtyBounds(
        points: List<StrokePoint>,
        brush: BrushConfig,
        maxWidth: Int,
        maxHeight: Int
    ): RectF {
        if (points.isEmpty()) return RectF()
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }

        val spreadMultiplier = when (brush.type) {
            BrushType.SPRAY_PAINT -> 1.1f + brush.scatter
            BrushType.TEXTURE_BRUSH -> 0.9f + brush.scatter * 0.5f
            BrushType.AIRBRUSH -> 0.85f
            else -> 0.75f
        }
        val pad = brush.sizePx * spreadMultiplier + 6f
        return RectF(
            max(0f, minX - pad),
            max(0f, minY - pad),
            min(maxWidth.toFloat(), maxX + pad),
            min(maxHeight.toFloat(), maxY + pad)
        )
    }

    private fun stampDab(
        canvas: Canvas,
        brush: BrushConfig,
        x: Float,
        y: Float,
        angleDeg: Float,
        dabIndex: Int
    ) {
        when (brush.type) {
            BrushType.PENCIL -> drawPencilDab(canvas, brush, x, y, dabIndex)
            BrushType.MARKER -> drawMarkerDab(canvas, brush, x, y)
            BrushType.AIRBRUSH -> drawAirbrushDab(canvas, brush, x, y)
            BrushType.SPRAY_PAINT -> drawSprayDab(canvas, brush, x, y, dabIndex)
            BrushType.TEXTURE_BRUSH -> drawTextureDab(canvas, brush, x, y, angleDeg, dabIndex)
            BrushType.PEN, BrushType.ERASER -> {
                configureFillPaint(fillPaint, brush)
                canvas.drawCircle(x, y, brush.sizePx * 0.5f, fillPaint)
            }
        }
    }

    private fun drawPencilDab(canvas: Canvas, brush: BrushConfig, x: Float, y: Float, dabIndex: Int) {
        val rng = Random(brush.seed xor (dabIndex.toLong() * 0x9E3779B97F4A7C15uL.toLong()))
        val radius = max(1f, brush.sizePx * 0.5f)
        val grains = max(4, (brush.sizePx * 0.7f * brush.flow).toInt())
        fillPaint.xfermode = null
        fillPaint.shader = null
        fillPaint.color = brush.colorArgb

        for (i in 0 until grains) {
            val angle = rng.nextFloat() * (2f * Math.PI.toFloat())
            val dist = sqrt(rng.nextFloat()) * radius
            val gx = x + cos(angle) * dist
            val gy = y + sin(angle) * dist
            val grainAlpha = (brush.opacity * brush.flow * (0.35f + rng.nextFloat() * 0.55f) * 255f)
                .toInt()
                .coerceIn(1, 255)
            fillPaint.alpha = grainAlpha
            canvas.drawCircle(gx, gy, max(0.8f, radius * 0.28f), fillPaint)
        }
    }

    private fun drawMarkerDab(canvas: Canvas, brush: BrushConfig, x: Float, y: Float) {
        fillPaint.xfermode = null
        fillPaint.shader = null
        fillPaint.color = brush.colorArgb
        fillPaint.alpha = (brush.opacity * brush.flow * 0.42f * 255f).toInt().coerceIn(1, 255)

        val halfW = brush.sizePx * 0.55f
        val halfH = brush.sizePx * 0.24f
        val save = canvas.save()
        canvas.translate(x, y)
        canvas.rotate(-28f)
        reusableOval.set(-halfW, -halfH, halfW, halfH)
        canvas.drawRoundRect(reusableOval, halfH * 0.4f, halfH * 0.4f, fillPaint)
        canvas.restoreToCount(save)
    }

    private fun drawAirbrushDab(canvas: Canvas, brush: BrushConfig, x: Float, y: Float) {
        val radius = max(2f, brush.sizePx * 0.65f)
        val rgb = brush.colorArgb and 0x00FFFFFF
        val centerAlpha = (brush.opacity * brush.flow * 0.28f * 255f).toInt().coerceIn(1, 255)
        val centerColor = (centerAlpha shl 24) or rgb
        val edgeColor = rgb // 0 alpha at edge

        if (cachedAirbrushShader == null || cachedAirbrushSize != radius || cachedAirbrushColor != centerColor) {
            cachedAirbrushShader = RadialGradient(
                0f,
                0f,
                radius,
                intArrayOf(centerColor, (centerAlpha / 3 shl 24) or rgb, edgeColor),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            cachedAirbrushSize = radius
            cachedAirbrushColor = centerColor
        }

        fillPaint.xfermode = null
        fillPaint.alpha = 255
        fillPaint.shader = cachedAirbrushShader

        val save = canvas.save()
        canvas.translate(x, y)
        canvas.drawCircle(0f, 0f, radius, fillPaint)
        canvas.restoreToCount(save)
        fillPaint.shader = null
    }

    private fun drawSprayDab(canvas: Canvas, brush: BrushConfig, x: Float, y: Float, dabIndex: Int) {
        val rng = Random(brush.seed xor (dabIndex.toLong() * 0x517CC1B727220A95L))
        val maxRadius = max(4f, brush.sizePx * (0.55f + brush.scatter * 0.55f))
        val particles = max(10, (brush.sizePx * 0.95f * brush.flow).toInt())

        fillPaint.xfermode = null
        fillPaint.shader = null
        fillPaint.color = brush.colorArgb

        for (i in 0 until particles) {
            val angle = rng.nextFloat() * (2f * Math.PI.toFloat())
            // Biased toward center for authentic aerosol nozzle falloff
            val u = rng.nextFloat() * rng.nextFloat()
            val dist = u * maxRadius
            val px = x + cos(angle) * dist
            val py = y + sin(angle) * dist
            val falloff = 1f - (dist / maxRadius) * 0.7f
            val alpha = (brush.opacity * brush.flow * falloff * (0.3f + rng.nextFloat() * 0.7f) * 255f)
                .toInt()
                .coerceIn(1, 255)
            val dotRadius = max(0.8f, (1.2f + rng.nextFloat() * 2.2f) * (brush.sizePx / 48f).coerceIn(0.6f, 1.8f))
            fillPaint.alpha = alpha
            canvas.drawCircle(px, py, dotRadius, fillPaint)
        }
    }

    private fun drawTextureDab(
        canvas: Canvas,
        brush: BrushConfig,
        x: Float,
        y: Float,
        angleDeg: Float,
        dabIndex: Int
    ) {
        val rng = Random(brush.seed xor (dabIndex.toLong() * 0x94D049BB133111EBuL.toLong()))
        val radius = max(3f, brush.sizePx * 0.5f)
        val count = max(6, (brush.sizePx * 0.45f * brush.flow).toInt())

        fillPaint.xfermode = null
        fillPaint.shader = null
        fillPaint.color = brush.colorArgb

        val save = canvas.save()
        canvas.translate(x, y)
        canvas.rotate(angleDeg + rng.nextFloat() * 35f - 17.5f)
        for (i in 0 until count) {
            val ox = (rng.nextFloat() - 0.5f) * radius * 1.6f
            val oy = (rng.nextFloat() - 0.5f) * radius * 1.6f
            val w = max(1.5f, radius * (0.14f + rng.nextFloat() * 0.25f))
            val h = max(1.5f, radius * (0.08f + rng.nextFloat() * 0.2f))
            fillPaint.alpha = (brush.opacity * brush.flow * (0.25f + rng.nextFloat() * 0.65f) * 255f)
                .toInt()
                .coerceIn(1, 255)
            reusableOval.set(ox - w, oy - h, ox + w, oy + h)
            canvas.drawRect(reusableOval, fillPaint)
        }
        canvas.restoreToCount(save)
    }

    private fun spacingFor(brush: BrushConfig): Float = when (brush.type) {
        BrushType.PENCIL -> max(1.5f, brush.sizePx * 0.22f)
        BrushType.MARKER -> max(2f, brush.sizePx * 0.14f)
        BrushType.AIRBRUSH -> max(2.5f, brush.sizePx * 0.16f)
        BrushType.SPRAY_PAINT -> max(3f, brush.sizePx * 0.24f)
        BrushType.TEXTURE_BRUSH -> max(3f, brush.sizePx * 0.28f)
        BrushType.PEN, BrushType.ERASER -> max(2f, brush.sizePx * 0.2f)
    }

    private fun configurePathPaint(paint: Paint, brush: BrushConfig) {
        paint.shader = null
        paint.strokeWidth = brush.sizePx.coerceAtLeast(1f)
        if (brush.type == BrushType.ERASER) {
            paint.color = Color.TRANSPARENT
            paint.xfermode = eraserXfermode
            paint.alpha = 255
        } else {
            paint.xfermode = null
            paint.color = brush.colorArgb
            paint.alpha = (brush.opacity.coerceIn(0f, 1f) * 255f).toInt()
        }
    }

    private fun configureFillPaint(paint: Paint, brush: BrushConfig) {
        paint.shader = null
        if (brush.type == BrushType.ERASER) {
            paint.color = Color.TRANSPARENT
            paint.xfermode = eraserXfermode
            paint.alpha = 255
        } else {
            paint.xfermode = null
            paint.color = brush.colorArgb
            paint.alpha = (brush.opacity.coerceIn(0f, 1f) * 255f).toInt()
        }
    }
}
