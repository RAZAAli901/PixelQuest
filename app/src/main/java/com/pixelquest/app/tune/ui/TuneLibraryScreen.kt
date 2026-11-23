package com.pixelquest.app.tune.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

import com.pixelquest.app.tune.model.TuneTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuneLibraryScreen(
    libraryTarget: TuneTarget,
    onBack: () -> Unit,
    viewModel: TuneLibraryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val previewPlayer = remember { TunePreviewPlayer(context) }
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer.release()
            viewModel.setPreviewingTune(null)
        }
    }

    LaunchedEffect(libraryTarget) {
        viewModel.initialize(libraryTarget)
    }

    val title = if (libraryTarget == TuneTarget.QUEST) "Quest Reminder Tunes" else "Namaz Reminder Tunes"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (uiState.totalCacheSizeBytes > 0) {
                val kb = uiState.totalCacheSizeBytes / 1024
                Text(
                    text = "Total cached audio: $kb KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Default Bundled Tune option
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { viewModel.selectDefault() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.isDefaultSelected,
                            onClick = { viewModel.selectDefault() }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Default Bundled Sound",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Built-in offline sound",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (uiState.tunes.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No tunes available yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(uiState.tunes, key = { it.tune.id }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable { viewModel.selectTune(item.tune) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = item.isSelected,
                            onClick = { viewModel.selectTune(item.tune) }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.tune.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = item.tune.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val kb = item.tune.bytes / 1024
                            val sec = item.tune.durationMillis / 1000
                            Text(
                                text = "${sec}s • ${kb} KB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Preview button
                        item.tune.previewUrl?.let { pUrl ->
                            IconButton(onClick = {
                                if (item.isPlayingPreview) {
                                    previewPlayer.stop()
                                    viewModel.setPreviewingTune(null)
                                } else {
                                    previewPlayer.playPreview(item.tune.id, pUrl)
                                    viewModel.setPreviewingTune(item.tune.id)
                                }
                            }) {
                                if (item.isPlayingPreview) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop Preview")
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Preview")
                                }
                            }
                        }

                        // Download status indicator
                        if (item.isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else if (item.isDownloaded && !item.isSelected) {
                            IconButton(onClick = { viewModel.removeTune(item.tune) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove cached tune",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
