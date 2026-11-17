package com.graffitistudio.feature.workspace

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import kotlin.math.roundToInt

@Composable
fun ColorPickerDialog(
    initialColorArgb: Int,
    onDismiss: () -> Unit,
    onSelectColor: (Int) -> Unit
) {
    val colors = LocalUrbanColors.current
    val initialHsv = remember(initialColorArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialColorArgb, it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    val selectedArgb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
    val hexString = String.format("#%06X", 0xFFFFFF and selectedArgb)

    Dialog(onDismissRequest = onDismiss) {
        UrbanPanel {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COLOR PICKER",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = hexString,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accentCyan
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(selectedArgb))
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
                )

                HsvSliderRow("Hue", hue, 0f..360f, "${hue.roundToInt()}°") { hue = it }
                HsvSliderRow("Sat", saturation, 0f..1f, "${(saturation * 100).roundToInt()}%") {
                    saturation = it
                }
                HsvSliderRow("Bright", value, 0f..1f, "${(value * 100).roundToInt()}%") {
                    value = it
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                    UrbanPrimaryButton(onClick = { onSelectColor(selectedArgb) }) {
                        Text("Use Color")
                    }
                }
            }
        }
    }
}

@Composable
private fun HsvSliderRow(
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
            modifier = Modifier.width(48.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = colors.accentLime,
                activeTrackColor = colors.accentLime,
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
