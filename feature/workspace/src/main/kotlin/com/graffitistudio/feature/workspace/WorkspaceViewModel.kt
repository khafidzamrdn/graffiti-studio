package com.graffitistudio.feature.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.graffitistudio.core.data.preferences.WorkspacePreferences
import com.graffitistudio.core.domain.history.HistoryState
import com.graffitistudio.core.domain.model.BrushConfig
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

data class WorkspaceUiState(
    val projectName: String = "",
    val dimensionsLabel: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val activeBrush: BrushConfig = BrushConfig(),
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
    }

    private fun loadProject() {
        viewModelScope.launch {
            val prefs = preferences.toolPreferences.first()
            val initialBrush = BrushConfig(
                type = BrushType.BASIC_BRUSH,
                colorArgb = prefs.brushColorArgb,
                sizePx = prefs.brushSizePx
            )
            drawingEngine.updateBrush(initialBrush)

            val doc = projectRepository.loadDocument(projectId)
            if (doc != null) {
                drawingEngine.loadDocument(doc) {
                    _uiState.value = WorkspaceUiState(
                        projectName = doc.name,
                        dimensionsLabel = "${doc.width}×${doc.height}",
                        isLoading = false,
                        activeBrush = initialBrush
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectBrushType(type: BrushType) {
        val updated = _uiState.value.activeBrush.copy(type = type)
        drawingEngine.updateBrush(updated)
        _uiState.value = _uiState.value.copy(activeBrush = updated)
    }

    fun setBrushSize(sizePx: Float) {
        val clamped = sizePx.coerceIn(2f, 120f)
        val updated = _uiState.value.activeBrush.copy(sizePx = clamped)
        drawingEngine.updateBrush(updated)
        _uiState.value = _uiState.value.copy(activeBrush = updated)
        viewModelScope.launch {
            preferences.saveBrushSize(clamped)
        }
    }

    fun setBrushColor(colorArgb: Int) {
        val updated = _uiState.value.activeBrush.copy(
            type = BrushType.BASIC_BRUSH,
            colorArgb = colorArgb
        )
        drawingEngine.updateBrush(updated)
        _uiState.value = _uiState.value.copy(activeBrush = updated)
        viewModelScope.launch {
            preferences.saveBrushColor(colorArgb)
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
