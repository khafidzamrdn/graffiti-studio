package com.graffitistudio.engine.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.graffitistudio.core.domain.history.HistoryCommand
import com.graffitistudio.core.domain.model.StrokeData

/**
 * Command-based history entry for a single brush or eraser stroke.
 *
 * Stores a cropped [beforeRegionBitmap] covering only the stroke's dirty bounding box
 * so undo restores pixels in O(stroke area) without keeping full-canvas snapshots.
 */
class DrawStrokeCommand(
    private val stroke: StrokeData,
    private val dirtyRect: Rect,
    private val beforeRegionBitmap: Bitmap?,
    private val onApplyStroke: (StrokeData) -> Unit,
    private val onRestoreRegion: (layerId: String, strokeId: String, dirtyRect: Rect, backup: Bitmap?) -> Unit,
    private val onReleaseBackup: (Bitmap?) -> Unit = { it?.recycle() }
) : HistoryCommand {

    override val description: String = "${stroke.brush.type.displayName} Stroke"

    override fun execute() {
        onApplyStroke(stroke)
    }

    override fun undo() {
        onRestoreRegion(stroke.layerId, stroke.id, dirtyRect, beforeRegionBitmap)
    }

    /**
     * The cropped pre-stroke backup can no longer be reached once this command is
     * dropped from history, so hand it back to the bitmap pool instead of waiting
     * for GC.
     */
    override fun dispose() {
        onReleaseBackup(beforeRegionBitmap)
    }
}

internal object RegionRestoreHelper {
    private val srcPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }

    fun copyRegion(source: Bitmap, rect: Rect, target: Bitmap) {
        if (rect.isEmpty) return
        val canvas = Canvas(target)
        val dst = Rect(0, 0, rect.width(), rect.height())
        canvas.drawBitmap(source, rect, dst, srcPaint)
    }

    fun restoreRegion(target: Bitmap, rect: Rect, backup: Bitmap) {
        if (rect.isEmpty || backup.isRecycled) return
        val canvas = Canvas(target)
        val src = Rect(0, 0, rect.width(), rect.height())
        canvas.drawBitmap(backup, src, rect, srcPaint)
    }
}
