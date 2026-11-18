package com.graffitistudio.engine.drawing

import com.graffitistudio.core.domain.history.HistoryCommand
import com.graffitistudio.core.domain.model.ArtworkDocument
import com.graffitistudio.core.domain.model.BlendMode
import com.graffitistudio.core.domain.model.LayerData

/**
 * Snapshot-based command for structural layer operations (add, delete, reorder,
 * duplicate, merge) that preserves exact layer order and active selection on undo/redo.
 *
 * Layer bitmaps are managed by [DrawingEngine] without re-rasterizing unchanged layers
 * whenever possible.
 */
internal class LayerStructureCommand(
    override val description: String,
    private val beforeLayers: List<LayerData>,
    private val afterLayers: List<LayerData>,
    private val beforeActiveLayerId: String?,
    private val afterActiveLayerId: String?,
    private val onApplyLayers: (layers: List<LayerData>, activeLayerId: String?) -> Unit
) : HistoryCommand {

    override fun execute() {
        onApplyLayers(afterLayers, afterActiveLayerId)
    }

    override fun undo() {
        onApplyLayers(beforeLayers, beforeActiveLayerId)
    }
}

/**
 * Lightweight command for non-pixel layer metadata updates (visibility, opacity,
 * blend mode, rename) that never re-rasterize layer bitmaps.
 */
internal class UpdateLayerMetadataCommand(
    override val description: String,
    private val layerId: String,
    private val beforeLayer: LayerData,
    private val afterLayer: LayerData,
    private val onUpdateLayer: (LayerData) -> Unit
) : HistoryCommand {

    override fun execute() {
        onUpdateLayer(afterLayer)
    }

    override fun undo() {
        onUpdateLayer(beforeLayer)
    }

    override fun canMergeWith(next: HistoryCommand): Boolean {
        return next is UpdateLayerMetadataCommand &&
            next.layerId == layerId &&
            next.description == description
    }

    override fun mergeWith(next: HistoryCommand): HistoryCommand {
        val nextUpdate = next as UpdateLayerMetadataCommand
        return UpdateLayerMetadataCommand(
            description = description,
            layerId = layerId,
            beforeLayer = beforeLayer,
            afterLayer = nextUpdate.afterLayer,
            onUpdateLayer = onUpdateLayer
        )
    }
}

/**
 * Command for merging the layer above into the layer below.
 *
 * Merge bakes the top layer's pixels (opacity, blend mode, effects, text) into the
 * bottom layer bitmap, so it cannot be reconstructed from layer metadata alone.
 * The pre-merge bottom bitmap is therefore captured as a backup and restored on undo,
 * while redo re-applies the pixel composite.
 */
internal class MergeLayersCommand(
    override val description: String,
    private val beforeLayers: List<LayerData>,
    private val afterLayers: List<LayerData>,
    private val beforeActiveLayerId: String?,
    private val afterActiveLayerId: String?,
    private val onExecuteMerge: () -> Unit,
    private val onUndoMerge: () -> Unit,
    private val onApplyLayers: (layers: List<LayerData>, activeLayerId: String?) -> Unit,
    private val onDispose: () -> Unit
) : HistoryCommand {

    override fun execute() {
        onExecuteMerge()
        onApplyLayers(afterLayers, afterActiveLayerId)
    }

    override fun undo() {
        onUndoMerge()
        onApplyLayers(beforeLayers, beforeActiveLayerId)
    }

    override fun dispose() = onDispose()
}

internal fun ArtworkDocument.replaceLayer(updated: LayerData): ArtworkDocument =
    copy(layers = layers.map { if (it.id == updated.id) updated else it })

internal fun BlendMode.displayName(): String = when (this) {
    BlendMode.NORMAL -> "Normal"
    BlendMode.MULTIPLY -> "Multiply"
    BlendMode.SCREEN -> "Screen"
    BlendMode.OVERLAY -> "Overlay"
    BlendMode.DARKEN -> "Darken"
    BlendMode.LIGHTEN -> "Lighten"
}
