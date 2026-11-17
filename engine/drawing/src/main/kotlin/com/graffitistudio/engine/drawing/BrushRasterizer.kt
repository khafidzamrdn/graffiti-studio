package com.graffitistudio.engine.drawing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.graffitistudio.core.domain.model.BrushConfig
import com.graffitistudio.core.domain.model.BrushType
import com.graffitistudio.core.domain.model.StrokeData
import com.graffitistudio.core.domain.model.StrokePoint
import kotlin.math.max
import kotlin.math.min

/**
 * Stateless brush and eraser rasterizer operating on Android [Canvas].
 */
class BrushRasterizer {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val eraserXfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    private val reusablePath = Path()

    fun drawStroke(canvas: Canvas, stroke: StrokeData) {
        drawPoints(canvas, stroke.brush, stroke.points)
    }

    fun drawSegment(
        canvas: Canvas,
        brush: BrushConfig,
        from: StrokePoint,
        control: StrokePoint,
        to: StrokePoint
    ) {
        configurePaint(strokePaint, brush)
        reusablePath.reset()
        reusablePath.moveTo(from.x, from.y)
        reusablePath.quadTo(control.x, control.y, to.x, to.y)
        canvas.drawPath(reusablePath, strokePaint)
    }

    fun drawPoints(canvas: Canvas, brush: BrushConfig, points: List<StrokePoint>) {
        if (points.isEmpty()) return

        if (points.size == 1) {
            configurePaint(dotPaint, brush)
            canvas.drawCircle(points[0].x, points[0].y, brush.sizePx / 2f, dotPaint)
            return
        }

        configurePaint(strokePaint, brush)
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
    }

    fun computeDirtyBounds(points: List<StrokePoint>, brushSizePx: Float, maxWidth: Int, maxHeight: Int): RectF {
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

        val pad = brushSizePx * 0.75f + 4f
        return RectF(
            max(0f, minX - pad),
            max(0f, minY - pad),
            min(maxWidth.toFloat(), maxX + pad),
            min(maxHeight.toFloat(), maxY + pad)
        )
    }

    private fun configurePaint(paint: Paint, brush: BrushConfig) {
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
}
