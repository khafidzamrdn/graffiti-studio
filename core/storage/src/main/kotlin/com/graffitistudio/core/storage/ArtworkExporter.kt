package com.graffitistudio.core.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.graffitistudio.core.domain.model.ExportConfig
import com.graffitistudio.core.domain.model.ExportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ExportResult(
    val displayName: String,
    val locationLabel: String
)

class ArtworkExporter(private val context: Context) {

    suspend fun exportBitmap(
        bitmap: Bitmap,
        projectName: String,
        config: ExportConfig
    ): ExportResult = withContext(Dispatchers.IO) {
        val safeName = projectName
            .trim()
            .replace(Regex("[^A-Za-z0-9_\\-]"), "_")
            .ifEmpty { "graffiti_artwork" }
        val timestamp = System.currentTimeMillis()
        val fileName = "${safeName}_$timestamp.${config.format.extension}"

        val compressFormat = when (config.format) {
            ExportFormat.PNG -> Bitmap.CompressFormat.PNG
            ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
        }
        val quality = if (config.format == ExportFormat.PNG) 100 else config.jpegQuality

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, config.format.mimeType)
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/GraffitiStudio"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("Failed to create MediaStore entry")

                resolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(compressFormat, quality, stream)
                } ?: error("Failed to open output stream")

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                ExportResult(
                    displayName = fileName,
                    locationLabel = "Pictures/GraffitiStudio/$fileName"
                )
            } else {
                val dir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "GraffitiStudio"
                ).apply { mkdirs() }
                val outFile = File(dir, fileName)
                FileOutputStream(outFile).use { stream ->
                    bitmap.compress(compressFormat, quality, stream)
                }
                ExportResult(
                    displayName = fileName,
                    locationLabel = outFile.absolutePath
                )
            }
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}
