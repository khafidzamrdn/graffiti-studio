package com.graffitistudio.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.graffitistudio.core.data.local.ProjectDao
import com.graffitistudio.core.data.local.ProjectEntity
import com.graffitistudio.core.domain.model.BackgroundSurfaceType
import com.graffitistudio.core.storage.GspProjectStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProjectRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var storage: GspProjectStorage
    private lateinit var dao: FakeProjectDao
    private lateinit var repository: ProjectRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.filesDir.deleteRecursively()
        context.filesDir.mkdirs()
        storage = GspProjectStorage(context)
        dao = FakeProjectDao()
        repository = ProjectRepositoryImpl(dao, storage)
    }

    @Test
    fun `createProject persists both metadata and payload`() = runTest {
        val created = repository.createProject(
            name = "First Wall",
            width = 1080,
            height = 1080,
            backgroundColorArgb = 0xFF14161C.toInt(),
            backgroundSurface = BackgroundSurfaceType.SOLID,
            initialBackground = null
        )

        assertEquals("First Wall", dao.rows[created.id]?.name)
        assertEquals("First Wall", repository.loadDocument(created.id)?.name)
    }

    @Test
    fun `rename updates metadata and payload together`() = runTest {
        val created = repository.createProject(
            name = "Old Name",
            width = 1080,
            height = 1080,
            backgroundColorArgb = 0xFF14161C.toInt(),
            backgroundSurface = BackgroundSurfaceType.SOLID,
            initialBackground = null
        )

        repository.renameProject(created.id, "New Name")

        assertEquals("New Name", dao.rows[created.id]?.name)
        assertEquals("New Name", repository.loadDocument(created.id)?.name)
    }

    @Test
    fun `rename fails and keeps metadata when the payload is missing`() = runTest {
        val created = repository.createProject(
            name = "Old Name",
            width = 1080,
            height = 1080,
            backgroundColorArgb = 0xFF14161C.toInt(),
            backgroundSurface = BackgroundSurfaceType.SOLID,
            initialBackground = null
        )
        storage.gspFileFor(created.id).delete()

        val result = runCatching { repository.renameProject(created.id, "New Name") }

        assertTrue(result.isFailure)
        assertEquals("Old Name", dao.rows[created.id]?.name)
    }

    @Test
    fun `rename fails when the project does not exist`() = runTest {
        val result = runCatching { repository.renameProject("missing", "New Name") }

        assertTrue(result.isFailure)
    }

    private class FakeProjectDao : ProjectDao {
        val rows = mutableMapOf<String, ProjectEntity>()

        override fun observeAll(): Flow<List<ProjectEntity>> = flowOf(rows.values.toList())

        override suspend fun getById(id: String): ProjectEntity? = rows[id]

        override suspend fun upsert(entity: ProjectEntity) {
            rows[entity.id] = entity
        }

        override suspend fun rename(id: String, newName: String, updatedAt: Long) {
            rows[id]?.let { rows[id] = it.copy(name = newName, updatedAtEpochMs = updatedAt) }
        }

        override suspend fun deleteById(id: String) {
            rows.remove(id)
        }
    }
}
