package com.graffitistudio.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.graffitistudio.core.designsystem.component.UrbanIconButton
import com.graffitistudio.core.designsystem.component.UrbanPanel
import com.graffitistudio.core.designsystem.theme.LocalUrbanColors
import com.graffitistudio.core.designsystem.theme.UrbanPalette
import com.graffitistudio.core.domain.model.BrushType
import com.graffitistudio.engine.drawing.DrawingSurfaceView
import kotlin.math.roundToInt

@Composable
fun WorkspaceScreen(
    viewModel: WorkspaceViewModel,
    onNavigateBack: () -> Unit
) {
    val colors = LocalUrbanColors.current
    val uiState by viewModel.uiState.collectAsState()
    val historyState by viewModel.historyState.collectAsState()
    var surfaceViewRef by remember { mutableStateOf<DrawingSurfaceView?>(null) }

    Scaffold(
        containerColor = colors.backgroundDeep,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfacePrimary)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    UrbanIconButton(
                        onClick = { viewModel.saveProject(onSaved = onNavigateBack) }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back & Save")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = uiState.projectName.ifEmpty { "Canvas" },
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = uiState.dimensionsLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UrbanIconButton(
                        onClick = { viewModel.undo() },
                        enabled = historyState.canUndo
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    UrbanIconButton(
                        onClick = { viewModel.redo() },
                        enabled = historyState.canRedo
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    UrbanIconButton(
                        onClick = { surfaceViewRef?.resetViewport() }
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Fit Canvas")
                    }
                    UrbanIconButton(
                        onClick = { viewModel.saveProject() },
                        enabled = !uiState.isSaving
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save Project")
                    }
                }
            }
        },
        bottomBar = {
            UrbanPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                containerColor = colors.surfacePrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UrbanIconButton(
                            onClick = { viewModel.selectBrushType(BrushType.BASIC_BRUSH) },
                            selected = uiState.activeBrush.type == BrushType.BASIC_BRUSH
                        ) {
                            Icon(Icons.Default.Brush, contentDescription = "Brush")
                        }
                        UrbanIconButton(
                            onClick = { viewModel.selectBrushType(BrushType.ERASER) },
                            selected = uiState.activeBrush.type == BrushType.ERASER
                        ) {
                            Icon(Icons.Default.AutoFixNormal, contentDescription = "Eraser")
                        }

                        Slider(
                            value = uiState.activeBrush.sizePx,
                            onValueChange = { viewModel.setBrushSize(it) },
                            valueRange = 2f..120f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accentLime,
                                activeTrackColor = colors.accentLime,
                                inactiveTrackColor = colors.surfaceHighlight
                            )
                        )

                        Text(
                            text = "${uiState.activeBrush.sizePx.roundToInt()}px",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UrbanPalette.studioSwatches.forEach { argb ->
                            val isSelected = uiState.activeBrush.type == BrushType.BASIC_BRUSH &&
                                uiState.activeBrush.colorArgb == argb
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) colors.accentLime else colors.borderSubtle,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.setBrushColor(argb) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.backgroundDeep),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    DrawingSurfaceView(context).also { view ->
                        surfaceViewRef = view
                        view.attachEngine(viewModel.drawingEngine)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(color = colors.accentLime)
            }
        }
    }
}
