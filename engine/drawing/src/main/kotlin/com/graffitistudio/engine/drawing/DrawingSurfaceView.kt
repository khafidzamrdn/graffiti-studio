package com.graffitistudio.engine.drawing

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.min

/**
 * Custom Android View providing low-latency touch sampling, pinch-zoom & pan,
 * and hardware-accelerated display of [DrawingEngine]'s composited surface.
 */
class DrawingSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var engine: DrawingEngine? = null

    private val viewMatrix = Matrix()
    private val inverseMatrix = Matrix()
    private val mappedPoint = FloatArray(2)
    private var hasInitializedViewport = false

    private var isDrawingStroke = false
    private var isTransforming = false
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var lastPanX = 0f
    private var lastPanY = 0f

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#2C313D")
    }

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                val currentScale = getCurrentScale()
                val targetScale = (currentScale * scaleFactor).coerceIn(0.25f, 8f)
                val appliedFactor = targetScale / currentScale
                viewMatrix.postScale(appliedFactor, appliedFactor, detector.focusX, detector.focusY)
                invalidate()
                return true
            }
        }
    )

    fun attachEngine(drawingEngine: DrawingEngine) {
        engine = drawingEngine
        drawingEngine.onFrameInvalidated = { postInvalidateOnAnimation() }
        hasInitializedViewport = false
        resetViewport()
        invalidate()
    }

    fun resetViewport() {
        val docSize = engine?.getDocumentSize() ?: return
        if (width <= 0 || height <= 0) return

        val (docW, docH) = docSize
        val padding = 48f
        val availW = (width - padding * 2).coerceAtLeast(1f)
        val availH = (height - padding * 2).coerceAtLeast(1f)
        val fitScale = min(availW / docW.toFloat(), availH / docH.toFloat())

        val dx = (width - docW * fitScale) * 0.5f
        val dy = (height - docH * fitScale) * 0.5f

        viewMatrix.reset()
        viewMatrix.postScale(fitScale, fitScale)
        viewMatrix.postTranslate(dx, dy)
        hasInitializedViewport = true
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!hasInitializedViewport) {
            resetViewport()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#0D0E12"))

        val drawingEngine = engine ?: return
        val docSize = drawingEngine.getDocumentSize() ?: return
        if (!hasInitializedViewport) {
            resetViewport()
        }

        val saveCount = canvas.save()
        canvas.concat(viewMatrix)
        drawingEngine.drawCompositeTo(canvas)
        canvas.drawRect(0f, 0f, docSize.first.toFloat(), docSize.second.toFloat(), borderPaint)
        canvas.restoreToCount(saveCount)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val drawingEngine = engine ?: return false
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                isTransforming = false
                isDrawingStroke = true
                mapToDocument(event.x, event.y)
                drawingEngine.beginStroke(
                    x = mappedPoint[0],
                    y = mappedPoint[1],
                    pressure = event.pressure,
                    timestampMs = event.eventTime
                )
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (isDrawingStroke) {
                    drawingEngine.cancelActiveStroke()
                    isDrawingStroke = false
                }
                isTransforming = true
                lastPanX = averageX(event)
                lastPanY = averageY(event)
            }

            MotionEvent.ACTION_MOVE -> {
                if (isTransforming || event.pointerCount > 1) {
                    val avgX = averageX(event)
                    val avgY = averageY(event)
                    if (!scaleDetector.isInProgress) {
                        viewMatrix.postTranslate(avgX - lastPanX, avgY - lastPanY)
                        invalidate()
                    }
                    lastPanX = avgX
                    lastPanY = avgY
                } else if (isDrawingStroke) {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex >= 0) {
                        // Process historical batched samples for smooth curves
                        for (h in 0 until event.historySize) {
                            mapToDocument(
                                event.getHistoricalX(pointerIndex, h),
                                event.getHistoricalY(pointerIndex, h)
                            )
                            drawingEngine.moveStroke(
                                x = mappedPoint[0],
                                y = mappedPoint[1],
                                pressure = event.getHistoricalPressure(pointerIndex, h),
                                timestampMs = event.getHistoricalEventTime(h)
                            )
                        }
                        mapToDocument(event.getX(pointerIndex), event.getY(pointerIndex))
                        drawingEngine.moveStroke(
                            x = mappedPoint[0],
                            y = mappedPoint[1],
                            pressure = event.getPressure(pointerIndex),
                            timestampMs = event.eventTime
                        )
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 2) {
                    lastPanX = averageX(event)
                    lastPanY = averageY(event)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDrawingStroke) {
                    drawingEngine.endStroke()
                }
                isDrawingStroke = false
                isTransforming = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
        }
        return true
    }

    private fun mapToDocument(screenX: Float, screenY: Float) {
        viewMatrix.invert(inverseMatrix)
        mappedPoint[0] = screenX
        mappedPoint[1] = screenY
        inverseMatrix.mapPoints(mappedPoint)
    }

    private fun getCurrentScale(): Float {
        val values = FloatArray(9)
        viewMatrix.getValues(values)
        return values[Matrix.MSCALE_X].coerceAtLeast(0.01f)
    }

    private fun averageX(event: MotionEvent): Float {
        var sum = 0f
        for (i in 0 until event.pointerCount) sum += event.getX(i)
        return sum / event.pointerCount.coerceAtLeast(1)
    }

    private fun averageY(event: MotionEvent): Float {
        var sum = 0f
        for (i in 0 until event.pointerCount) sum += event.getY(i)
        return sum / event.pointerCount.coerceAtLeast(1)
    }
}
