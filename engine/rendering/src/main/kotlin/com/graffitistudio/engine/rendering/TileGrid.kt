package com.graffitistudio.engine.rendering

import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Tile-based dirty-region tracker.
 *
 * Divides the document into fixed-size tiles so compositing and cache invalidation
 * only touch regions modified by the active stroke or undo/redo command.
 */
class TileGrid(
    val documentWidth: Int,
    val documentHeight: Int,
    val tileSize: Int = DEFAULT_TILE_SIZE
) {
    val columns: Int = max(1, ceil(documentWidth / tileSize.toFloat()).toInt())
    val rows: Int = max(1, ceil(documentHeight / tileSize.toFloat()).toInt())

    private val dirtyTiles = BooleanArray(columns * rows)

    fun markAllDirty() {
        dirtyTiles.fill(true)
    }

    fun clearAllDirty() {
        dirtyTiles.fill(false)
    }

    fun markDirty(rect: RectF) {
        if (rect.isEmpty) return
        val startCol = max(0, (rect.left / tileSize).toInt())
        val endCol = min(columns - 1, (rect.right / tileSize).toInt())
        val startRow = max(0, (rect.top / tileSize).toInt())
        val endRow = min(rows - 1, (rect.bottom / tileSize).toInt())

        for (row in startRow..endRow) {
            val rowOffset = row * columns
            for (col in startCol..endCol) {
                dirtyTiles[rowOffset + col] = true
            }
        }
    }

    fun consumeDirtyBounds(): Rect? {
        var minCol = columns
        var maxCol = -1
        var minRow = rows
        var maxRow = -1

        for (row in 0 until rows) {
            val rowOffset = row * columns
            for (col in 0 until columns) {
                if (dirtyTiles[rowOffset + col]) {
                    if (col < minCol) minCol = col
                    if (col > maxCol) maxCol = col
                    if (row < minRow) minRow = row
                    if (row > maxRow) maxRow = row
                    dirtyTiles[rowOffset + col] = false
                }
            }
        }

        if (maxCol < minCol || maxRow < minRow) return null
        return Rect(
            minCol * tileSize,
            minRow * tileSize,
            min(documentWidth, (maxCol + 1) * tileSize),
            min(documentHeight, (maxRow + 1) * tileSize)
        )
    }

    companion object {
        const val DEFAULT_TILE_SIZE = 256
    }
}
