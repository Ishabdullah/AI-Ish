/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ishabdullah.aiish.core.PermissionManager

/**
 * Permission Dialog - Claude Code-style permission request
 *
 * Shows:
 * - Operation type with icon
 * - Warning level (color-coded)
 * - Preview of changes
 * - Affected files list
 * - Approve/Deny buttons
 */
@Composable
fun PermissionDialog(
    request: PermissionManager.PermissionRequest,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onApproveAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDeny,
        icon = {
            Icon(
                imageVector = getIconForPermissionType(request.type),
                contentDescription = null,
                tint = getColorForWarningLevel(request.warningLevel),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Column {
                Text(
                    text = getPermissionTitle(request.type),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                WarningLevelBadge(request.warningLevel)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Action description
                Text(
                    text = request.action,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Target path
                if (request.targetPath != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Target:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = request.targetPath,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Preview
                if (request.preview != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    PreviewCard(request.preview)
                }

                // Affected files
                if (request.affectedFiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AffectedFilesList(request.affectedFiles, request.warningLevel)
                }

                // Warning message for dangerous operations
                if (request.warningLevel == PermissionManager.WarningLevel.DANGER) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This operation is potentially destructive",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                // Approve All button (for batch operations)
                if (onApproveAll != null) {
                    TextButton(onClick = onApproveAll) {
                        Text("Approve All")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Deny button
                TextButton(onClick = onDeny) {
                    Text("Deny")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Approve button
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (request.warningLevel) {
                            PermissionManager.WarningLevel.DANGER -> MaterialTheme.colorScheme.error
                            PermissionManager.WarningLevel.CAUTION -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text("Approve")
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun WarningLevelBadge(level: PermissionManager.WarningLevel) {
    Surface(
        color = getColorForWarningLevel(level).copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = level.name,
            style = MaterialTheme.typography.labelSmall,
            color = getColorForWarningLevel(level),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PreviewCard(preview: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Preview:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AffectedFilesList(
    files: List<String>,
    warningLevel: PermissionManager.WarningLevel
) {
    Column {
        Text(
            text = "Affected Files:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                files.take(10).forEach { file ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = getColorForWarningLevel(warningLevel)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = file,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                if (files.size > 10) {
                    Text(
                        text = "... and ${files.size - 10} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun getIconForPermissionType(type: PermissionManager.PermissionType) = when (type) {
    PermissionManager.PermissionType.FILE_CREATE -> Icons.Default.Add
    PermissionManager.PermissionType.FILE_READ -> Icons.Default.Description
    PermissionManager.PermissionType.FILE_EDIT -> Icons.Default.Edit
    PermissionManager.PermissionType.FILE_DELETE -> Icons.Default.Delete
    PermissionManager.PermissionType.DIRECTORY_CREATE -> Icons.Default.Folder
    PermissionManager.PermissionType.GIT_CLONE -> Icons.Default.Download
    PermissionManager.PermissionType.GIT_COMMIT -> Icons.Default.Save
    PermissionManager.PermissionType.GIT_PUSH -> Icons.Default.Upload
    PermissionManager.PermissionType.GIT_PULL -> Icons.Default.Sync
    PermissionManager.PermissionType.SHELL_SAFE -> Icons.Default.Build
    PermissionManager.PermissionType.SHELL_RISKY -> Icons.Default.Warning
    PermissionManager.PermissionType.SHELL_FORBIDDEN -> Icons.Default.Cancel
    PermissionManager.PermissionType.NETWORK_REQUEST -> Icons.Default.Language
    PermissionManager.PermissionType.CAMERA_ACCESS -> Icons.Default.PhotoCamera
    PermissionManager.PermissionType.LOCATION_ACCESS -> Icons.Default.LocationOn
}

@Composable
private fun getColorForWarningLevel(level: PermissionManager.WarningLevel) = when (level) {
    PermissionManager.WarningLevel.INFO -> MaterialTheme.colorScheme.primary
    PermissionManager.WarningLevel.CAUTION -> Color(0xFFFFA726) // Orange
    PermissionManager.WarningLevel.DANGER -> MaterialTheme.colorScheme.error
    PermissionManager.WarningLevel.FORBIDDEN -> Color(0xFF9E9E9E) // Gray
}

private fun getPermissionTitle(type: PermissionManager.PermissionType) = when (type) {
    PermissionManager.PermissionType.FILE_CREATE -> "Create File"
    PermissionManager.PermissionType.FILE_READ -> "Read File"
    PermissionManager.PermissionType.FILE_EDIT -> "Edit File"
    PermissionManager.PermissionType.FILE_DELETE -> "Delete File"
    PermissionManager.PermissionType.DIRECTORY_CREATE -> "Create Directory"
    PermissionManager.PermissionType.GIT_CLONE -> "Git Clone"
    PermissionManager.PermissionType.GIT_COMMIT -> "Git Commit"
    PermissionManager.PermissionType.GIT_PUSH -> "Git Push"
    PermissionManager.PermissionType.GIT_PULL -> "Git Pull"
    PermissionManager.PermissionType.SHELL_SAFE -> "Execute Command"
    PermissionManager.PermissionType.SHELL_RISKY -> "Execute Risky Command"
    PermissionManager.PermissionType.SHELL_FORBIDDEN -> "Forbidden Command"
    PermissionManager.PermissionType.NETWORK_REQUEST -> "Network Request"
    PermissionManager.PermissionType.CAMERA_ACCESS -> "Camera Access"
    PermissionManager.PermissionType.LOCATION_ACCESS -> "Location Access"
}
