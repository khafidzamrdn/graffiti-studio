package com.graffitistudio.feature.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.graffitistudio.core.domain.model.ExportConfig
import com.graffitistudio.core.domain.model.ExportFormat
import com.graffitistudio.core.domain.model.ExportScale

@Composable
fun ExportDialog(
    docWidth: Int,
    docHeight: Int,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onExport: (ExportConfig) -> Unit
) {
    val colors = LocalUrbanColors.current
    var format by remember { mutableStateOf(ExportFormat.PNG) }
    var scale by remember { mutableStateOf(ExportScale.ORIGINAL) }
    var transparentBg by remember { mutableStateOf(false) }

    val config = ExportConfig(
        format = format,
        scale = scale,
        transparentBackground = transparentBg
    )
    val (outW, outH) = config.resolveDimensions(docWidth, docHeight)

    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        UrbanPanel {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "EXPORT ARTWORK",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FORMAT",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportFormat.entries.forEach { item ->
                            val selected = format == item
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) colors.surfaceHighlight else colors.surfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) colors.accentLime else colors.borderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { format = item }
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selected) colors.accentLime else colors.textPrimary,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp)
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "RESOLUTION",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "${outW}×${outH}px",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accentCyan
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportScale.entries.forEach { item ->
                            val selected = scale == item
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) colors.surfaceHighlight else colors.surfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) colors.accentCyan else colors.borderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { scale = item }
                            ) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) colors.accentCyan else colors.textPrimary,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Transparent Background",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (format == ExportFormat.PNG) colors.textPrimary else colors.textMuted
                        )
                        Text(
                            text = if (format == ExportFormat.PNG) {
                                "Omit base wall color"
                            } else {
                                "Available for PNG only"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                    Switch(
                        checked = format == ExportFormat.PNG && transparentBg,
                        onCheckedChange = { transparentBg = it },
                        enabled = format == ExportFormat.PNG,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onAccent,
                            checkedTrackColor = colors.accentLime
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isExporting
                    ) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                    UrbanPrimaryButton(
                        onClick = { onExport(config) },
                        enabled = !isExporting
                    ) {
                        Text(if (isExporting) "Exporting..." else "Export")
                    }
                }
            }
        }
    }
}
