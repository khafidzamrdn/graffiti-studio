package com.graffitistudio.feature.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.graffitistudio.core.data.preferences.WorkspacePreferences
import com.graffitistudio.core.domain.history.HistoryState
import com.graffitistudio.core.domain.model.BlendMode
import com.graffitistudio.core.domain.model.BrushConfig
import com.graffitistudio.core.domain.model.BrushPreset
import com.graffitistudio.core.domain.model.BrushPresets
import com.graffitistudio.core.domain.model.BrushType
import com.graffitistudio.core.domain.model.ExportConfig
import com.graffitistudio.core.domain.model.GraffitiTextElement
import com.graffitistudio.core.domain.model.LayerData
import com.graffitistudio.core.domain.model.LayerEffect
import com.graffitistudio.core.domain.repository.ProjectRepository
import com.graffitistudio.core.storage.ArtworkExporter
import com.graffitistudio.engine.drawing.DrawingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class WorkspaceUiState(
    val projectName: String = "",
    val documentWidth: Int = 1080,
    val documentHeight: Int = 1080,
    val dimensionsLabel: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isExporting: Boolean = false,
    val isExportDialogOpen: Boolean = false,
    val isTypographyDialogOpen: Boolean = false,
    val isEffectsDialogOpen: Boolean = false,
    val isColorPickerOpen: Boolean = false,
    val exportStatusMessage: String? = null,
    val activeBrush: BrushConfig = BrushConfig(),
    val lastUsedNonEraserType: BrushType = BrushType.PEN,
    val isBrushPanelOpen: Boolean = false,
    val isLayerPanelOpen: Boolean = false,
    val presets: List<BrushPreset> = BrushPresets.builtIn,
    val activePresetId: String? = null,
    val lastSavedMessage: String? = null
)

class WorkspaceViewModel(
    private val projectId: String,
    private val projectRepository: ProjectRepository,
    private val preferences: WorkspacePreferences,
    private val artworkExporter: ArtworkExporter,
    val drawingEngine: DrawingEngine = DrawingEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    val historyState: StateFlow<HistoryState> = drawingEngine.historyState
    val layersState: StateFlow<List<LayerData>> = drawingEngine.layersState
    val activeLayerIdState: StateFlow<String?> = drawingEngine.activeLayerIdState

    init {
        loadProject()
        observeCustomPresets()
        startBackgroundAutosave()
    }

    private fun startBackgroundAutosave() {
        viewModelScope.launch {
            while (true) {
                delay(20_000L)
                val doc = drawingEngine.snapshotDocument() ?: continue
                runCatching { projectRepository.autosaveDocument(doc) }
            }
        }
    }

    private fun observeCustomPresets() {
        viewModelScope.launch {
            preferences.toolPreferences.collect { prefs ->
                _uiState.value = _uiState.value.copy(
                    presets = BrushPresets.builtIn + prefs.customPresets
                )
            }
        }
    }

    private fun loadProject() {
        viewModelScope.launch {
            val prefs = preferences.toolPreferences.first()
            val initialBrush = prefs.toBrushConfig()
            drawingEngine.updateBrush(initialBrush)

            val doc = projectRepository.loadDocument(projectId)
            if (doc != null) {
                drawingEngine.loadDocument(doc) {
                    _uiState.value = _uiState.value.copy(
                        projectName = doc.name,
                        documentWidth = doc.width,
                        documentHeight = doc.height,
                        dimensionsLabel = "${doc.width}×${doc.height}",
                        isLoading = false,
                        activeBrush = initialBrush,
                        lastUsedNonEraserType = if (initialBrush.type == BrushType.ERASER) {
                            BrushType.PEN
                        } else {
                            initialBrush.type
                        }
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun toggleBrushPanel() {
        _uiState.value = _uiState.value.copy(
            isBrushPanelOpen = !_uiState.value.isBrushPanelOpen,
            isLayerPanelOpen = false
        )
    }

    fun toggleLayerPanel() {
        _uiState.value = _uiState.value.copy(
            isLayerPanelOpen = !_uiState.value.isLayerPanelOpen,
            isBrushPanelOpen = false
        )
    }

    fun selectLayer(layerId: String) = drawingEngine.selectLayer(layerId)

    fun addLayer() = drawingEngine.addLayer()

    fun deleteLayer(layerId: String) = drawingEngine.deleteLayer(layerId)

    fun renameLayer(layerId: String, newName: String) = drawingEngine.renameLayer(layerId, newName)

    fun setLayerVisibility(layerId: String, visible: Boolean) =
        drawingEngine.setLayerVisibility(layerId, visible)

    fun setLayerOpacity(layerId: String, opacity: Float) =
        drawingEngine.setLayerOpacity(layerId, opacity)

    fun setLayerBlendMode(layerId: String, blendMode: BlendMode) =
        drawingEngine.setLayerBlendMode(layerId, blendMode)

    fun moveLayerUp(layerId: String) = drawingEngine.moveLayer(layerId, delta = 1)

    fun moveLayerDown(layerId: String) = drawingEngine.moveLayer(layerId, delta = -1)

    fun duplicateLayer(layerId: String) = drawingEngine.duplicateLayer(layerId)

    fun mergeLayerDown(layerId: String) = drawingEngine.mergeLayerDown(layerId)

    fun selectBrushType(type: BrushType) {
        val current = _uiState.value.activeBrush
        val updated = current.copy(type = type)
        applyBrushUpdate(updated, presetId = null)
    }

    fun applyPreset(preset: BrushPreset) {
        val currentColor = _uiState.value.activeBrush.colorArgb
        val updated = preset.brush.copy(colorArgb = currentColor)
        applyBrushUpdate(updated, presetId = preset.id)
    }

    fun setBrushSize(sizePx: Float) {
        val updated = _uiState.value.activeBrush.copy(sizePx = sizePx.coerceIn(2f, 150f))
        applyBrushUpdate(updated, presetId = _uiState.value.activePresetId)
    }

    fun setBrushOpacity(opacity: Float) {
        val updated = _uiState.value.activeBrush.copy(opacity = opacity.coerceIn(0.05f, 1f))
        applyBrushUpdate(updated, presetId = null)
    }

    fun setBrushFlow(flow: Float) {
        val updated = _uiState.value.activeBrush.copy(flow = flow.coerceIn(0.1f, 1f))
        applyBrushUpdate(updated, presetId = null)
    }

    fun setStabilizerLevel(level: Float) {
        val updated = _uiState.value.activeBrush.copy(stabilizerLevel = level.coerceIn(0f, 0.9f))
        applyBrushUpdate(updated, presetId = null)
    }

    fun setBrushColor(colorArgb: Int) {
        val current = _uiState.value.activeBrush
        val nextType = if (current.type == BrushType.ERASER) {
            _uiState.value.lastUsedNonEraserType
        } else {
            current.type
        }
        val updated = current.copy(type = nextType, colorArgb = colorArgb)
        applyBrushUpdate(updated, presetId = _uiState.value.activePresetId)
    }

    fun saveCurrentAsPreset(name: String) {
        val trimmed = name.trim().ifEmpty { "Custom ${_uiState.value.activeBrush.type.displayName}" }
        val preset = BrushPreset(
            id = "custom_${UUID.randomUUID()}",
            name = trimmed,
            brush = _uiState.value.activeBrush.copy(seed = 0L),
            isCustom = true
        )
        viewModelScope.launch {
            preferences.saveCustomPreset(preset)
            _uiState.value = _uiState.value.copy(activePresetId = preset.id)
        }
    }

    fun deleteCustomPreset(presetId: String) {
        viewModelScope.launch {
            preferences.deleteCustomPreset(presetId)
        }
    }

    private fun applyBrushUpdate(updated: BrushConfig, presetId: String?) {
        drawingEngine.updateBrush(updated)
        val nonEraser = if (updated.type != BrushType.ERASER) {
            updated.type
        } else {
            _uiState.value.lastUsedNonEraserType
        }
        _uiState.value = _uiState.value.copy(
            activeBrush = updated,
            lastUsedNonEraserType = nonEraser,
            activePresetId = presetId
        )
        viewModelScope.launch {
            preferences.saveBrushConfig(updated)
        }
    }

    fun undo() {
        drawingEngine.undo()
    }

    fun redo() {
        drawingEngine.redo()
    }

    fun openTypographyDialog() {
        _uiState.value = _uiState.value.copy(isTypographyDialogOpen = true)
    }

    fun dismissTypographyDialog() {
        _uiState.value = _uiState.value.copy(isTypographyDialogOpen = false)
    }

    fun upsertTextElement(element: GraffitiTextElement) {
        drawingEngine.upsertTextElement(element)
        _uiState.value = _uiState.value.copy(isTypographyDialogOpen = false)
    }

    fun deleteTextElement(layerId: String, elementId: String) {
        drawingEngine.deleteTextElement(layerId, elementId)
    }

    fun openEffectsDialog() {
        _uiState.value = _uiState.value.copy(isEffectsDialogOpen = true)
    }

    fun dismissEffectsDialog() {
        _uiState.value = _uiState.value.copy(isEffectsDialogOpen = false)
    }

    fun addLayerEffect(layerId: String, effect: LayerEffect) {
        drawingEngine.addLayerEffect(layerId, effect)
    }

    fun removeLayerEffect(layerId: String, effectId: String) {
        drawingEngine.removeLayerEffect(layerId, effectId)
    }

    fun openColorPicker() {
        _uiState.value = _uiState.value.copy(isColorPickerOpen = true)
    }

    fun dismissColorPicker() {
        _uiState.value = _uiState.value.copy(isColorPickerOpen = false)
    }

    fun trimMemory(level: Int) {
        drawingEngine.trimMemory(level)
    }

    fun openExportDialog() {
        _uiState.value = _uiState.value.copy(isExportDialogOpen = true, exportStatusMessage = null)
    }

    fun dismissExportDialog() {
        _uiState.value = _uiState.value.copy(isExportDialogOpen = false)
    }

    fun clearExportStatus() {
        _uiState.value = _uiState.value.copy(exportStatusMessage = null)
    }

    fun exportArtwork(config: ExportConfig) {
        val docName = _uiState.value.projectName.ifEmpty { "GraffitiStudio" }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val result = runCatching {
                val bitmap = withContext(Dispatchers.Default) {
                    drawingEngine.renderExportBitmap(config)
                } ?: error("Canvas not ready")
                artworkExporter.exportBitmap(bitmap, docName, config)
            }
            _uiState.value = result.fold(
                onSuccess = { exported ->
                    _uiState.value.copy(
                        isExporting = false,
                        isExportDialogOpen = false,
                        exportStatusMessage = "Exported: ${exported.locationLabel}"
                    )
                },
                onFailure = { err ->
                    _uiState.value.copy(
                        isExporting = false,
                        exportStatusMessage = "Export failed: ${err.message}"
                    )
                }
            )
        }
    }

    fun saveProject(onSaved: (() -> Unit)? = null) {
        if (_uiState.value.isSaving) return
        val doc = drawingEngine.snapshotDocument() ?: run {
            onSaved?.invoke()
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val result = runCatching {
                val thumbBytes = withContext(Dispatchers.Default) {
                    drawingEngine.renderThumbnailPngBytes()
                }
                projectRepository.saveDocument(doc, thumbBytes)
            }
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    lastSavedMessage = "Saved"
                )
                onSaved?.invoke()
            } else {
                val error = result.exceptionOrNull()
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    exportStatusMessage = "Save failed: ${error?.message ?: "unknown error"}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        drawingEngine.release()
    }

    class Factory(
        private val projectId: String,
        private val projectRepository: ProjectRepository,
        private val preferences: WorkspacePreferences,
        private val artworkExporter: ArtworkExporter
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkspaceViewModel(projectId, projectRepository, preferences, artworkExporter) as T
        }
    }
}
