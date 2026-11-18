package com.graffitistudio.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.graffitistudio.core.domain.model.CanvasPreset
import com.graffitistudio.core.domain.model.ProjectMetadata
import com.graffitistudio.core.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    val projects: StateFlow<List<ProjectMetadata>> = projectRepository
        .observeProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun openCreateDialog() {
        _showCreateDialog.value = true
    }

    fun dismissCreateDialog() {
        _showCreateDialog.value = false
    }

    fun createProject(
        name: String,
        preset: CanvasPreset,
        backgroundColorArgb: Int,
        onCreated: (String) -> Unit
    ) {
        if (_isCreating.value) return
        val trimmedName = name.trim().ifEmpty { "Untitled Wall" }
        viewModelScope.launch {
            _isCreating.value = true
            runCatching {
                projectRepository.createProject(
                    name = trimmedName,
                    width = preset.width,
                    height = preset.height,
                    backgroundColorArgb = backgroundColorArgb
                )
            }.onSuccess { created ->
                _showCreateDialog.value = false
                onCreated(created.id)
            }.onFailure { error ->
                _message.value = "Could not create project: ${error.message ?: "unknown error"}"
            }
            _isCreating.value = false
        }
    }

    fun renameProject(projectId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                projectRepository.renameProject(projectId, trimmed)
            }.onFailure { error ->
                _message.value = "Rename failed: ${error.message ?: "unknown error"}"
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            runCatching {
                projectRepository.deleteProject(projectId)
            }.onFailure { error ->
                _message.value = "Delete failed: ${error.message ?: "unknown error"}"
            }
        }
    }

    class Factory(
        private val projectRepository: ProjectRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(projectRepository) as T
        }
    }
}
