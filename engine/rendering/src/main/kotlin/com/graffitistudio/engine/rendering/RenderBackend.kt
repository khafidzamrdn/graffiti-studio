package com.graffitistudio.engine.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.graffitistudio.core.domain.model.BlendMode

/**
 * Abstract rendering backend designed so software Canvas compositing can later be
 * swapped or augmented with an OpenGL ES 3.0 / FBO tile renderer without touching
 * the drawing engine or UI layers.
 */
interface RenderBackend {
    val bitmapPool: BitmapPool

    fun allocateSurface(width: Int, height: Int): Bitmap

    fun releaseSurface(bitmap: Bitmap?)

    fun composite(
        target: Canvas,
        backgroundColorArgb: Int,
        layers: List<RenderLayer>,
        dirtyRect: Rect? = null,
        backgroundBitmap: Bitmap? = null
    )

    fun trimMemory(level: Int)
}

data class RenderLayer(
    val id: String,
    val bitmap: Bitmap,
    val isVisible: Boolean,
    val opacity: Float,
    val blendMode: BlendMode
)

class SoftwareCanvasRenderBackend(
    override val bitmapPool: BitmapPool = BitmapPool()
) : RenderBackend {

    private val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    override fun allocateSurface(width: Int, height: Int): Bitmap {
        return bitmapPool.acquire(width, height, Bitmap.Config.ARGB_8888, clearBitmap = true)
    }

    override fun releaseSurface(bitmap: Bitmap?) {
        bitmapPool.release(bitmap)
    }

    override fun composite(
        target: Canvas,
        backgroundColorArgb: Int,
        layers: List<RenderLayer>,
        dirtyRect: Rect?,
        backgroundBitmap: Bitmap?
    ) {
        val saveCount = target.save()
        if (dirtyRect != null) {
            target.clipRect(dirtyRect)
        }
        if (backgroundBitmap != null && !backgroundBitmap.isRecycled) {
            target.drawBitmap(backgroundBitmap, 0f, 0f, null)
        } else {
            target.drawColor(backgroundColorArgb)
        }

        for (layer in layers) {
            if (!layer.isVisible || layer.opacity <= 0f || layer.bitmap.isRecycled) continue
            compositePaint.alpha = (layer.opacity.coerceIn(0f, 1f) * 255f).toInt()
            compositePaint.xfermode = layer.blendMode.toPorterDuffXfermode()
            target.drawBitmap(layer.bitmap, 0f, 0f, compositePaint)
        }
        compositePaint.xfermode = null
        compositePaint.alpha = 255
        target.restoreToCount(saveCount)
    }

    override fun trimMemory(level: Int) {
        bitmapPool.trimMemory(level)
    }

    private fun BlendMode.toPorterDuffXfermode(): PorterDuffXfermode? = when (this) {
        BlendMode.NORMAL -> null
        BlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        BlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        BlendMode.OVERLAY -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
        BlendMode.DARKEN -> PorterDuffXfermode(PorterDuff.Mode.DARKEN)
        BlendMode.LIGHTEN -> PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
    }
}
