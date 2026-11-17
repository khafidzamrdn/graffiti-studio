package com.graffitistudio

import android.content.Context
import com.graffitistudio.core.data.DataFactory
import com.graffitistudio.core.data.preferences.WorkspacePreferences
import com.graffitistudio.core.domain.repository.ProjectRepository

class AppContainer(context: Context) {
    val workspacePreferences: WorkspacePreferences by lazy {
        DataFactory.createWorkspacePreferences(context)
    }

    val projectRepository: ProjectRepository by lazy {
        DataFactory.createProjectRepository(context)
    }
}
