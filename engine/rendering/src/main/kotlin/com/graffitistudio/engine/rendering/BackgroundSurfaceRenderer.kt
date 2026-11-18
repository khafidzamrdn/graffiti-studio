package com.graffitistudio.engine.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.graffitistudio.core.domain.model.BackgroundSurfaceType
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Procedurally renders built-in urban background surfaces (brick wall, raw concrete,
 * metal shutter, subway tile, blackbook paper, and solid color) into a cached [Bitmap].
 */
object BackgroundSurfaceRenderer {

    fun renderInto(
        target: Bitmap,
        surfaceType: BackgroundSurfaceType,
        baseColorArgb: Int
    ) {
        val canvas = Canvas(target)
        val w = target.width
        val h = target.height

        canvas.drawColor(baseColorArgb)
        when (surfaceType) {
            BackgroundSurfaceType.SOLID -> Unit
            BackgroundSurfaceType.BRICK_WALL -> drawBrickWall(canvas, w, h, baseColorArgb)
            BackgroundSurfaceType.CONCRETE_WALL -> drawConcreteWall(canvas, w, h, baseColorArgb)
            BackgroundSurfaceType.METAL_SHUTTER -> drawMetalShutter(canvas, w, h, baseColorArgb)
            BackgroundSurfaceType.SUBWAY_TILE -> drawSubwayTile(canvas, w, h, baseColorArgb)
            BackgroundSurfaceType.BLACKBOOK_PAPER -> drawBlackbookPaper(canvas, w, h, baseColorArgb)
        }
    }

    private fun drawBrickWall(canvas: Canvas, width: Int, height: Int, baseColor: Int) {
        val rng = Random(1009L)
        val brickW = max(96f, width / 9f)
        val brickH = brickW * 0.44f
        val mortar = max(4f, brickH * 0.12f)

        val mortarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shiftBrightness(baseColor, 0.55f)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), mortarPaint)

        val brickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        var row = 0
        var y = 0f
        while (y < height) {
            val offsetX = if (row % 2 == 1) -brickW * 0.5f else 0f
            var x = offsetX
            while (x < width) {
                val factor = 0.85f + rng.nextFloat() * 0.3f
                brickPaint.color = shiftBrightness(baseColor, factor)
                canvas.drawRect(
                    x + mortar * 0.5f,
                    y + mortar * 0.5f,
                    x + brickW - mortar * 0.5f,
                    y + brickH - mortar * 0.5f,
                    brickPaint
                )
                x += brickW
            }
            y += brickH
            row++
        }

        drawSurfaceGrit(canvas, width, height, rng, count = 1800, maxAlpha = 26)
    }

    private fun drawConcreteWall(canvas: Canvas, width: Int, height: Int, baseColor: Int) {
        val rng = Random(2029L)
        val panelW = max(240f, width / 3f)
        val panelH = max(240f, height / 3f)

        val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shiftBrightness(baseColor, 0.72f)
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shiftBrightness(baseColor, 0.5f)
            style = Paint.Style.FILL
        }

        var y = 0f
        while (y < height) {
            var x = 0f
            while (x < width) {
                canvas.drawRect(x, y, x + panelW, y + panelH, seamPaint)
                val pad = 28f
                canvas.drawCircle(x + pad, y + pad, 5f, holePaint)
                canvas.drawCircle(min(width.toFloat(), x + panelW - pad), y + pad, 5f, holePaint)
                x += panelW
            }
            y += panelH
        }

        drawSurfaceGrit(canvas, width, height, rng, count = 2600, maxAlpha = 32)
    }

    private fun drawMetalShutter(canvas: Canvas, width: Int, height: Int, baseColor: Int) {
        val slatH = max(28f, height / 28f)
        val slatPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val highlight = shiftBrightness(baseColor, 1.28f)
        val shadow = shiftBrightness(baseColor, 0.58f)

        var y = 0f
        while (y < height) {
            slatPaint.shader = LinearGradient(
                0f,
                y,
                0f,
                y + slatH,
                intArrayOf(highlight, baseColor, shadow),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, y, width.toFloat(), y + slatH, slatPaint)
            y += slatH
        }
        slatPaint.shader = null
        drawSurfaceGrit(canvas, width, height, Random(3037L), count = 900, maxAlpha = 18)
    }

    private fun drawSubwayTile(canvas: Canvas, width: Int, height: Int, baseColor: Int) {
        val rng = Random(4049L)
        val tileW = max(84f, width / 10f)
        val tileH = tileW * 0.5f
        val grout = max(3f, tileH * 0.08f)

        val groutPaint = Paint().apply {
            color = shiftBrightness(baseColor, 0.65f)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), groutPaint)

        val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        var row = 0
        var y = 0f
        while (y < height) {
            val offsetX = if (row % 2 == 1) -tileW * 0.5f else 0f
            var x = offsetX
            while (x < width) {
                tilePaint.color = shiftBrightness(baseColor, 0.94f + rng.nextFloat() * 0.1f)
                canvas.drawRect(
                    x + grout * 0.5f,
                    y + grout * 0.5f,
                    x + tileW - grout * 0.5f,
                    y + tileH - grout * 0.5f,
                    tilePaint
                )
                x += tileW
            }
            y += tileH
            row++
        }
    }

    private fun drawBlackbookPaper(canvas: Canvas, width: Int, height: Int, baseColor: Int) {
        val rng = Random(5051L)
        drawSurfaceGrit(canvas, width, height, rng, count = 3200, maxAlpha = 24)
    }

    private fun drawSurfaceGrit(
        canvas: Canvas,
        width: Int,
        height: Int,
        rng: Random,
        count: Int,
        maxAlpha: Int
    ) {
        val gritPaint = Paint().apply { style = Paint.Style.FILL }
        for (i in 0 until count) {
            val x = rng.nextFloat() * width
            val y = rng.nextFloat() * height
            val size = 1f + rng.nextFloat() * 2.5f
            val v = if (rng.nextBoolean()) 255 else 0
            gritPaint.color = Color.argb(rng.nextInt(4, maxAlpha + 1), v, v, v)
            canvas.drawRect(x, y, x + size, y + size, gritPaint)
        }
    }

    private fun shiftBrightness(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(255, r, g, b)
    }
}
