package com.graffitistudio.core.data.repository

import com.graffitistudio.core.data.local.ProjectDao
import com.graffitistudio.core.data.local.ProjectEntity
import com.graffitistudio.core.domain.model.ArtworkDocument
import com.graffitistudio.core.domain.model.BackgroundSurfaceType
import com.graffitistudio.core.domain.model.DocumentBackground
import com.graffitistudio.core.domain.model.LayerData
import com.graffitistudio.core.domain.model.ProjectMetadata
import com.graffitistudio.core.domain.repository.ProjectRepository
import com.graffitistudio.core.storage.GspProjectStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProjectRepositoryImpl(
    private val projectDao: ProjectDao,
    private val storage: GspProjectStorage
) : ProjectRepository {

    override fun observeProjects(): Flow<List<ProjectMetadata>> {
        return projectDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getProject(projectId: String): ProjectMetadata? {
        return projectDao.getById(projectId)?.toDomain()
    }

    override suspend fun createProject(
        name: String,
        width: Int,
        height: Int,
        backgroundColorArgb: Int,
        backgroundSurface: BackgroundSurfaceType,
        initialBackground: DocumentBackground?
    ): ProjectMetadata {
        val now = System.currentTimeMillis()
        val projectId = UUID.randomUUID().toString()
        val initialDoc = ArtworkDocument(
            projectId = projectId,
            name = name,
            width = width,
            height = height,
            backgroundColorArgb = backgroundColorArgb,
            backgroundSurface = backgroundSurface,
            background = initialBackground,
            layers = listOf(
                LayerData(
                    id = UUID.randomUUID().toString(),
                    name = "Layer 1"
                )
            )
        )
        val gspPath = storage.saveDocument(initialDoc, thumbnailPngBytes = null)
        val metadata = ProjectMetadata(
            id = projectId,
            name = name,
            width = width,
            height = height,
            backgroundColorArgb = backgroundColorArgb,
            backgroundSurface = backgroundSurface,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            gspFilePath = gspPath,
            thumbnailPath = null
        )
        projectDao.upsert(ProjectEntity.fromDomain(metadata))
        return metadata
    }

    override suspend fun loadDocument(projectId: String): ArtworkDocument? {
        return storage.loadDocument(projectId)
    }

    override suspend fun saveDocument(
        document: ArtworkDocument,
        thumbnailPngBytes: ByteArray?
    ) {
        val gspPath = storage.saveDocument(document, thumbnailPngBytes)
        val existing = projectDao.getById(document.projectId)
        val now = System.currentTimeMillis()
        val thumbPath = if (thumbnailPngBytes != null) {
            storage.thumbnailFileFor(document.projectId).absolutePath
        } else {
            existing?.thumbnailPath
        }

        val entity = ProjectEntity(
            id = document.projectId,
            name = document.name,
            width = document.width,
            height = document.height,
            backgroundColorArgb = document.backgroundColorArgb,
            backgroundSurface = document.backgroundSurface.name,
            createdAtEpochMs = existing?.createdAtEpochMs ?: now,
            updatedAtEpochMs = now,
            gspFilePath = gspPath,
            thumbnailPath = thumbPath
        )
        projectDao.upsert(entity)
    }

    override suspend fun autosaveDocument(document: ArtworkDocument) {
        storage.saveAutosave(document)
    }

    override suspend fun renameProject(projectId: String, newName: String) {
        // Fail before touching anything when the project is already inconsistent, so a
        // rename cannot silently leave the Room row and the .gsp payload out of sync.
        projectDao.getById(projectId)
            ?: error("Project $projectId does not exist")
        val doc = storage.loadDocument(projectId)
            ?: error("Project data for $projectId is missing")

        // Write the payload first: if it fails, the metadata keeps the old name and the
        // two stores stay consistent.
        storage.saveDocument(doc.copy(name = newName), thumbnailPngBytes = null)
        projectDao.rename(projectId, newName, System.currentTimeMillis())
    }

    override suspend fun deleteProject(projectId: String) {
        projectDao.deleteById(projectId)
        storage.deleteProjectFiles(projectId)
    }
}
