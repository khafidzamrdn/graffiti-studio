package com.graffitistudio.engine.rendering

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import java.util.concurrent.ConcurrentHashMap

/**
 * Reusable ARGB_8888 / ALPHA_8 bitmap pool to prevent allocation churn during stroke
 * rasterization, tile caching, and dirty-region undo backups.
 */
class BitmapPool(
    private val maxBytes: Long = 32L * 1024L * 1024L
) {
    private data class Key(val width: Int, val height: Int, val config: Bitmap.Config)

    private val lock = Any()
    private val pools = ConcurrentHashMap<Key, ArrayDeque<Bitmap>>()
    private var currentBytes: Long = 0L

    fun acquire(
        width: Int,
        height: Int,
        config: Bitmap.Config = Bitmap.Config.ARGB_8888,
        clearBitmap: Boolean = true
    ): Bitmap {
        require(width > 0 && height > 0) { "Bitmap dimensions must be positive" }
        val key = Key(width, height, config)
        val recycled = synchronized(lock) {
            val deque = pools[key]
            val bmp = deque?.removeLastOrNull()
            if (bmp != null) {
                currentBytes -= bmp.allocationByteCount.toLong()
            }
            bmp
        }

        if (recycled != null && !recycled.isRecycled) {
            if (clearBitmap) {
                recycled.eraseColor(0)
            }
            return recycled
        }
        return Bitmap.createBitmap(width, height, config)
    }

    fun release(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled || !bitmap.isMutable) return
        val bytes = bitmap.allocationByteCount.toLong()
        if (bytes > maxBytes / 2) {
            bitmap.recycle()
            return
        }

        val key = Key(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        synchronized(lock) {
            if (currentBytes + bytes > maxBytes) {
                evictOldestLocked(targetBytes = maxBytes - bytes)
            }
            if (currentBytes + bytes <= maxBytes) {
                pools.getOrPut(key) { ArrayDeque() }.addLast(bitmap)
                currentBytes += bytes
            } else {
                bitmap.recycle()
            }
        }
    }

    fun trimMemory(level: Int) {
        synchronized(lock) {
            when {
                level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> clearLocked()
                level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE -> evictOldestLocked(maxBytes / 4)
                else -> evictOldestLocked(maxBytes / 2)
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            clearLocked()
        }
    }

    private fun clearLocked() {
        pools.values.forEach { deque ->
            deque.forEach { if (!it.isRecycled) it.recycle() }
            deque.clear()
        }
        pools.clear()
        currentBytes = 0L
    }

    private fun evictOldestLocked(targetBytes: Long) {
        val iterator = pools.entries.iterator()
        while (currentBytes > targetBytes && iterator.hasNext()) {
            val entry = iterator.next()
            val deque = entry.value
            while (currentBytes > targetBytes && deque.isNotEmpty()) {
                val bmp = deque.removeFirst()
                currentBytes -= bmp.allocationByteCount.toLong()
                if (!bmp.isRecycled) bmp.recycle()
            }
            if (deque.isEmpty()) {
                iterator.remove()
            }
        }
    }
}
