package com.graffitistudio.engine.drawing

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.graffitistudio.core.domain.model.EffectType
import com.graffitistudio.core.domain.model.LayerEffect
import com.graffitistudio.engine.rendering.BitmapPool
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Applies deterministic, layer-scoped effects (Spray Texture, Paint Splatter,
 * Glow, Blur, Noise, Halftone) onto a layer [Bitmap].
 */
class LayerEffectsRenderer(
    private val bitmapPool: BitmapPool
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun applyEffect(layerBitmap: Bitmap, layerCanvas: Canvas, effect: LayerEffect) {
        when (effect.type) {
            EffectType.SPRAY_TEXTURE -> applySprayTexture(layerBitmap.width, layerBitmap.height, layerCanvas, effect)
            EffectType.PAINT_SPLATTER -> applyPaintSplatter(layerBitmap.width, layerBitmap.height, layerCanvas, effect)
            EffectType.GLOW -> applyGlow(layerBitmap, layerCanvas, effect)
            EffectType.BLUR -> applyBlur(layerBitmap, layerCanvas, effect)
            EffectType.NOISE -> applyNoise(layerBitmap.width, layerBitmap.height, layerCanvas, effect)
            EffectType.HALFTONE -> applyHalftone(layerBitmap, layerCanvas, effect)
        }
    }

    private fun applySprayTexture(width: Int, height: Int, canvas: Canvas, effect: LayerEffect) {
        val rng = Random(effect.seed)
        val count = ((width * height) / 900f * effect.intensity).toInt().coerceIn(80, 4500)
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = effect.colorArgb

        for (i in 0 until count) {
            val x = rng.nextFloat() * width
            val y = rng.nextFloat() * height
            val r = 0.8f + rng.nextFloat() * max(1.5f, effect.radiusPx * 0.16f)
            paint.alpha = (effect.intensity * (0.15f + rng.nextFloat() * 0.55f) * 255f)
                .toInt()
                .coerceIn(1, 255)
            canvas.drawCircle(x, y, r, paint)
        }
    }

    private fun applyPaintSplatter(width: Int, height: Int, canvas: Canvas, effect: LayerEffect) {
        val rng = Random(effect.seed)
        val clusters = (4 + (effect.intensity * 14f).toInt()).coerceIn(3, 20)
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = effect.colorArgb

        for (c in 0 until clusters) {
            val cx = rng.nextFloat() * width
            val cy = rng.nextFloat() * height
            val coreRadius = max(4f, effect.radiusPx * (0.4f + rng.nextFloat() * 0.8f))
            paint.alpha = (effect.intensity * 220f).toInt().coerceIn(40, 255)
            canvas.drawCircle(cx, cy, coreRadius, paint)

            // Satellite droplets
            val droplets = 6 + rng.nextInt(10)
            for (d in 0 until droplets) {
                val angle = rng.nextFloat() * (2f * Math.PI.toFloat())
                val dist = coreRadius * (1.2f + rng.nextFloat() * 3.5f)
                val dx = cx + cos(angle) * dist
                val dy = cy + sin(angle) * dist
                val dr = max(1.2f, coreRadius * (0.12f + rng.nextFloat() * 0.28f))
                canvas.drawCircle(dx, dy, dr, paint)
            }

            // Occasional gravity drip
            if (rng.nextFloat() < 0.55f) {
                val dripLen = coreRadius * (2f + rng.nextFloat() * 5f)
                val dripWidth = max(2f, coreRadius * 0.35f)
                canvas.drawRect(cx - dripWidth * 0.5f, cy, cx + dripWidth * 0.5f, cy + dripLen, paint)
                canvas.drawCircle(cx, cy + dripLen, dripWidth * 0.65f, paint)
            }
        }
    }

    private fun applyGlow(layerBitmap: Bitmap, layerCanvas: Canvas, effect: LayerEffect) {
        val blurRadius = max(4f, effect.radiusPx)
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            colorFilter = PorterDuffColorFilter(effect.colorArgb, PorterDuff.Mode.SRC_IN)
            alpha = (effect.intensity * 255f).toInt().coerceIn(20, 255)
        }

        val alphaBitmap = layerBitmap.extractAlpha()
        val copy = bitmapPool.acquire(layerBitmap.width, layerBitmap.height)
        val copyCanvas = Canvas(copy)
        copyCanvas.drawBitmap(layerBitmap, 0f, 0f, null)

        // Draw glow behind original pixels
        layerCanvas.drawColor(0, PorterDuff.Mode.CLEAR)
        layerCanvas.drawBitmap(alphaBitmap, 0f, 0f, glowPaint)
        layerCanvas.drawBitmap(copy, 0f, 0f, null)

        alphaBitmap.recycle()
        bitmapPool.release(copy)
    }

    private fun applyBlur(layerBitmap: Bitmap, layerCanvas: Canvas, effect: LayerEffect) {
        val factor = (1f + effect.radiusPx * 0.25f * effect.intensity).coerceIn(1.5f, 14f)
        val smallW = max(1, (layerBitmap.width / factor).toInt())
        val smallH = max(1, (layerBitmap.height / factor).toInt())

        val downsampled = Bitmap.createScaledBitmap(layerBitmap, smallW, smallH, true)
        val srcRect = Rect(0, 0, smallW, smallH)
        val dstRect = Rect(0, 0, layerBitmap.width, layerBitmap.height)

        val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }
        layerCanvas.drawBitmap(downsampled, srcRect, dstRect, blurPaint)
        downsampled.recycle()
    }

    private fun applyNoise(width: Int, height: Int, canvas: Canvas, effect: LayerEffect) {
        val rng = Random(effect.seed)
        val step = 6
        paint.reset()
        paint.style = Paint.Style.FILL
        val maxAlpha = (effect.intensity * 90f).toInt().coerceIn(5, 110)

        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                if (rng.nextFloat() < 0.45f) {
                    val v = if (rng.nextBoolean()) 255 else 0
                    paint.color = Color.argb(rng.nextInt(1, maxAlpha + 1), v, v, v)
                    canvas.drawRect(
                        x.toFloat(),
                        y.toFloat(),
                        min(width, x + 3).toFloat(),
                        min(height, y + 3).toFloat(),
                        paint
                    )
                }
            }
        }
    }

    private fun applyHalftone(layerBitmap: Bitmap, layerCanvas: Canvas, effect: LayerEffect) {
        val cell = max(10, effect.radiusPx.toInt()).coerceIn(10, 48)
        val w = layerBitmap.width
        val h = layerBitmap.height

        val source = bitmapPool.acquire(w, h)
        Canvas(source).drawBitmap(layerBitmap, 0f, 0f, null)

        val rowPixels = IntArray(w)
        layerCanvas.drawColor(0, PorterDuff.Mode.CLEAR)
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL

        val half = cell * 0.5f
        for (y in 0 until h step cell) {
            val sampleY = min(h - 1, y + cell / 2)
            source.getPixels(rowPixels, 0, w, 0, sampleY, w, 1)
            for (x in 0 until w step cell) {
                val sampleX = min(w - 1, x + cell / 2)
                val px = rowPixels[sampleX]
                val a = Color.alpha(px)
                if (a > 12) {
                    val r = half * (a / 255f) * (0.65f + effect.intensity * 0.55f)
                    paint.color = px
                    layerCanvas.drawCircle(x + half, y + half, r, paint)
                }
            }
        }
        bitmapPool.release(source)
    }
}
