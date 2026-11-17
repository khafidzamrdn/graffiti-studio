package com.graffitistudio.core.data

import android.content.Context
import com.graffitistudio.core.data.local.GraffitiDatabase
import com.graffitistudio.core.data.preferences.WorkspacePreferences
import com.graffitistudio.core.data.repository.ProjectRepositoryImpl
import com.graffitistudio.core.domain.repository.ProjectRepository
import com.graffitistudio.core.storage.ArtworkExporter
import com.graffitistudio.core.storage.GspProjectStorage

object DataFactory {
    fun createProjectRepository(context: Context): ProjectRepository {
        val appContext = context.applicationContext
        val database = GraffitiDatabase.create(appContext)
        val storage = GspProjectStorage(appContext)
        return ProjectRepositoryImpl(
            projectDao = database.projectDao(),
            storage = storage
        )
    }

    fun createWorkspacePreferences(context: Context): WorkspacePreferences {
        return WorkspacePreferences(context.applicationContext)
    }

    fun createArtworkExporter(context: Context): ArtworkExporter {
        return ArtworkExporter(context.applicationContext)
    }
}
