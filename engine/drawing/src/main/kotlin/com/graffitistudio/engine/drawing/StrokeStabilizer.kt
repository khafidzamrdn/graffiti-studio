package com.graffitistudio.engine.drawing

import com.graffitistudio.core.domain.model.StrokePoint

/**
 * Smooths raw touch coordinates using exponential moving averaging weighted by [level] (0f..1f).
 */
class StrokeStabilizer(
    private var level: Float = 0.35f
) {
    private var hasLast = false
    private var lastX = 0f
    private var lastY = 0f

    fun setLevel(level: Float) {
        this.level = level.coerceIn(0f, 0.9f)
    }

    fun reset() {
        hasLast = false
    }

    fun filter(point: StrokePoint): StrokePoint {
        if (!hasLast || level <= 0.01f) {
            lastX = point.x
            lastY = point.y
            hasLast = true
            return point
        }
        val alpha = 1f - level
        val smoothedX = lastX + alpha * (point.x - lastX)
        val smoothedY = lastY + alpha * (point.y - lastY)
        lastX = smoothedX
        lastY = smoothedY
        return point.copy(x = smoothedX, y = smoothedY)
    }
}
