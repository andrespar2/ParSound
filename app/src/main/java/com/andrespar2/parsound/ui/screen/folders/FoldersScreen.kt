package com.andrespar2.parsound.ui.screen.folders

import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.andrespar2.parsound.data.prefs.FolderConfig
import com.andrespar2.parsound.scanner.ScanState
import com.andrespar2.parsound.ui.components.AmbientBackground
import com.andrespar2.parsound.ui.components.EmptyState
import com.andrespar2.parsound.ui.components.GlassCard
import com.andrespar2.parsound.ui.components.ParSoundTopBar
import com.andrespar2.parsound.ui.components.PillButton
import com.andrespar2.parsound.util.songsLabel
import com.andrespar2.parsound.util.foldersLabel
import com.andrespar2.parsound.ui.theme.LocalGlass
import com.andrespar2.parsound.util.formatDate

@Composable
fun FoldersScreen(
    navController: NavController,
    viewModel: FoldersViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val glass = LocalGlass.current
    val folders by viewModel.configuredFolders.collectAsState()
    val summaries by viewModel.folderSummaries.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val lastScan by viewModel.lastScan.collectAsState()
    val songCount by viewModel.songCount.collectAsState()
    var folderToDelete by remember { mutableStateOf<FolderConfig?>(null) }
    var showImportInfo by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val permission = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            if (permission.isFailure) {
                Toast.makeText(context, "No se pudo conservar el permiso de la carpeta", Toast.LENGTH_LONG).show()
            }
            val name = runCatching { DocumentFile.fromTreeUri(context, uri)?.name }.getOrNull()
                ?: "Carpeta de música"
            viewModel.addFolder(FolderConfig(uri.toString(), name))
            Toast.makeText(context, "Analizando $name…", Toast.LENGTH_SHORT).show()
        }
    }

    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.importMediaStore { count ->
                Toast.makeText(
                    context,
                    if (count > 0) "$count canciones importadas" else "No hay música nueva",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(context, "Permiso denegado. Puedes seguir usando carpetas SAF.", Toast.LENGTH_LONG).show()
        }
    }

    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            containerColor = glass.backgroundEnd,
            title = { Text("Quitar carpeta", color = glass.textPrimary) },
            text = {
                Text(
                    "Las canciones de esta carpeta se quitarán de ParSound. Tus archivos no se modifican ni se borran.",
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    folderToDelete?.let { viewModel.removeFolder(it) }
                    folderToDelete = null
                }) { Text("Quitar", color = glass.accent) }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Cancelar", color = glass.textSecondary)
                }
            }
        )
    }

    if (showImportInfo) {
        AlertDialog(
            onDismissRequest = { showImportInfo = false },
            containerColor = glass.backgroundEnd,
            title = { Text("Importar del dispositivo", color = glass.textPrimary) },
            text = {
                Text(
                    "Se pedirá permiso para leer el audio registrado por Android. Es opcional: con las carpetas SAF ParSound accede únicamente a lo que elijas.",
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportInfo = false
                    permissionLauncher.launch(mediaPermission)
                }) { Text("Conceder permiso", color = glass.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showImportInfo = false }) {
                    Text("Cancelar", color = glass.textSecondary)
                }
            }
        )
    }

    AmbientBackground(showArtwork = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            ParSoundTopBar(
                title = "Carpetas de música",
                subtitle = "${songsLabel(songCount)} · ${foldersLabel(folders.size)}",
                onBack = { navController.navigateUp() },
                actions = {
                    IconButton(onClick = { viewModel.scan() }) {
                        Icon(Icons.Default.Refresh, "Actualizar", tint = glass.textPrimary)
                    }
                    IconButton(onClick = { folderPicker.launch(null) }) {
                        Icon(Icons.Default.Add, "Añadir carpeta", tint = glass.textPrimary)
                    }
                }
            )
            if (scanState is ScanState.Scanning) {
                val scanning = scanState as ScanState.Scanning
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(glass.glassFill)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = glass.accent
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Analizando ${scanning.folder}",
                            color = glass.textPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${scanning.filesProcessed} archivos procesados · ${scanning.errors} avisos",
                            color = glass.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(onClick = viewModel::cancelScan) {
                        Text("Cancelar", color = glass.accent)
                    }
                }
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    GlassCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        strong = true
                    ) {
                        Text(
                            "Control total de tu música",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "ParSound usa el selector de carpetas de Android. Solo puede leer las carpetas que autorices y conserva el permiso entre sesiones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Último análisis: ${formatDate(lastScan)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillButton("Añadir carpeta", onClick = { folderPicker.launch(null) }, icon = Icons.Default.Add)
                            PillButton(
                                "Importar",
                                onClick = { showImportInfo = true },
                                icon = Icons.Default.FolderOpen,
                                primary = false
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        PillButton(
                            "Analizar ahora",
                            onClick = viewModel::scan,
                            icon = Icons.Default.Refresh,
                            primary = false
                        )
                    }
                }

                if (folders.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Folder,
                            title = "Sin carpetas",
                            message = "Elige la carpeta donde tienes tus MP3, FLAC, M4A, OGG o WAV. ParSound leerá los metadatos y las portadas.",
                            actionLabel = "Elegir carpeta",
                            onAction = { folderPicker.launch(null) }
                        )
                    }
                }

                items(folders, key = { it.uri }) { folder ->
                    val summary = summaries.firstOrNull { it.folderUri == folder.uri }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(glass.glassFill)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(glass.glassStrong),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Folder, null, tint = glass.accent)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                folder.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = glass.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                songsLabel(summary?.songCount ?: 0),
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                        IconButton(onClick = { folderToDelete = folder }) {
                            Icon(Icons.Default.Delete, "Quitar carpeta", tint = glass.textSecondary)
                        }
                    }
                }

                val mediastore = summaries.firstOrNull { it.folderUri == "mediastore://all" }
                if (mediastore != null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(glass.glassFill)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderOpen, null, tint = glass.accent)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Almacenamiento del dispositivo",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = glass.textPrimary
                                )
                                Text(
                                    songsLabel(mediastore.songCount) + " importadas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
