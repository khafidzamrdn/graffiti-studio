package com.graffitistudio.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "workspace_prefs")

data class ToolPreferences(
    val brushSizePx: Float = 24f,
    val brushColorArgb: Int = 0xFFB8FF3B.toInt()
)

class WorkspacePreferences(private val context: Context) {

    private object Keys {
        val BRUSH_SIZE_PX = floatPreferencesKey("brush_size_px")
        val BRUSH_COLOR_ARGB = intPreferencesKey("brush_color_argb")
    }

    val toolPreferences: Flow<ToolPreferences> = context.dataStore.data.map { prefs ->
        ToolPreferences(
            brushSizePx = prefs[Keys.BRUSH_SIZE_PX] ?: 24f,
            brushColorArgb = prefs[Keys.BRUSH_COLOR_ARGB] ?: 0xFFB8FF3B.toInt()
        )
    }

    suspend fun saveBrushSize(sizePx: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BRUSH_SIZE_PX] = sizePx
        }
    }

    suspend fun saveBrushColor(colorArgb: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BRUSH_COLOR_ARGB] = colorArgb
        }
    }
}
