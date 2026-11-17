package com.graffitistudio.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.graffitistudio.core.domain.model.BrushConfig
import com.graffitistudio.core.domain.model.BrushPreset
import com.graffitistudio.core.domain.model.BrushType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "workspace_prefs")

data class ToolPreferences(
    val brushType: BrushType = BrushType.PEN,
    val brushSizePx: Float = 24f,
    val brushColorArgb: Int = 0xFFB8FF3B.toInt(),
    val brushOpacity: Float = 1f,
    val brushFlow: Float = 0.85f,
    val stabilizerLevel: Float = 0.35f,
    val customPresets: List<BrushPreset> = emptyList()
) {
    fun toBrushConfig(): BrushConfig = BrushConfig(
        type = brushType,
        colorArgb = brushColorArgb,
        sizePx = brushSizePx,
        opacity = brushOpacity,
        flow = brushFlow,
        stabilizerLevel = stabilizerLevel
    )
}

class WorkspacePreferences(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val BRUSH_TYPE = stringPreferencesKey("brush_type")
        val BRUSH_SIZE_PX = floatPreferencesKey("brush_size_px")
        val BRUSH_COLOR_ARGB = intPreferencesKey("brush_color_argb")
        val BRUSH_OPACITY = floatPreferencesKey("brush_opacity")
        val BRUSH_FLOW = floatPreferencesKey("brush_flow")
        val STABILIZER_LEVEL = floatPreferencesKey("stabilizer_level")
        val CUSTOM_PRESETS_JSON = stringPreferencesKey("custom_presets_json")
    }

    val toolPreferences: Flow<ToolPreferences> = context.dataStore.data.map { prefs ->
        val rawType = prefs[Keys.BRUSH_TYPE]
        val brushType = rawType?.let { name ->
            BrushType.entries.firstOrNull { it.name == name }
        } ?: BrushType.PEN

        val presets = prefs[Keys.CUSTOM_PRESETS_JSON]?.let { raw ->
            runCatching { json.decodeFromString<List<BrushPreset>>(raw) }.getOrNull()
        } ?: emptyList()

        ToolPreferences(
            brushType = brushType,
            brushSizePx = prefs[Keys.BRUSH_SIZE_PX] ?: 24f,
            brushColorArgb = prefs[Keys.BRUSH_COLOR_ARGB] ?: 0xFFB8FF3B.toInt(),
            brushOpacity = prefs[Keys.BRUSH_OPACITY] ?: 1f,
            brushFlow = prefs[Keys.BRUSH_FLOW] ?: 0.85f,
            stabilizerLevel = prefs[Keys.STABILIZER_LEVEL] ?: 0.35f,
            customPresets = presets
        )
    }

    suspend fun saveBrushConfig(brush: BrushConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BRUSH_TYPE] = brush.type.name
            prefs[Keys.BRUSH_SIZE_PX] = brush.sizePx
            prefs[Keys.BRUSH_COLOR_ARGB] = brush.colorArgb
            prefs[Keys.BRUSH_OPACITY] = brush.opacity
            prefs[Keys.BRUSH_FLOW] = brush.flow
            prefs[Keys.STABILIZER_LEVEL] = brush.stabilizerLevel
        }
    }

    suspend fun saveCustomPreset(preset: BrushPreset) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_PRESETS_JSON]?.let { raw ->
                runCatching { json.decodeFromString<List<BrushPreset>>(raw) }.getOrNull()
            } ?: emptyList()
            val updated = (current.filterNot { it.id == preset.id } + preset)
            prefs[Keys.CUSTOM_PRESETS_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun deleteCustomPreset(presetId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_PRESETS_JSON]?.let { raw ->
                runCatching { json.decodeFromString<List<BrushPreset>>(raw) }.getOrNull()
            } ?: emptyList()
            val updated = current.filterNot { it.id == presetId }
            prefs[Keys.CUSTOM_PRESETS_JSON] = json.encodeToString(updated)
        }
    }
}
