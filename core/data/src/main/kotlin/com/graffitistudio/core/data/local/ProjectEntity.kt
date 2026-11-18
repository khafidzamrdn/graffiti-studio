package com.graffitistudio.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.graffitistudio.core.domain.model.BackgroundSurfaceType
import com.graffitistudio.core.domain.model.ProjectMetadata

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val backgroundColorArgb: Int,
    val backgroundSurface: String = BackgroundSurfaceType.SOLID.name,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val gspFilePath: String,
    val thumbnailPath: String?
) {
    fun toDomain(): ProjectMetadata = ProjectMetadata(
        id = id,
        name = name,
        width = width,
        height = height,
        backgroundColorArgb = backgroundColorArgb,
        backgroundSurface = BackgroundSurfaceType.entries
            .firstOrNull { it.name == backgroundSurface }
            ?: BackgroundSurfaceType.SOLID,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs,
        gspFilePath = gspFilePath,
        thumbnailPath = thumbnailPath
    )

    companion object {
        fun fromDomain(model: ProjectMetadata): ProjectEntity = ProjectEntity(
            id = model.id,
            name = model.name,
            width = model.width,
            height = model.height,
            backgroundColorArgb = model.backgroundColorArgb,
            backgroundSurface = model.backgroundSurface.name,
            createdAtEpochMs = model.createdAtEpochMs,
            updatedAtEpochMs = model.updatedAtEpochMs,
            gspFilePath = model.gspFilePath,
            thumbnailPath = model.thumbnailPath
        )
    }
}
