package com.graffitistudio.core.domain.repository

import com.graffitistudio.core.domain.model.ArtworkDocument
import com.graffitistudio.core.domain.model.ProjectMetadata
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjects(): Flow<List<ProjectMetadata>>

    suspend fun getProject(projectId: String): ProjectMetadata?

    suspend fun createProject(
        name: String,
        width: Int,
        height: Int,
        backgroundColorArgb: Int
    ): ProjectMetadata

    suspend fun loadDocument(projectId: String): ArtworkDocument?

    suspend fun saveDocument(
        document: ArtworkDocument,
        thumbnailPngBytes: ByteArray? = null
    )

    suspend fun autosaveDocument(document: ArtworkDocument)

    suspend fun renameProject(projectId: String, newName: String)

    suspend fun deleteProject(projectId: String)
}
