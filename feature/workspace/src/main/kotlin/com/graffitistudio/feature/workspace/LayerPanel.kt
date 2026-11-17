package com.graffitistudio.feature.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.graffitistudio.core.designsystem.component.UrbanPanel
import com.graffitistudio.core.designsystem.component.UrbanPrimaryButton
import com.graffitistudio.core.designsystem.theme.LocalUrbanColors
import com.graffitistudio.core.domain.model.BlendMode
import com.graffitistudio.core.domain.model.LayerData
import kotlin.math.roundToInt

@Composable
fun LayerPanel(
    layers: List<LayerData>,
    activeLayerId: String?,
    onSelectLayer: (String) -> Unit,
    onAddLayer: () -> Unit,
    onDeleteLayer: (String) -> Unit,
    onRenameLayer: (String, String) -> Unit,
    onToggleVisibility: (String, Boolean) -> Unit,
    onOpacityChange: (String, Float) -> Unit,
    onBlendModeChange: (String, BlendMode) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onMergeDown: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalUrbanColors.current
    val activeLayer = layers.firstOrNull { it.id == activeLayerId }
    val displayLayers = remember(layers) { layers.asReversed() }
    var renamingLayer by remember { mutableStateOf<LayerData?>(null) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LAYERS (${layers.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.accentLime,
                    modifier = Modifier.clickable { onAddLayer() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Layer",
                            tint = colors.onAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onAccent
                        )
                    }
                }
            }

            if (activeLayer != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Opacity",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        modifier = Modifier.width(52.dp)
                    )
                    Slider(
                        value = activeLayer.opacity,
                        onValueChange = { onOpacityChange(activeLayer.id, it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accentCyan,
                            activeTrackColor = colors.accentCyan,
                            inactiveTrackColor = colors.surfaceHighlight
                        )
                    )
                    Text(
                        text = "${(activeLayer.opacity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        modifier = Modifier.width(38.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BlendMode.entries.forEach { mode ->
                        val selected = activeLayer.blendMode == mode
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (selected) colors.surfaceHighlight else colors.surfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selected) colors.accentCyan else colors.borderSubtle
                            ),
                            modifier = Modifier.clickable { onBlendModeChange(activeLayer.id, mode) }
                        ) {
                            Text(
                                text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) colors.accentCyan else colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(displayLayers, key = { _, item -> item.id }) { revIndex, layer ->
                    val isSelected = layer.id == activeLayerId
                    val isTop = revIndex == 0
                    val isBottom = revIndex == displayLayers.lastIndex

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) colors.surfaceHighlight else colors.surfaceElevated,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) colors.accentLime else colors.borderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLayer(layer.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onToggleVisibility(layer.id, !layer.isVisible) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (layer.isVisible) {
                                        Icons.Default.Visibility
                                    } else {
                                        Icons.Default.VisibilityOff
                                    },
                                    contentDescription = "Toggle visibility",
                                    tint = if (layer.isVisible) colors.accentLime else colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = layer.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${layer.blendMode.name} · ${(layer.opacity * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }

                            IconButton(
                                onClick = { onMoveUp(layer.id) },
                                enabled = !isTop,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = "Move Up",
                                    tint = if (!isTop) colors.textSecondary else colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onMoveDown(layer.id) },
                                enabled = !isBottom,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = "Move Down",
                                    tint = if (!isBottom) colors.textSecondary else colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDuplicate(layer.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Duplicate",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            IconButton(
                                onClick = { onMergeDown(layer.id) },
                                enabled = !isBottom,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.MergeType,
                                    contentDescription = "Merge Down",
                                    tint = if (!isBottom) colors.textSecondary else colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { renamingLayer = layer },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Rename",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteLayer(layer.id) },
                                enabled = layers.size > 1,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = if (layers.size > 1) colors.accentCoral else colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    renamingLayer?.let { target ->
        RenameLayerDialog(
            initialName = target.name,
            onDismiss = { renamingLayer = null },
            onConfirm = { newName ->
                onRenameLayer(target.id, newName)
                renamingLayer = null
            }
        )
    }
}

@Composable
private fun RenameLayerDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = LocalUrbanColors.current
    var text by remember { mutableStateOf(initialName) }

    Dialog(onDismissRequest = onDismiss) {
        UrbanPanel {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "RENAME LAYER",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
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
                    UrbanPrimaryButton(onClick = { onConfirm(text) }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
