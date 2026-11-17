package com.graffitistudio.feature.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.graffitistudio.core.domain.model.EffectType
import com.graffitistudio.core.domain.model.LayerEffect
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EffectsStudioDialog(
    activeLayerId: String,
    activeEffects: List<LayerEffect>,
    onDismiss: () -> Unit,
    onApplyEffect: (layerId: String, LayerEffect) -> Unit,
    onRemoveEffect: (layerId: String, effectId: String) -> Unit
) {
    val colors = LocalUrbanColors.current
    var selectedType by remember { mutableStateOf(EffectType.GLOW) }
    var intensity by remember { mutableFloatStateOf(0.65f) }
    var radiusPx by remember { mutableFloatStateOf(20f) }
    var effectColor by remember { mutableIntStateOf(UrbanPalette.studioSwatches[0]) }

    Dialog(onDismissRequest = onDismiss) {
        UrbanPanel {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "EFFECTS STUDIO",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )

                if (activeEffects.isNotEmpty()) {
                    Text(
                        text = "ACTIVE ON LAYER",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeEffects.forEach { fx ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.surfaceHighlight,
                                border = BorderStroke(1.dp, colors.borderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = fx.type.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.accentCyan
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove effect",
                                        tint = colors.textMuted,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onRemoveEffect(activeLayerId, fx.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "SELECT EFFECT",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EffectType.entries.forEach { type ->
                        val selected = selectedType == type
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) colors.accentLime else colors.surfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selected) colors.accentLime else colors.borderSubtle
                            ),
                            modifier = Modifier.clickable { selectedType = type }
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

                EffectSliderRow(
                    label = "Intensity",
                    value = intensity,
                    range = 0.1f..1f,
                    readout = "${(intensity * 100).roundToInt()}%",
                    onValueChange = { intensity = it }
                )

                EffectSliderRow(
                    label = "Radius",
                    value = radiusPx,
                    range = 6f..48f,
                    readout = "${radiusPx.roundToInt()}px",
                    onValueChange = { radiusPx = it }
                )

                if (selectedType == EffectType.SPRAY_TEXTURE ||
                    selectedType == EffectType.PAINT_SPLATTER ||
                    selectedType == EffectType.GLOW
                ) {
                    Text(
                        text = "EFFECT COLOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UrbanPalette.studioSwatches.forEach { argb ->
                            val selected = effectColor == argb
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) colors.accentLime else colors.borderSubtle,
                                        shape = CircleShape
                                    )
                                    .clickable { effectColor = argb }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = colors.textSecondary)
                    }
                    UrbanPrimaryButton(
                        onClick = {
                            onApplyEffect(
                                activeLayerId,
                                LayerEffect(
                                    id = UUID.randomUUID().toString(),
                                    type = selectedType,
                                    intensity = intensity,
                                    radiusPx = radiusPx,
                                    colorArgb = effectColor
                                )
                            )
                        }
                    ) {
                        Text("Apply Effect")
                    }
                }
            }
        }
    }
}

@Composable
private fun EffectSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    readout: String,
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
            modifier = Modifier.width(60.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = colors.accentCyan,
                activeTrackColor = colors.accentCyan,
                inactiveTrackColor = colors.surfaceHighlight
            )
        )
        Text(
            text = readout,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
            modifier = Modifier.width(42.dp)
        )
    }
}
