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
        val trimmedName = name.trim().ifEmpty { "Untitled Wall" }
        viewModelScope.launch {
            val created = projectRepository.createProject(
                name = trimmedName,
                width = preset.width,
                height = preset.height,
                backgroundColorArgb = backgroundColorArgb
            )
            _showCreateDialog.value = false
            onCreated(created.id)
        }
    }

    fun renameProject(projectId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            projectRepository.renameProject(projectId, trimmed)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
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
