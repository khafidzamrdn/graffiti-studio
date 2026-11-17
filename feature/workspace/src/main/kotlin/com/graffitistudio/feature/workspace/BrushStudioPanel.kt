package com.graffitistudio.feature.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.graffitistudio.core.designsystem.component.UrbanPanel
import com.graffitistudio.core.designsystem.component.UrbanPrimaryButton
import com.graffitistudio.core.designsystem.theme.LocalUrbanColors
import com.graffitistudio.core.domain.model.BrushConfig
import com.graffitistudio.core.domain.model.BrushPreset
import com.graffitistudio.core.domain.model.BrushType
import kotlin.math.roundToInt

@Composable
fun BrushStudioPanel(
    activeBrush: BrushConfig,
    presets: List<BrushPreset>,
    activePresetId: String?,
    onSelectBrushType: (BrushType) -> Unit,
    onSelectPreset: (BrushPreset) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onFlowChange: (Float) -> Unit,
    onStabilizerChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalUrbanColors.current
    var showSaveDialog by remember { mutableStateOf(false) }

    UrbanPanel(
        modifier = modifier.fillMaxWidth(),
        containerColor = colors.surfacePrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "BRUSH ENGINE",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BrushType.entries.forEach { type ->
                    val selected = activeBrush.type == type
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) colors.accentLime else colors.surfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (selected) colors.accentLime else colors.borderSubtle
                        ),
                        modifier = Modifier.clickable { onSelectBrushType(type) }
                    ) {
                        Text(
                            text = type.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) colors.onAccent else colors.textPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRESETS",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.clickable { showSaveDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = colors.accentCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save Preset",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accentCyan
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { preset ->
                    val selected = preset.id == activePresetId
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) colors.surfaceHighlight else colors.surfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (selected) colors.accentCyan else colors.borderSubtle
                        ),
                        modifier = Modifier.clickable { onSelectPreset(preset) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) colors.accentCyan else colors.textPrimary
                            )
                            if (preset.isCustom) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete preset",
                                    tint = colors.textMuted,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onDeleteCustomPreset(preset.id) }
                                )
                            }
                        }
                    }
                }
            }

            DynamicSliderRow(
                label = "Opacity",
                value = activeBrush.opacity,
                valueRange = 0.05f..1f,
                percentText = "${(activeBrush.opacity * 100).roundToInt()}%",
                onValueChange = onOpacityChange
            )
            DynamicSliderRow(
                label = "Flow",
                value = activeBrush.flow,
                valueRange = 0.1f..1f,
                percentText = "${(activeBrush.flow * 100).roundToInt()}%",
                onValueChange = onFlowChange
            )
            DynamicSliderRow(
                label = "Stabilizer",
                value = activeBrush.stabilizerLevel,
                valueRange = 0f..0.9f,
                percentText = "${(activeBrush.stabilizerLevel / 0.9f * 100).roundToInt()}%",
                onValueChange = onStabilizerChange
            )
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            defaultName = "${activeBrush.type.displayName} Custom",
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                onSaveCustomPreset(name)
                showSaveDialog = false
            }
        )
    }
}

@Composable
private fun DynamicSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    percentText: String,
    onValueChange: (Float) -> Unit
) {
    val colors = LocalUrbanColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            modifier = Modifier.width(68.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = colors.accentCyan,
                activeTrackColor = colors.accentCyan,
                inactiveTrackColor = colors.surfaceHighlight
            )
        )
        Text(
            text = percentText,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
            modifier = Modifier.width(38.dp)
        )
    }
}

@Composable
private fun SavePresetDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val colors = LocalUrbanColors.current
    var name by remember { mutableStateOf(defaultName) }

    Dialog(onDismissRequest = onDismiss) {
        UrbanPanel {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SAVE BRUSH PRESET",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Preset Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentLime,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                    UrbanPrimaryButton(onClick = { onSave(name) }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
