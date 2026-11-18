package com.graffitistudio.feature.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.graffitistudio.core.domain.model.GraffitiTextElement
import com.graffitistudio.core.domain.model.GraffitiTextStyle
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun TypographyDialog(
    activeLayerId: String,
    existingElements: List<GraffitiTextElement>,
    docWidth: Int,
    docHeight: Int,
    onDismiss: () -> Unit,
    onApply: (GraffitiTextElement) -> Unit,
    onDelete: (layerId: String, elementId: String) -> Unit
) {
    val colors = LocalUrbanColors.current

    data class FontFamilyItem(val resourceName: String, val displayName: String)

    // Font families bundled from device (.ttf / .otf) under res/font/
    val availableFonts = remember {
        listOf(
            FontFamilyItem("sans-serif", "System Sans"),
            FontFamilyItem("serif", "System Serif"),
            FontFamilyItem("monospace", "System Monospace"),
            FontFamilyItem("cairo_regular", "Cairo Regular"),
            FontFamilyItem("cairo_bold", "Cairo Bold"),
            FontFamilyItem("cairo_black", "Cairo Black"),
            FontFamilyItem("cairo_light", "Cairo Light"),
            FontFamilyItem("cairo_medium", "Cairo Medium"),
            FontFamilyItem("cairo_semibold", "Cairo SemiBold"),
            FontFamilyItem("cairo_extrabold", "Cairo ExtraBold"),
            FontFamilyItem("cairo_extralight", "Cairo ExtraLight"),
            FontFamilyItem("poppins_regular", "Poppins Regular"),
            FontFamilyItem("poppins_bold", "Poppins Bold"),
            FontFamilyItem("poppins_black", "Poppins Black"),
            FontFamilyItem("poppins_light", "Poppins Light"),
            FontFamilyItem("poppins_medium", "Poppins Medium"),
            FontFamilyItem("poppins_semibold", "Poppins SemiBold"),
            FontFamilyItem("poppins_extrabold", "Poppins ExtraBold"),
            FontFamilyItem("poppins_extralight", "Poppins ExtraLight"),
            FontFamilyItem("poppins_thin", "Poppins Thin"),
            FontFamilyItem("poppins_italic", "Poppins Italic"),
            FontFamilyItem("poppins_bolditalic", "Poppins BoldItalic"),
            FontFamilyItem("poppins_blackitalic", "Poppins BlackItalic"),
            FontFamilyItem("poppins_lightitalic", "Poppins LightItalic"),
            FontFamilyItem("poppins_mediumitalic", "Poppins MediumItalic"),
            FontFamilyItem("poppins_semibolditalic", "Poppins SemiBoldItalic"),
            FontFamilyItem("poppins_extrabolditalic", "Poppins ExtraBoldItalic"),
            FontFamilyItem("poppins_extralightitalic", "Poppins ExtraLightItalic"),
            FontFamilyItem("poppins_thinitalic", "Poppins ThinItalic"),
            FontFamilyItem("docallisme_on_street", "Docallisme On Street"),
            FontFamilyItem("docallismeonstreet", "Docallisme On Street (OTF)"),
            FontFamilyItem("click_cheese", "Click Cheese"),
            FontFamilyItem("graffiti", "Graffiti"),
            FontFamilyItem("graffitixenoa_regular", "Graffiti Xenoa Regular"),
            FontFamilyItem("graffitixenoa_shadow", "Graffiti Xenoa Shadow"),
            FontFamilyItem("simpsonfont_demo", "Simpsonfont Demo"),
            FontFamilyItem("inter_regular", "Inter Regular"),
            FontFamilyItem("inter_bold", "Inter Bold"),
            FontFamilyItem("inter_medium", "Inter Medium"),
            FontFamilyItem("inter_semi_bold", "Inter SemiBold")
        )
    }

    var editingId by remember { mutableStateOf<String?>(null) }
    var text by remember { mutableStateOf("STREET") }
    var style by remember { mutableStateOf(GraffitiTextStyle.BUBBLE) }
    var fontSizePx by remember { mutableFloatStateOf(160f) }
    var outlineWidthPx by remember { mutableFloatStateOf(16f) }
    var extrusionDepthPx by remember { mutableFloatStateOf(18f) }
    var shadowEnabled by remember { mutableStateOf(true) }
    var useGradientFill by remember { mutableStateOf(true) }
    var primaryColor by remember { mutableIntStateOf(UrbanPalette.studioSwatches[0]) }
    var secondaryColor by remember { mutableIntStateOf(UrbanPalette.studioSwatches[1]) }
    var posX by remember { mutableFloatStateOf(docWidth * 0.5f) }
    var posY by remember { mutableFloatStateOf(docHeight * 0.55f) }
    var fontFamily by remember { mutableStateOf("sans-serif") }
    var letterSpacing by remember { mutableFloatStateOf(0.06f) }

    fun loadElement(el: GraffitiTextElement) {
        editingId = el.id
        text = el.text
        style = el.style
        fontSizePx = el.fontSizePx
        fontFamily = el.fontFamily
        letterSpacing = el.letterSpacing
        outlineWidthPx = el.outlineWidthPx
        extrusionDepthPx = el.extrusionDepthPx
        shadowEnabled = el.shadowEnabled
        useGradientFill = el.useGradientFill
        primaryColor = el.primaryColorArgb
        secondaryColor = el.secondaryColorArgb
        posX = el.x
        posY = el.y
    }

    Dialog(onDismissRequest = onDismiss) {
        UrbanPanel {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "GRAFFITI TYPOGRAPHY",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )

                if (existingElements.isNotEmpty()) {
                    Text(
                        text = "EDIT EXISTING ON LAYER",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (editingId == null) colors.accentLime else colors.surfaceElevated,
                            modifier = Modifier.clickable {
                                editingId = null
                                text = "STREET"
                                posX = docWidth * 0.5f
                                posY = docHeight * 0.55f
                            }
                        ) {
                            Text(
                                text = "+ New",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (editingId == null) colors.onAccent else colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        existingElements.forEach { el ->
                            val selected = editingId == el.id
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selected) colors.surfaceHighlight else colors.surfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) colors.accentCyan else colors.borderSubtle
                                ),
                                modifier = Modifier.clickable { loadElement(el) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = el.text,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) colors.accentCyan else colors.textPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete text",
                                        tint = colors.textMuted,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                onDelete(activeLayerId, el.id)
                                                if (editingId == el.id) editingId = null
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Tag / Piece Text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentLime,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )

                Text(
                    text = "LETTERING STYLE",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GraffitiTextStyle.entries.forEach { item ->
                        val selected = style == item
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) colors.accentLime else colors.surfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selected) colors.accentLime else colors.borderSubtle
                            ),
                            modifier = Modifier.clickable { style = item }
                        ) {
                            Text(
                                text = item.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) colors.onAccent else colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                TypographySlider("Size", fontSizePx, 40f..360f, "${fontSizePx.roundToInt()}px") {
                    fontSizePx = it
                }
                TypographySlider("Outline", outlineWidthPx, 0f..40f, "${outlineWidthPx.roundToInt()}px") {
                    outlineWidthPx = it
                }
                TypographySlider("3D Depth", extrusionDepthPx, 0f..40f, "${extrusionDepthPx.roundToInt()}px") {
                    extrusionDepthPx = it
                }
                TypographySlider("Pos X", posX, 0f..docWidth.toFloat(), "${posX.roundToInt()}") {
                    posX = it
                }
                TypographySlider("Pos Y", posY, 0f..docHeight.toFloat(), "${posY.roundToInt()}") {
                    posY = it
                }

                // --- Font family ---
                Text(
                    text = "FONT FAMILY",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableFonts.forEach { family ->
                        val selected = fontFamily == family.resourceName
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (selected) colors.accentLime else colors.surfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selected) colors.accentLime else colors.borderSubtle
                            ),
                            modifier = Modifier.clickable { fontFamily = family.resourceName }
                        ) {
                            Text(
                                text = family.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) colors.onAccent else colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // --- Letter spacing (tracking) ---
                TypographySlider(
                    "Letter spacing",
                    letterSpacing,
                    -0.2f..0.5f,
                    "${if (letterSpacing >= 0) "+" else ""}${letterSpacing.toString().take(3)}em"
                ) {
                    letterSpacing = it
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gradient Fill", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    Switch(
                        checked = useGradientFill,
                        onCheckedChange = { useGradientFill = it },
                        enabled = style != GraffitiTextStyle.CHROME,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onAccent,
                            checkedTrackColor = colors.accentLime
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Drop Shadow", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    Switch(
                        checked = shadowEnabled,
                        onCheckedChange = { shadowEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onAccent,
                            checkedTrackColor = colors.accentLime
                        )
                    )
                }

                if (style != GraffitiTextStyle.CHROME) {
                    Text("PRIMARY COLOR", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    ColorSwatchRow(selectedArgb = primaryColor, onSelect = { primaryColor = it })
                    if (useGradientFill) {
                        Text("GRADIENT END COLOR", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        ColorSwatchRow(selectedArgb = secondaryColor, onSelect = { secondaryColor = it })
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                    UrbanPrimaryButton(
                        enabled = text.isNotBlank(),
                        onClick = {
                            onApply(
                                GraffitiTextElement(
                                    id = editingId ?: UUID.randomUUID().toString(),
                                    layerId = activeLayerId,
                                    text = text.trim(),
                                    x = posX,
                                    y = posY,
                                    fontSizePx = fontSizePx,
                                    fontFamily = fontFamily,
                                    letterSpacing = letterSpacing,
                                    style = style,
                                    primaryColorArgb = primaryColor,
                                    secondaryColorArgb = secondaryColor,
                                    useGradientFill = useGradientFill,
                                    outlineWidthPx = outlineWidthPx,
                                    shadowEnabled = shadowEnabled,
                                    extrusionDepthPx = extrusionDepthPx
                                )
                            )
                        }
                    ) {
                        Text(if (editingId != null) "Update Text" else "Apply Text")
                    }
                }
            }
        }
    }
}

@Composable
private fun TypographySlider(
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
            modifier = Modifier.width(46.dp)
        )
    }
}

@Composable
private fun ColorSwatchRow(
    selectedArgb: Int,
    onSelect: (Int) -> Unit
) {
    val colors = LocalUrbanColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UrbanPalette.studioSwatches.forEach { argb ->
            val selected = selectedArgb == argb
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
                    .clickable { onSelect(argb) }
            )
        }
    }
}
