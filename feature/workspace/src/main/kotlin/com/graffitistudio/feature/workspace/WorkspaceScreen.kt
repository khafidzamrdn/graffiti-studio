package com.graffitistudio.feature.workspace

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val layers by viewModel.layersState.collectAsState()
    val activeLayerId by viewModel.activeLayerIdState.collectAsState()
    var surfaceViewRef by remember { mutableStateOf<DrawingSurfaceView?>(null) }
    var isToolbarMenuOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    DisposableEffect(context) {
        val callbacks = object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                viewModel.trimMemory(level)
            }

            override fun onConfigurationChanged(newConfig: Configuration) = Unit

            @Deprecated("Deprecated in Java")
            override fun onLowMemory() {
                viewModel.trimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
            }
        }
        context.applicationContext.registerComponentCallbacks(callbacks)
        onDispose {
            context.applicationContext.unregisterComponentCallbacks(callbacks)
        }
    }

    LaunchedEffect(uiState.exportStatusMessage) {
        val msg = uiState.exportStatusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearExportStatus()
    }

    Scaffold(
        containerColor = colors.backgroundDeep,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfacePrimary)
                    .windowInsetsPadding(WindowInsets.statusBars)
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
                            text = "${uiState.dimensionsLabel} · ${uiState.activeBrush.type.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accentCyan
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
                        onClick = { viewModel.toggleLayerPanel() },
                        selected = uiState.isLayerPanelOpen
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = "Layers")
                    }

                    Box {
                        UrbanIconButton(onClick = { isToolbarMenuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                        }
                        DropdownMenu(
                            expanded = isToolbarMenuOpen,
                            onDismissRequest = { isToolbarMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Graffiti Typography") },
                                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                                onClick = {
                                    isToolbarMenuOpen = false
                                    viewModel.openTypographyDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Effects Studio") },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                                onClick = {
                                    isToolbarMenuOpen = false
                                    viewModel.openEffectsDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Fit Canvas") },
                                leadingIcon = {
                                    Icon(Icons.Default.CenterFocusStrong, contentDescription = null)
                                },
                                onClick = {
                                    isToolbarMenuOpen = false
                                    surfaceViewRef?.resetViewport()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Save Project") },
                                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                                enabled = !uiState.isSaving,
                                onClick = {
                                    isToolbarMenuOpen = false
                                    viewModel.saveProject()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Artwork") },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = {
                                    isToolbarMenuOpen = false
                                    viewModel.openExportDialog()
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(visible = uiState.isLayerPanelOpen) {
                    LayerPanel(
                        layers = layers,
                        activeLayerId = activeLayerId,
                        onSelectLayer = viewModel::selectLayer,
                        onAddLayer = viewModel::addLayer,
                        onDeleteLayer = viewModel::deleteLayer,
                        onRenameLayer = viewModel::renameLayer,
                        onToggleVisibility = viewModel::setLayerVisibility,
                        onOpacityChange = viewModel::setLayerOpacity,
                        onBlendModeChange = viewModel::setLayerBlendMode,
                        onMoveUp = viewModel::moveLayerUp,
                        onMoveDown = viewModel::moveLayerDown,
                        onDuplicate = viewModel::duplicateLayer,
                        onMergeDown = viewModel::mergeLayerDown
                    )
                }

                AnimatedVisibility(visible = uiState.isBrushPanelOpen) {
                    BrushStudioPanel(
                        activeBrush = uiState.activeBrush,
                        presets = uiState.presets,
                        activePresetId = uiState.activePresetId,
                        onSelectBrushType = viewModel::selectBrushType,
                        onSelectPreset = viewModel::applyPreset,
                        onSaveCustomPreset = viewModel::saveCurrentAsPreset,
                        onDeleteCustomPreset = viewModel::deleteCustomPreset,
                        onOpacityChange = viewModel::setBrushOpacity,
                        onFlowChange = viewModel::setBrushFlow,
                        onStabilizerChange = viewModel::setStabilizerLevel
                    )
                }

                UrbanPanel(
                    modifier = Modifier.fillMaxWidth(),
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UrbanIconButton(
                                onClick = { viewModel.selectBrushType(uiState.lastUsedNonEraserType) },
                                selected = uiState.activeBrush.type != BrushType.ERASER
                            ) {
                                Icon(Icons.Default.Brush, contentDescription = "Brush")
                            }
                            UrbanIconButton(
                                onClick = { viewModel.selectBrushType(BrushType.ERASER) },
                                selected = uiState.activeBrush.type == BrushType.ERASER
                            ) {
                                Icon(Icons.Default.AutoFixNormal, contentDescription = "Eraser")
                            }
                            UrbanIconButton(
                                onClick = { viewModel.toggleBrushPanel() },
                                selected = uiState.isBrushPanelOpen
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "Brush Engine & Presets")
                            }

                            Slider(
                                value = uiState.activeBrush.sizePx,
                                onValueChange = { viewModel.setBrushSize(it) },
                                valueRange = 2f..150f,
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
                                val isSelected = uiState.activeBrush.type != BrushType.ERASER &&
                                    uiState.activeBrush.colorArgb == argb
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
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
                            UrbanIconButton(
                                onClick = { viewModel.openColorPicker() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Custom Color Picker",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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
                        viewModel.drawingEngine.setContext(context)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(color = colors.accentLime)
            }
        }
    }

    if (uiState.isExportDialogOpen) {
        ExportDialog(
            docWidth = uiState.documentWidth,
            docHeight = uiState.documentHeight,
            isExporting = uiState.isExporting,
            onDismiss = { viewModel.dismissExportDialog() },
            onExport = { config -> viewModel.exportArtwork(config) }
        )
    }

    if (uiState.isTypographyDialogOpen) {
        val currentLayerId = activeLayerId ?: layers.lastOrNull()?.id
        if (currentLayerId != null) {
            val activeElements = layers.firstOrNull { it.id == currentLayerId }?.textElements.orEmpty()
            TypographyDialog(
                activeLayerId = currentLayerId,
                existingElements = activeElements,
                docWidth = uiState.documentWidth,
                docHeight = uiState.documentHeight,
                onDismiss = { viewModel.dismissTypographyDialog() },
                onApply = { element -> viewModel.upsertTextElement(element) },
                onDelete = { layerId, elementId -> viewModel.deleteTextElement(layerId, elementId) }
            )
        }
    }

    if (uiState.isEffectsDialogOpen) {
        val currentLayerId = activeLayerId ?: layers.lastOrNull()?.id
        if (currentLayerId != null) {
            val activeEffects = layers.firstOrNull { it.id == currentLayerId }?.effects.orEmpty()
            EffectsStudioDialog(
                activeLayerId = currentLayerId,
                activeEffects = activeEffects,
                onDismiss = { viewModel.dismissEffectsDialog() },
                onApplyEffect = { layerId, fx -> viewModel.addLayerEffect(layerId, fx) },
                onRemoveEffect = { layerId, fxId -> viewModel.removeLayerEffect(layerId, fxId) }
            )
        }
    }

    if (uiState.isColorPickerOpen) {
        ColorPickerDialog(
            initialColorArgb = uiState.activeBrush.colorArgb,
            onDismiss = { viewModel.dismissColorPicker() },
            onSelectColor = { argb ->
                viewModel.setBrushColor(argb)
                viewModel.dismissColorPicker()
            }
        )
    }
}
