package com.graffitistudio.core.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.graffitistudio.core.domain.model.ArtworkDocument
import com.graffitistudio.core.domain.model.BrushConfig
import com.graffitistudio.core.domain.model.GraffitiTextElement
import com.graffitistudio.core.domain.model.LayerData
import com.graffitistudio.core.domain.model.StrokeData
import com.graffitistudio.core.domain.model.StrokePoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GspProjectStorageTest {

    private lateinit var context: Context
    private lateinit var storage: GspProjectStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.filesDir.deleteRecursively()
        context.filesDir.mkdirs()
        storage = GspProjectStorage(context)
    }

    private fun document(name: String = "Wall"): ArtworkDocument = ArtworkDocument(
        projectId = PROJECT_ID,
        name = name,
        width = 1080,
        height = 1080,
        layers = listOf(
            LayerData(
                id = "layer-1",
                name = "Layer 1",
                strokes = listOf(
                    StrokeData(
                        id = "stroke-1",
                        layerId = "layer-1",
                        brush = BrushConfig(),
                        points = listOf(StrokePoint(10f, 20f), StrokePoint(30f, 40f))
                    )
                ),
                textElements = listOf(
                    GraffitiTextElement(
                        id = "text-1",
                        layerId = "layer-1",
                        text = "GRAFFITI",
                        x = 120f,
                        y = 240f
                    )
                )
            )
        )
    )

    @Test
    fun `save and load round trips document`() = runTest {
        storage.saveDocument(document())

        val loaded = storage.loadDocument(PROJECT_ID)

        assertEquals("Wall", loaded?.name)
        assertEquals(1080, loaded?.width)
        assertEquals(1, loaded?.layers?.size)
        assertEquals(1, loaded?.layers?.first()?.strokes?.size)
        assertEquals("GRAFFITI", loaded?.layers?.first()?.textElements?.first()?.text)
    }

    @Test
    fun `saving twice replaces the previous file without leaving temp files`() = runTest {
        storage.saveDocument(document("First"))
        storage.saveDocument(document("Second"))

        assertEquals("Second", storage.loadDocument(PROJECT_ID)?.name)
        val tempFiles = File(context.filesDir, "projects").listFiles()
            ?.filter { it.name.endsWith(".tmp") }
            .orEmpty()
        assertTrue(tempFiles.isEmpty())
    }

    @Test
    fun `newer autosave wins over the primary file`() = runTest {
        storage.saveDocument(document("Primary"))
        storage.saveAutosave(document("Autosave"))
        // Make the ordering deterministic: autosave must be strictly newer.
        storage.gspFileFor(PROJECT_ID).setLastModified(1_000L)

        assertEquals("Autosave", storage.loadDocument(PROJECT_ID)?.name)
    }

    @Test
    fun `corrupt primary file is reported as missing instead of crashing`() = runTest {
        storage.gspFileFor(PROJECT_ID).writeText("this is not a zip archive")

        assertNull(storage.loadDocument(PROJECT_ID))
    }

    @Test
    fun `project written by a newer format version is rejected`() = runTest {
        writeRawGsp(
            storage.gspFileFor(PROJECT_ID),
            """{"formatVersion":99,"savedAtEpochMs":0,"document":{"projectId":"$PROJECT_ID","name":"Future","width":10,"height":10}}"""
        )

        assertNull(storage.loadDocument(PROJECT_ID))
    }

    @Test
    fun `delete removes project, thumbnail and autosave files`() = runTest {
        storage.saveDocument(document(), thumbnailPngBytes = byteArrayOf(1, 2, 3))
        storage.saveAutosave(document())

        storage.deleteProjectFiles(PROJECT_ID)

        assertFalse(storage.gspFileFor(PROJECT_ID).exists())
        assertFalse(storage.thumbnailFileFor(PROJECT_ID).exists())
        assertNull(storage.loadDocument(PROJECT_ID))
    }

    private fun writeRawGsp(file: File, manifestJson: String) {
        file.parentFile?.mkdirs()
        ZipOutputStream(BufferedOutputStream(FileOutputStream(file))).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifestJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    private companion object {
        const val PROJECT_ID = "project-1"
    }
}
