package com.graffitistudio.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.graffitistudio.core.designsystem.component.UrbanPanel
import com.graffitistudio.core.designsystem.component.UrbanPrimaryButton
import com.graffitistudio.core.designsystem.theme.LocalUrbanColors
import com.graffitistudio.core.designsystem.theme.UrbanPalette
import com.graffitistudio.core.domain.model.CanvasPreset

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, preset: CanvasPreset, backgroundColorArgb: Int) -> Unit
) {
    val colors = LocalUrbanColors.current
    var projectName by remember { mutableStateOf("Wall Piece 01") }
    var selectedPreset by remember { mutableStateOf(CanvasPreset.defaults.first()) }
    var selectedBg by remember { mutableIntStateOf(UrbanPalette.canvasBackgrounds.first()) }

    Dialog(onDismissRequest = onDismiss) {
        UrbanPanel(
            containerColor = colors.surfacePrimary,
            borderColor = colors.borderSubtle
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "NEW ARTWORK",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )

                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentLime,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedLabelColor = colors.accentLime,
                        unfocusedLabelColor = colors.textSecondary,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CANVAS SIZE",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CanvasPreset.defaults.forEach { preset ->
                            val selected = preset == selectedPreset
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) colors.surfaceHighlight else colors.surfaceElevated,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selected) colors.accentLime else colors.borderSubtle
                                ),
                                modifier = Modifier.clickable { selectedPreset = preset }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (selected) colors.accentLime else colors.textPrimary
                                    )
                                    Text(
                                        text = "${preset.width}×${preset.height}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "BASE SURFACE",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        UrbanPalette.canvasBackgrounds.forEach { argb ->
                            val selected = argb == selectedBg
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) colors.accentLime else colors.borderSubtle,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedBg = argb }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                    UrbanPrimaryButton(
                        onClick = { onCreate(projectName, selectedPreset, selectedBg) }
                    ) {
                        Text("Create Canvas")
                    }
                }
            }
        }
    }
}
