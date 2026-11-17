package com.graffitistudio.engine.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.graffitistudio.core.domain.history.HistoryManager
import com.graffitistudio.core.domain.history.HistoryState
import com.graffitistudio.core.domain.model.ArtworkDocument
import com.graffitistudio.core.domain.model.BlendMode
import com.graffitistudio.core.domain.model.BrushConfig
import com.graffitistudio.core.domain.model.ExportConfig
import com.graffitistudio.core.domain.model.LayerData
import com.graffitistudio.core.domain.model.StrokeData
import com.graffitistudio.core.domain.model.StrokePoint
import com.graffitistudio.engine.rendering.RenderBackend
import com.graffitistudio.engine.rendering.RenderLayer
import com.graffitistudio.engine.rendering.SoftwareCanvasRenderBackend
import com.graffitistudio.engine.rendering.TileGrid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Core drawing engine independent of Jetpack Compose.
 *
 * Owns document layer surfaces, tile-based dirty tracking, brush rasterization,
 * layer management, and the command-based [HistoryManager].
 */
class DrawingEngine(
    private val renderBackend: RenderBackend = SoftwareCanvasRenderBackend(),
    private val historyManager: HistoryManager = HistoryManager(maxHistorySize = 60)
) {
    val historyState: StateFlow<HistoryState> = historyManager.state

    private val _layersState = MutableStateFlow<List<LayerData>>(emptyList())
    val layersState: StateFlow<List<LayerData>> = _layersState.asStateFlow()

    private val _activeLayerIdState = MutableStateFlow<String?>(null)
    val activeLayerIdState: StateFlow<String?> = _activeLayerIdState.asStateFlow()

    private val lock = Any()
    private val rasterizer = BrushRasterizer()
    private val stabilizer = StrokeStabilizer()
    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    private var document: ArtworkDocument? = null
    private var tileGrid: TileGrid? = null
    private var compositeBitmap: Bitmap? = null
    private var compositeCanvas: Canvas? = null

    private val layerSurfaces = LinkedHashMap<String, LayerSurface>()
    private var activeLayerId: String? = null
    private var activeBrush: BrushConfig = BrushConfig()

    // Active live stroke state
    private var currentStrokeBrush: BrushConfig = BrushConfig()
    private var currentStrokePoints = mutableListOf<StrokePoint>()
    private var preStrokeBackup: Bitmap? = null
    private var preStrokeLayerBitmap: Bitmap? = null
    private var activeDirtyBounds = RectF()
    private var lastMidX = 0f
    private var lastMidY = 0f
    private var liveDabIndex = 0

    var onFrameInvalidated: (() -> Unit)? = null

    private class LayerSurface(
        val id: String,
        val bitmap: Bitmap,
        val canvas: Canvas
    )

    fun loadDocument(doc: ArtworkDocument, onLoaded: (() -> Unit)? = null) {
        backgroundExecutor.execute {
            synchronized(lock) {
                releaseSurfacesLocked()
                historyManager.clear()

                val normalizedDoc = if (doc.layers.isEmpty()) {
                    doc.copy(
                        layers = listOf(
                            LayerData(
                                id = UUID.randomUUID().toString(),
                                name = "Layer 1"
                            )
                        )
                    )
                } else {
                    doc
                }

                document = normalizedDoc
                tileGrid = TileGrid(normalizedDoc.width, normalizedDoc.height)
                compositeBitmap = renderBackend.allocateSurface(normalizedDoc.width, normalizedDoc.height)
                compositeCanvas = Canvas(compositeBitmap!!)

                for (layer in normalizedDoc.layers) {
                    allocateAndRasterizeLayerLocked(normalizedDoc.width, normalizedDoc.height, layer)
                }

                activeLayerId = normalizedDoc.layers.last().id
                syncLayerFlowsLocked()
                tileGrid?.markAllDirty()
                recompositeDirtyTilesLocked()
            }
            onFrameInvalidated?.invoke()
            onLoaded?.invoke()
        }
    }

    fun updateBrush(brush: BrushConfig) {
        synchronized(lock) {
            activeBrush = brush
            stabilizer.setLevel(brush.stabilizerLevel)
        }
    }

    fun getBrush(): BrushConfig = synchronized(lock) { activeBrush }

    fun getDocumentSize(): Pair<Int, Int>? = synchronized(lock) {
        document?.let { it.width to it.height }
    }

    // --- Layer Engine Operations ---

    fun selectLayer(layerId: String) {
        synchronized(lock) {
            val doc = document ?: return
            if (doc.layers.any { it.id == layerId }) {
                activeLayerId = layerId
                _activeLayerIdState.value = layerId
            }
        }
    }

    fun addLayer(name: String? = null) {
        synchronized(lock) {
            val doc = document ?: return
            val beforeLayers = doc.layers
            val beforeActive = activeLayerId
            val newLayer = LayerData(
                id = UUID.randomUUID().toString(),
                name = name?.trim()?.ifEmpty { null } ?: "Layer ${beforeLayers.size + 1}"
            )
            val insertIndex = (beforeLayers.indexOfFirst { it.id == beforeActive }
                .takeIf { it >= 0 } ?: (beforeLayers.size - 1)) + 1
            val afterLayers = beforeLayers.toMutableList().apply { add(insertIndex, newLayer) }

            val command = LayerStructureCommand(
                description = "Add Layer",
                beforeLayers = beforeLayers,
                afterLayers = afterLayers,
                beforeActiveLayerId = beforeActive,
                afterActiveLayerId = newLayer.id,
                onApplyLayers = ::applyLayerStructureChange
            )
            historyManager.execute(command)
        }
    }

    fun deleteLayer(layerId: String) {
        synchronized(lock) {
            val doc = document ?: return
            if (doc.layers.size <= 1) return
            val index = doc.layers.indexOfFirst { it.id == layerId }
            if (index < 0) return

            val beforeLayers = doc.layers
            val beforeActive = activeLayerId
            val afterLayers = beforeLayers.filterNot { it.id == layerId }
            val afterActive = if (beforeActive == layerId) {
                afterLayers[min(index, afterLayers.lastIndex)].id
            } else {
                beforeActive
            }

            val command = LayerStructureCommand(
                description = "Delete Layer",
                beforeLayers = beforeLayers,
                afterLayers = afterLayers,
                beforeActiveLayerId = beforeActive,
                afterActiveLayerId = afterActive,
                onApplyLayers = ::applyLayerStructureChange
            )
            historyManager.execute(command)
        }
    }

    fun renameLayer(layerId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        updateLayerMetadata(layerId, "Rename Layer") { it.copy(name = trimmed) }
    }

    fun setLayerVisibility(layerId: String, isVisible: Boolean) {
        updateLayerMetadata(layerId, "Toggle Layer Visibility") { it.copy(isVisible = isVisible) }
    }

    fun setLayerOpacity(layerId: String, opacity: Float) {
        val clamped = opacity.coerceIn(0f, 1f)
        updateLayerMetadata(layerId, "Layer Opacity") { it.copy(opacity = clamped) }
    }

    fun setLayerBlendMode(layerId: String, blendMode: BlendMode) {
        updateLayerMetadata(layerId, "Layer Blend Mode") { it.copy(blendMode = blendMode) }
    }

    fun moveLayer(layerId: String, delta: Int) {
        synchronized(lock) {
            val doc = document ?: return
            val index = doc.layers.indexOfFirst { it.id == layerId }
            if (index < 0) return
            val targetIndex = (index + delta).coerceIn(0, doc.layers.lastIndex)
            if (targetIndex == index) return

            val beforeLayers = doc.layers
            val afterLayers = beforeLayers.toMutableList().apply {
                val item = removeAt(index)
                add(targetIndex, item)
            }

            val command = LayerStructureCommand(
                description = "Reorder Layer",
                beforeLayers = beforeLayers,
                afterLayers = afterLayers,
                beforeActiveLayerId = activeLayerId,
                afterActiveLayerId = activeLayerId,
                onApplyLayers = ::applyLayerStructureChange
            )
            historyManager.execute(command)
        }
    }

    fun duplicateLayer(layerId: String) {
        synchronized(lock) {
            val doc = document ?: return
            val index = doc.layers.indexOfFirst { it.id == layerId }
            if (index < 0) return

            val source = doc.layers[index]
            val newId = UUID.randomUUID().toString()
            val duplicated = source.copy(
                id = newId,
                name = "${source.name} Copy",
                strokes = source.strokes.map { it.copy(id = UUID.randomUUID().toString(), layerId = newId) }
            )

            // Fast bitmap clone from existing cached surface so strokes don't need re-rasterization
            val srcSurface = layerSurfaces[source.id]
            if (srcSurface != null) {
                val cloneBmp = renderBackend.allocateSurface(doc.width, doc.height)
                val cloneCanvas = Canvas(cloneBmp)
                cloneCanvas.drawBitmap(srcSurface.bitmap, 0f, 0f, null)
                layerSurfaces[newId] = LayerSurface(newId, cloneBmp, cloneCanvas)
            }

            val afterLayers = doc.layers.toMutableList().apply { add(index + 1, duplicated) }
            val command = LayerStructureCommand(
                description = "Duplicate Layer",
                beforeLayers = doc.layers,
                afterLayers = afterLayers,
                beforeActiveLayerId = activeLayerId,
                afterActiveLayerId = newId,
                onApplyLayers = ::applyLayerStructureChange
            )
            historyManager.execute(command)
        }
    }

    fun mergeLayerDown(layerId: String) {
        synchronized(lock) {
            val doc = document ?: return
            val index = doc.layers.indexOfFirst { it.id == layerId }
            if (index <= 0) return

            val topLayer = doc.layers[index]
            val bottomLayer = doc.layers[index - 1]
            val mergedStrokes = bottomLayer.strokes + topLayer.strokes.map {
                it.copy(layerId = bottomLayer.id)
            }
            val mergedLayer = bottomLayer.copy(strokes = mergedStrokes)

            // Fast composite onto bottom surface if both are normal opacity/visibility
            val topSurface = layerSurfaces[topLayer.id]
            val bottomSurface = layerSurfaces[bottomLayer.id]
            if (topSurface != null && bottomSurface != null && topLayer.isVisible && topLayer.opacity > 0f) {
                val mergePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    alpha = (topLayer.opacity.coerceIn(0f, 1f) * 255f).toInt()
                    xfermode = topLayer.blendMode.toPorterDuff()
                }
                bottomSurface.canvas.drawBitmap(topSurface.bitmap, 0f, 0f, mergePaint)
            }

            val afterLayers = doc.layers.toMutableList().apply {
                set(index - 1, mergedLayer)
                removeAt(index)
            }

            val command = LayerStructureCommand(
                description = "Merge Layer Down",
                beforeLayers = doc.layers,
                afterLayers = afterLayers,
                beforeActiveLayerId = activeLayerId,
                afterActiveLayerId = bottomLayer.id,
                onApplyLayers = ::applyLayerStructureChange
            )
            historyManager.execute(command)
        }
    }

    private fun updateLayerMetadata(
        layerId: String,
        description: String,
        transform: (LayerData) -> LayerData
    ) {
        synchronized(lock) {
            val doc = document ?: return
            val beforeLayer = doc.layers.firstOrNull { it.id == layerId } ?: return
            val afterLayer = transform(beforeLayer)
            if (beforeLayer == afterLayer) return

            val command = UpdateLayerMetadataCommand(
                description = description,
                layerId = layerId,
                beforeLayer = beforeLayer,
                afterLayer = afterLayer,
                onUpdateLayer = { target ->
                    synchronized(lock) {
                        val currentDoc = document ?: return@synchronized
                        document = currentDoc.replaceLayer(target)
                        syncLayerFlowsLocked()
                        tileGrid?.markAllDirty()
                        recompositeDirtyTilesLocked()
                    }
                    onFrameInvalidated?.invoke()
                }
            )
            historyManager.execute(command)
        }
    }

    private fun applyLayerStructureChange(layers: List<LayerData>, nextActiveLayerId: String?) {
        synchronized(lock) {
            val doc = document ?: return
            val targetIds = layers.map { it.id }.toSet()

            // Release surfaces no longer present
            val iterator = layerSurfaces.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.key !in targetIds) {
                    renderBackend.releaseSurface(entry.value.bitmap)
                    iterator.remove()
                }
            }

            // Allocate/rasterize any missing surfaces (e.g., undone delete or undone merge)
            for (layer in layers) {
                if (!layerSurfaces.containsKey(layer.id)) {
                    allocateAndRasterizeLayerLocked(doc.width, doc.height, layer)
                }
            }

            document = doc.copy(layers = layers)
            activeLayerId = nextActiveLayerId?.takeIf { it in targetIds } ?: layers.lastOrNull()?.id
            syncLayerFlowsLocked()
            tileGrid?.markAllDirty()
            recompositeDirtyTilesLocked()
        }
        onFrameInvalidated?.invoke()
    }

    private fun allocateAndRasterizeLayerLocked(width: Int, height: Int, layer: LayerData) {
        val bmp = renderBackend.allocateSurface(width, height)
        val canvas = Canvas(bmp)
        for (stroke in layer.strokes) {
            rasterizer.drawStroke(canvas, stroke)
        }
        layerSurfaces[layer.id] = LayerSurface(layer.id, bmp, canvas)
    }

    private fun syncLayerFlowsLocked() {
        _layersState.value = document?.layers ?: emptyList()
        _activeLayerIdState.value = activeLayerId
    }

    // --- Stroke Operations ---

    fun beginStroke(x: Float, y: Float, pressure: Float, timestampMs: Long) {
        synchronized(lock) {
            val doc = document ?: return
            val layerId = activeLayerId ?: return
            val surface = layerSurfaces[layerId] ?: return

            stabilizer.reset()
            val firstPoint = stabilizer.filter(StrokePoint(x, y, pressure, timestampMs))
            currentStrokePoints = mutableListOf(firstPoint)
            currentStrokeBrush = activeBrush.copy(seed = System.nanoTime())
            lastMidX = firstPoint.x
            lastMidY = firstPoint.y
            liveDabIndex = 1

            val scratch = renderBackend.allocateSurface(doc.width, doc.height)
            RegionRestoreHelper.copyRegion(
                source = surface.bitmap,
                rect = Rect(0, 0, doc.width, doc.height),
                target = scratch
            )
            preStrokeBackup = scratch
            preStrokeLayerBitmap = surface.bitmap

            rasterizer.drawLiveDot(surface.canvas, currentStrokeBrush, firstPoint)
            val bounds = rasterizer.computeDirtyBounds(
                currentStrokePoints,
                currentStrokeBrush,
                doc.width,
                doc.height
            )
            activeDirtyBounds.set(bounds)
            tileGrid?.markDirty(bounds)
            recompositeDirtyTilesLocked()
        }
        onFrameInvalidated?.invoke()
    }

    fun moveStroke(x: Float, y: Float, pressure: Float, timestampMs: Long) {
        synchronized(lock) {
            val doc = document ?: return
            val layerId = activeLayerId ?: return
            val surface = layerSurfaces[layerId] ?: return
            if (currentStrokePoints.isEmpty()) return

            val filtered = stabilizer.filter(StrokePoint(x, y, pressure, timestampMs))
            val prev = currentStrokePoints.last()
            currentStrokePoints.add(filtered)

            val newMidX = (prev.x + filtered.x) * 0.5f
            val newMidY = (prev.y + filtered.y) * 0.5f

            liveDabIndex = rasterizer.drawLiveSegment(
                canvas = surface.canvas,
                brush = currentStrokeBrush,
                from = StrokePoint(lastMidX, lastMidY),
                control = prev,
                to = StrokePoint(newMidX, newMidY),
                startDabIndex = liveDabIndex
            )
            lastMidX = newMidX
            lastMidY = newMidY

            val segBounds = rasterizer.computeDirtyBounds(
                listOf(prev, filtered),
                currentStrokeBrush,
                doc.width,
                doc.height
            )
            activeDirtyBounds.union(segBounds)
            tileGrid?.markDirty(segBounds)
            recompositeDirtyTilesLocked()
        }
        onFrameInvalidated?.invoke()
    }

    fun endStroke() {
        synchronized(lock) {
            val doc = document ?: return
            val layerId = activeLayerId ?: return
            layerSurfaces[layerId] ?: return
            val scratchBackup = preStrokeBackup
            preStrokeBackup = null
            preStrokeLayerBitmap = null

            if (currentStrokePoints.isEmpty()) {
                renderBackend.releaseSurface(scratchBackup)
                return
            }

            val finalPoints = currentStrokePoints.toList()
            currentStrokePoints.clear()

            val stroke = StrokeData(
                id = UUID.randomUUID().toString(),
                layerId = layerId,
                brush = currentStrokeBrush,
                points = finalPoints
            )

            appendStrokeToDocumentLocked(stroke)

            val dirtyRect = Rect(
                max(0, floor(activeDirtyBounds.left).toInt()),
                max(0, floor(activeDirtyBounds.top).toInt()),
                min(doc.width, ceil(activeDirtyBounds.right).toInt()),
                min(doc.height, ceil(activeDirtyBounds.bottom).toInt())
            )

            val croppedBackup = if (!dirtyRect.isEmpty && scratchBackup != null) {
                val cropped = renderBackend.allocateSurface(dirtyRect.width(), dirtyRect.height())
                RegionRestoreHelper.copyRegion(scratchBackup, dirtyRect, cropped)
                cropped
            } else {
                null
            }
            renderBackend.releaseSurface(scratchBackup)

            val command = DrawStrokeCommand(
                stroke = stroke,
                dirtyRect = dirtyRect,
                beforeRegionBitmap = croppedBackup,
                onApplyStroke = { applied ->
                    synchronized(lock) {
                        val targetSurface = layerSurfaces[applied.layerId] ?: return@synchronized
                        rasterizer.drawStroke(targetSurface.canvas, applied)
                        appendStrokeToDocumentLocked(applied)
                        tileGrid?.markDirty(
                            RectF(
                                dirtyRect.left.toFloat(),
                                dirtyRect.top.toFloat(),
                                dirtyRect.right.toFloat(),
                                dirtyRect.bottom.toFloat()
                            )
                        )
                        recompositeDirtyTilesLocked()
                    }
                    onFrameInvalidated?.invoke()
                },
                onRestoreRegion = { targetLayerId, strokeId, rect, backup ->
                    synchronized(lock) {
                        val targetSurface = layerSurfaces[targetLayerId] ?: return@synchronized
                        if (backup != null && !rect.isEmpty) {
                            RegionRestoreHelper.restoreRegion(targetSurface.bitmap, rect, backup)
                        }
                        removeStrokeFromDocumentLocked(targetLayerId, strokeId)
                        tileGrid?.markDirty(
                            RectF(
                                rect.left.toFloat(),
                                rect.top.toFloat(),
                                rect.right.toFloat(),
                                rect.bottom.toFloat()
                            )
                        )
                        recompositeDirtyTilesLocked()
                    }
                    onFrameInvalidated?.invoke()
                }
            )

            historyManager.recordExecuted(command)
        }
    }

    fun cancelActiveStroke() {
        synchronized(lock) {
            val backup = preStrokeBackup
            val layerBmp = preStrokeLayerBitmap
            val doc = document
            preStrokeBackup = null
            preStrokeLayerBitmap = null
            currentStrokePoints.clear()

            if (backup != null && layerBmp != null && doc != null) {
                RegionRestoreHelper.restoreRegion(
                    target = layerBmp,
                    rect = Rect(0, 0, doc.width, doc.height),
                    backup = backup
                )
                tileGrid?.markAllDirty()
                recompositeDirtyTilesLocked()
            }
            renderBackend.releaseSurface(backup)
        }
        onFrameInvalidated?.invoke()
    }

    fun undo(): Boolean = historyManager.undo()

    fun redo(): Boolean = historyManager.redo()

    fun drawCompositeTo(canvas: Canvas) {
        synchronized(lock) {
            val bmp = compositeBitmap ?: return
            if (!bmp.isRecycled) {
                canvas.drawBitmap(bmp, 0f, 0f, null)
            }
        }
    }

    fun snapshotDocument(): ArtworkDocument? = synchronized(lock) { document }

    fun renderExportBitmap(config: ExportConfig): Bitmap? = synchronized(lock) {
        val doc = document ?: return@synchronized null
        val (targetW, targetH) = config.resolveDimensions(doc.width, doc.height)
        val outBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val outCanvas = Canvas(outBitmap)

        if (!config.effectiveTransparent) {
            outCanvas.drawColor(doc.backgroundColorArgb)
        }

        val scaleX = targetW / doc.width.toFloat()
        val scaleY = targetH / doc.height.toFloat()
        outCanvas.save()
        outCanvas.scale(scaleX, scaleY)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        for (layer in doc.layers) {
            if (!layer.isVisible || layer.opacity <= 0f) continue
            val surface = layerSurfaces[layer.id] ?: continue
            if (surface.bitmap.isRecycled) continue
            paint.alpha = (layer.opacity.coerceIn(0f, 1f) * 255f).toInt()
            paint.xfermode = layer.blendMode.toPorterDuff()
            outCanvas.drawBitmap(surface.bitmap, 0f, 0f, paint)
        }
        outCanvas.restore()
        outBitmap
    }

    fun renderThumbnailPngBytes(maxDimension: Int = 360): ByteArray? = synchronized(lock) {
        val bmp = compositeBitmap ?: return@synchronized null
        if (bmp.isRecycled) return@synchronized null
        val scale = min(
            maxDimension / bmp.width.toFloat(),
            maxDimension / bmp.height.toFloat()
        ).coerceAtMost(1f)
        val targetW = max(1, (bmp.width * scale).toInt())
        val targetH = max(1, (bmp.height * scale).toInt())
        val scaled = Bitmap.createScaledBitmap(bmp, targetW, targetH, true)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 90, out)
        if (scaled !== bmp) scaled.recycle()
        out.toByteArray()
    }

    fun trimMemory(level: Int) {
        renderBackend.trimMemory(level)
    }

    fun release() {
        synchronized(lock) {
            releaseSurfacesLocked()
        }
        backgroundExecutor.shutdown()
    }

    private fun recompositeDirtyTilesLocked() {
        val doc = document ?: return
        val canvas = compositeCanvas ?: return
        val dirtyRect = tileGrid?.consumeDirtyBounds() ?: return

        val renderLayers = doc.layers.mapNotNull { layer ->
            val surface = layerSurfaces[layer.id] ?: return@mapNotNull null
            RenderLayer(
                id = layer.id,
                bitmap = surface.bitmap,
                isVisible = layer.isVisible,
                opacity = layer.opacity,
                blendMode = layer.blendMode
            )
        }

        renderBackend.composite(
            target = canvas,
            backgroundColorArgb = doc.backgroundColorArgb,
            layers = renderLayers,
            dirtyRect = dirtyRect
        )
    }

    private fun appendStrokeToDocumentLocked(stroke: StrokeData) {
        val doc = document ?: return
        val updatedLayers = doc.layers.map { layer ->
            if (layer.id == stroke.layerId) {
                layer.copy(strokes = layer.strokes + stroke)
            } else {
                layer
            }
        }
        document = doc.copy(layers = updatedLayers)
        syncLayerFlowsLocked()
    }

    private fun removeStrokeFromDocumentLocked(layerId: String, strokeId: String) {
        val doc = document ?: return
        val updatedLayers = doc.layers.map { layer ->
            if (layer.id == layerId) {
                layer.copy(strokes = layer.strokes.filterNot { it.id == strokeId })
            } else {
                layer
            }
        }
        document = doc.copy(layers = updatedLayers)
        syncLayerFlowsLocked()
    }

    private fun releaseSurfacesLocked() {
        renderBackend.releaseSurface(preStrokeBackup)
        preStrokeBackup = null
        preStrokeLayerBitmap = null
        renderBackend.releaseSurface(compositeBitmap)
        compositeBitmap = null
        compositeCanvas = null
        layerSurfaces.values.forEach { renderBackend.releaseSurface(it.bitmap) }
        layerSurfaces.clear()
    }

    private fun BlendMode.toPorterDuff(): PorterDuffXfermode? = when (this) {
        BlendMode.NORMAL -> null
        BlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        BlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        BlendMode.OVERLAY -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
        BlendMode.DARKEN -> PorterDuffXfermode(PorterDuff.Mode.DARKEN)
        BlendMode.LIGHTEN -> PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
    }
}
