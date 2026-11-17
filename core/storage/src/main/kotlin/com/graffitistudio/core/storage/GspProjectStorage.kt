package com.graffitistudio.core.storage

import android.content.Context
import com.graffitistudio.core.domain.model.ArtworkDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
private data class GspManifest(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val savedAtEpochMs: Long,
    val document: ArtworkDocument
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

/**
 * Reads and writes Graffiti Studio's native `.gsp` (ZIP-packaged) project files
 * using atomic temp-file replacement and autosave recovery files.
 */
class GspProjectStorage(context: Context) {

    private val projectsDir = File(context.filesDir, "projects").apply { mkdirs() }
    private val thumbnailsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
    private val autosaveDir = File(context.filesDir, "autosave").apply { mkdirs() }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun gspFileFor(projectId: String): File = File(projectsDir, "$projectId.gsp")

    fun thumbnailFileFor(projectId: String): File = File(thumbnailsDir, "$projectId.png")

    private fun autosaveFileFor(projectId: String): File = File(autosaveDir, "$projectId.autosave.gsp")

    suspend fun saveDocument(
        document: ArtworkDocument,
        thumbnailPngBytes: ByteArray? = null
    ): String = withContext(Dispatchers.IO) {
        val target = gspFileFor(document.projectId)
        writeGspAtomically(target, document)
        autosaveFileFor(document.projectId).delete()

        if (thumbnailPngBytes != null) {
            val thumbFile = thumbnailFileFor(document.projectId)
            val tmpThumb = File(thumbnailsDir, "${document.projectId}.png.tmp")
            FileOutputStream(tmpThumb).use { it.write(thumbnailPngBytes) }
            if (thumbFile.exists()) thumbFile.delete()
            tmpThumb.renameTo(thumbFile)
        }
        target.absolutePath
    }

    suspend fun saveAutosave(document: ArtworkDocument) = withContext(Dispatchers.IO) {
        writeGspAtomically(autosaveFileFor(document.projectId), document)
    }

    suspend fun loadDocument(projectId: String): ArtworkDocument? = withContext(Dispatchers.IO) {
        val autosave = autosaveFileFor(projectId)
        val primary = gspFileFor(projectId)

        val candidate = when {
            autosave.exists() && (!primary.exists() || autosave.lastModified() > primary.lastModified()) -> autosave
            primary.exists() -> primary
            else -> return@withContext null
        }

        readGsp(candidate) ?: if (candidate === autosave && primary.exists()) readGsp(primary) else null
    }

    suspend fun deleteProjectFiles(projectId: String) = withContext(Dispatchers.IO) {
        gspFileFor(projectId).delete()
        thumbnailFileFor(projectId).delete()
        autosaveFileFor(projectId).delete()
        Unit
    }

    private fun writeGspAtomically(target: File, document: ArtworkDocument) {
        val tempFile = File(target.parentFile, "${target.name}.tmp")
        val manifest = GspManifest(
            savedAtEpochMs = System.currentTimeMillis(),
            document = document
        )
        val payload = json.encodeToString(manifest).toByteArray(Charsets.UTF_8)

        ZipOutputStream(BufferedOutputStream(FileOutputStream(tempFile))).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(payload)
            zip.closeEntry()
        }

        if (target.exists()) {
            target.delete()
        }
        if (!tempFile.renameTo(target)) {
            tempFile.copyTo(target, overwrite = true)
            tempFile.delete()
        }
    }

    private fun readGsp(file: File): ArtworkDocument? {
        return runCatching {
            ZipInputStream(BufferedInputStream(FileInputStream(file))).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == MANIFEST_ENTRY) {
                        val content = zip.readBytes().toString(Charsets.UTF_8)
                        return@runCatching json.decodeFromString<GspManifest>(content).document
                    }
                    entry = zip.nextEntry
                }
                null
            }
        }.getOrNull()
    }

    companion object {
        private const val MANIFEST_ENTRY = "manifest.json"
    }
}
