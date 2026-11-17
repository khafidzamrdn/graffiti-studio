package com.graffitistudio

import android.content.Context
import com.graffitistudio.core.data.local.GraffitiDatabase
import com.graffitistudio.core.data.preferences.WorkspacePreferences
import com.graffitistudio.core.data.repository.ProjectRepositoryImpl
import com.graffitistudio.core.domain.repository.ProjectRepository
import com.graffitistudio.core.storage.GspProjectStorage

class AppContainer(context: Context) {
    private val database: GraffitiDatabase by lazy {
        GraffitiDatabase.create(context)
    }

    private val storage: GspProjectStorage by lazy {
        GspProjectStorage(context)
    }

    val workspacePreferences: WorkspacePreferences by lazy {
        WorkspacePreferences(context)
    }

    val projectRepository: ProjectRepository by lazy {
        ProjectRepositoryImpl(
            projectDao = database.projectDao(),
            storage = storage
        )
    }
}
