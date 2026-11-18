package com.graffitistudio.feature.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.graffitistudio.core.designsystem.component.UrbanPanel
import com.graffitistudio.core.designsystem.component.UrbanPrimaryButton
import com.graffitistudio.core.designsystem.theme.LocalUrbanColors
import com.graffitistudio.core.domain.model.ProjectMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenProject: (String) -> Unit
) {
    val colors = LocalUrbanColors.current
    val projects by viewModel.projects.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val message by viewModel.message.collectAsState()
    var renamingProject by remember { mutableStateOf<ProjectMetadata?>(null) }
    var deletingProject by remember { mutableStateOf<ProjectMetadata?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        val msg = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearMessage()
    }

    Scaffold(
        containerColor = colors.backgroundDeep,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GRAFFITI STUDIO",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Urban Digital Canvas",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.accentCyan
                    )
                }

                UrbanPrimaryButton(onClick = { viewModel.openCreateDialog() }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("New Canvas")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "RECENT PROJECTS (${projects.size})",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (projects.isEmpty()) {
                UrbanPanel(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "No canvases yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Create your first wall piece to start sketching.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { onOpenProject(project.id) },
                            onRename = { renamingProject = project },
                            onDelete = { deletingProject = project }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { viewModel.dismissCreateDialog() },
            onCreate = { name, preset, bg ->
                viewModel.createProject(name, preset, bg, onOpenProject)
            }
        )
    }

    renamingProject?.let { target ->
        RenameProjectDialog(
            initialName = target.name,
            onDismiss = { renamingProject = null },
            onConfirm = { newName ->
                viewModel.renameProject(target.id, newName)
                renamingProject = null
            }
        )
    }

    deletingProject?.let { target ->
        DeleteProjectDialog(
            projectName = target.name,
            onDismiss = { deletingProject = null },
            onConfirm = {
                viewModel.deleteProject(target.id)
                deletingProject = null
            }
        )
    }
}

@Composable
private fun DeleteProjectDialog(
    projectName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalUrbanColors.current

    Dialog(onDismissRequest = onDismiss) {
        UrbanPanel {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "DELETE PROJECT",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = "\"$projectName\" and its thumbnail will be permanently deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                    TextButton(onClick = onConfirm) {
                        Text("Delete", color = colors.accentCoral)
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameProjectDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = LocalUrbanColors.current
    var name by remember { mutableStateOf(initialName) }

    Dialog(onDismissRequest = onDismiss) {
        UrbanPanel {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "RENAME PROJECT",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
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
                    UrbanPrimaryButton(onClick = { onConfirm(name) }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: ProjectMetadata,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalUrbanColors.current
    val thumbnailBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        project.thumbnailPath,
        project.updatedAtEpochMs
    ) {
        value = project.thumbnailPath?.let { path ->
            withContext(Dispatchers.IO) {
                val file = File(path)
                if (file.exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
            }
        }
    }

    UrbanPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(project.backgroundColorArgb)),
                contentAlignment = Alignment.Center
            ) {
                thumbnailBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = project.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${project.width}×${project.height}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }

                IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename project",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete project",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
