/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ishabdullah.aiish.ml.ModelCatalog
import com.ishabdullah.aiish.ml.ModelInfo
import com.ishabdullah.aiish.ml.ModelType
import com.ishabdullah.aiish.ui.viewmodels.ModelDownloadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadScreen(
    onDownloadComplete: () -> Unit,
    viewModel: ModelDownloadViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.downloadComplete) {
        if (state.downloadComplete) {
            onDownloadComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to AI Ish") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Choose Your AI Mode",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Select a model to download and start using AI Ish",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (state.isDownloading) {
                DownloadProgressCard(
                    progress = state.currentDownload,
                    onCancel = { viewModel.cancelDownload() }
                )
            } else {
                ModelSelectionCard(
                    modelInfo = ModelCatalog.PHI4_MINI,
                    isSelected = state.selectedModel?.id == ModelCatalog.PHI4_MINI.id,
                    onSelect = { viewModel.selectModel(ModelCatalog.PHI4_MINI) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                ModelSelectionCard(
                    modelInfo = ModelCatalog.QWEN2_7B,
                    isSelected = state.selectedModel?.id == ModelCatalog.QWEN2_7B.id,
                    onSelect = { viewModel.selectModel(ModelCatalog.QWEN2_7B) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.includeVision)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Include Vision Mode",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${ModelCatalog.MOONDREAM2.sizeMB} MB • Optional",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = state.includeVision,
                            onCheckedChange = { viewModel.toggleVision(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = { viewModel.startDownload() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = state.selectedModel != null && !state.isDownloading
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    val totalSize = (state.selectedModel?.sizeMB ?: 0) +
                            if (state.includeVision) ModelCatalog.MOONDREAM2.sizeMB else 0
                    Text("Download ($totalSize MB)")
                }
            }
        }
    }
}

@Composable
fun ModelSelectionCard(
    modelInfo: ModelInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            CardDefaults.outlinedCardBorder().copy(width = 2.dp)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (modelInfo.type) {
                    ModelType.INSTANT -> Icons.Default.Bolt
                    ModelType.DEEP -> Icons.Default.Psychology
                    else -> Icons.Default.Visibility
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = modelInfo.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = modelInfo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${modelInfo.sizeMB} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DownloadProgressCard(
    progress: com.ishabdullah.aiish.ml.DownloadProgress?,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                progress = { (progress?.progressPercent ?: 0) / 100f },
                modifier = Modifier.size(80.dp),
                strokeWidth = 8.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Downloading Model...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            progress?.let {
                Text(
                    text = "${it.progressPercent}% • ${String.format("%.1f", it.speedMBps)} MB/s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val downloadedMB = it.bytesDownloaded / 1024f / 1024f
                val totalMB = it.totalBytes / 1024f / 1024f
                Text(
                    text = "${String.format("%.1f", downloadedMB)} / ${String.format("%.1f", totalMB)} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { (progress?.progressPercent ?: 0) / 100f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel Download")
            }
        }
    }
}
