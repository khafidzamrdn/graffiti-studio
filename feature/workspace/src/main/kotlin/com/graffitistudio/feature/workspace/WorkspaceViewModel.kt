package com.graffitistudio.feature.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.graffitistudio.core.data.preferences.WorkspacePreferences
import com.graffitistudio.core.domain.history.HistoryState
import com.graffitistudio.core.domain.model.BrushConfig
import com.graffitistudio.core.domain.model.BrushPreset
import com.graffitistudio.core.domain.model.BrushPresets
import com.graffitistudio.core.domain.model.BrushType
import com.graffitistudio.core.domain.repository.ProjectRepository
import com.graffitistudio.engine.drawing.DrawingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class WorkspaceUiState(
    val projectName: String = "",
    val dimensionsLabel: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val activeBrush: BrushConfig = BrushConfig(),
    val lastUsedNonEraserType: BrushType = BrushType.PEN,
    val isBrushPanelOpen: Boolean = false,
    val presets: List<BrushPreset> = BrushPresets.builtIn,
    val activePresetId: String? = null,
    val lastSavedMessage: String? = null
)

class WorkspaceViewModel(
    private val projectId: String,
    private val projectRepository: ProjectRepository,
    private val preferences: WorkspacePreferences,
    val drawingEngine: DrawingEngine = DrawingEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    val historyState: StateFlow<HistoryState> = drawingEngine.historyState

    init {
        loadProject()
        observeCustomPresets()
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
            isBrushPanelOpen = !_uiState.value.isBrushPanelOpen
        )
    }

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

    fun saveProject(onSaved: (() -> Unit)? = null) {
        val doc = drawingEngine.snapshotDocument() ?: run {
            onSaved?.invoke()
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val thumbBytes = withContext(Dispatchers.Default) {
                drawingEngine.renderThumbnailPngBytes()
            }
            projectRepository.saveDocument(doc, thumbBytes)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                lastSavedMessage = "Saved"
            )
            onSaved?.invoke()
        }
    }

    override fun onCleared() {
        super.onCleared()
        drawingEngine.release()
    }

    class Factory(
        private val projectId: String,
        private val projectRepository: ProjectRepository,
        private val preferences: WorkspacePreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkspaceViewModel(projectId, projectRepository, preferences) as T
        }
    }
}
