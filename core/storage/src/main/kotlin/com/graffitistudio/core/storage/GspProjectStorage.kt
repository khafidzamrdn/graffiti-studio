package com.graffitistudio.core.storage

import android.content.Context
import android.util.Log
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
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
            if (!replaceFile(tmpThumb, thumbFile)) {
                Log.w(TAG, "Could not replace thumbnail ${thumbFile.name}")
            }
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

        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(tempFile))).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(payload)
                zip.closeEntry()
            }
        } catch (error: Throwable) {
            tempFile.delete()
            throw error
        }

        if (!replaceFile(tempFile, target)) {
            tempFile.delete()
            error("Could not write ${target.name}")
        }
    }

    /**
     * Replaces [target] with [source] in one step where the filesystem supports it.
     * The previous target is never deleted before the new file is ready, so a crash
     * mid-write cannot leave the project without a file.
     */
    private fun replaceFile(source: File, target: File): Boolean {
        return try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
            true
        } catch (error: Throwable) {
            Log.w(TAG, "Atomic replace failed for ${target.name}, falling back", error)
            runCatching {
                source.copyTo(target, overwrite = true)
                source.delete()
            }.isSuccess
        }
    }

    private fun readGsp(file: File): ArtworkDocument? {
        return runCatching {
            ZipInputStream(BufferedInputStream(FileInputStream(file))).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == MANIFEST_ENTRY) {
                        val content = zip.readBytes().toString(Charsets.UTF_8)
                        val manifest = json.decodeFromString<GspManifest>(content)
                        if (manifest.formatVersion > GspManifest.CURRENT_FORMAT_VERSION) {
                            Log.w(
                                TAG,
                                "Skipping ${file.name}: format ${manifest.formatVersion} is newer " +
                                    "than supported ${GspManifest.CURRENT_FORMAT_VERSION}"
                            )
                            return@runCatching null
                        }
                        return@runCatching manifest.document
                    }
                    entry = zip.nextEntry
                }
                Log.w(TAG, "Missing $MANIFEST_ENTRY in ${file.name}")
                null
            }
        }.onFailure { error ->
            Log.w(TAG, "Failed to read ${file.name}", error)
        }.getOrNull()
    }

    companion object {
        private const val TAG = "GspProjectStorage"
        private const val MANIFEST_ENTRY = "manifest.json"
    }
}
