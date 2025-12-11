package com.ishabdullah.aiish.code

import com.ishabdullah.aiish.core.PermissionManager
import timber.log.Timber
import java.io.File

/**
 * CodeToolPro - Safe code editing with permission-first architecture
 * Enhanced with previews and comprehensive safety checks
 */
class CodeToolPro(private val permissionManager: PermissionManager) {

    private val allowedPaths = mutableSetOf<String>()

    /**
     * Read file with permission check
     */
    suspend fun readFile(path: String): String? {
        return try {
            if (!isPathAllowed(path)) {
                Timber.w("Access denied: $path not in allowed paths")
                return null
            }

            val file = File(path)
            if (!file.exists()) {
                Timber.w("File not found: $path")
                return null
            }

            // Request permission to read
            val request = permissionManager.createFileRequest(
                operation = "read",
                filePath = path,
                content = null
            )

            val granted = permissionManager.requestPermission(request)
            if (!granted) {
                Timber.w("Permission denied to read file: $path")
                return null
            }

            file.readText()
        } catch (e: Exception) {
            Timber.e(e, "Error reading file: $path")
            null
        }
    }

    /**
     * Write file with preview and permission
     */
    suspend fun writeFile(path: String, content: String): Boolean {
        return try {
            if (!isPathAllowed(path)) {
                Timber.w("Write access denied: $path not in allowed paths")
                return false
            }

            val file = File(path)
            val operation = if (file.exists()) "edit" else "create"

            // Request permission with content preview
            val request = permissionManager.createFileRequest(
                operation = operation,
                filePath = path,
                content = content
            )

            val granted = permissionManager.requestPermission(request)
            if (!granted) {
                Timber.w("Permission denied to write file: $path")
                return false
            }

            // Create backup if editing existing file
            if (file.exists()) {
                createBackup(file)
            }

            file.writeText(content)
            Timber.i("File written: $path")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error writing file: $path")
            false
        }
    }

    /**
     * Delete file with confirmation
     */
    suspend fun deleteFile(path: String): Boolean {
        return try {
            if (!isPathAllowed(path)) {
                Timber.w("Delete access denied: $path not in allowed paths")
                return false
            }

            val file = File(path)
            if (!file.exists()) {
                Timber.w("File not found for deletion: $path")
                return false
            }

            // Request permission with high danger level
            val request = permissionManager.createFileRequest(
                operation = "delete",
                filePath = path,
                content = "Size: ${file.length()} bytes"
            )

            val granted = permissionManager.requestPermission(request)
            if (!granted) {
                Timber.w("Permission denied to delete file: $path")
                return false
            }

            val deleted = file.delete()
            if (deleted) {
                Timber.i("File deleted: $path")
            }
            deleted
        } catch (e: Exception) {
            Timber.e(e, "Error deleting file: $path")
            false
        }
    }

    /**
     * Create directory with permission
     */
    suspend fun createDirectory(path: String): Boolean {
        return try {
            if (!isPathAllowed(path)) {
                Timber.w("Create directory access denied: $path not in allowed paths")
                return false
            }

            val dir = File(path)
            if (dir.exists()) {
                Timber.w("Directory already exists: $path")
                return true
            }

            // Verify parent exists first (Claude Code pattern)
            val parent = dir.parentFile
            if (parent != null && !parent.exists()) {
                Timber.w("Parent directory does not exist: ${parent.path}")
                return false
            }

            // Request permission
            val request = PermissionManager.PermissionRequest(
                type = PermissionManager.PermissionType.DIRECTORY_CREATE,
                action = "Create directory: $path",
                targetPath = path,
                warningLevel = PermissionManager.WarningLevel.INFO
            )

            val granted = permissionManager.requestPermission(request)
            if (!granted) {
                Timber.w("Permission denied to create directory: $path")
                return false
            }

            val created = dir.mkdirs()
            if (created) {
                Timber.i("Directory created: $path")
            }
            created
        } catch (e: Exception) {
            Timber.e(e, "Error creating directory: $path")
            false
        }
    }

    /**
     * Allow path for operations
     */
    fun allowPath(path: String) {
        allowedPaths.add(path)
        Timber.i("Path allowed: $path")
    }

    /**
     * Get all allowed paths
     */
    fun getAllowedPaths(): Set<String> = allowedPaths.toSet()

    /**
     * Check if path is allowed
     */
    private fun isPathAllowed(path: String): Boolean {
        // Empty allowlist = allow all (for now, until configured)
        if (allowedPaths.isEmpty()) {
            return true
        }
        return allowedPaths.any { path.startsWith(it) }
    }

    /**
     * Create backup of file before modification
     */
    private fun createBackup(file: File) {
        try {
            val backupFile = File(file.parent, "${file.name}.bak")
            file.copyTo(backupFile, overwrite = true)
            Timber.d("Backup created: ${backupFile.path}")
        } catch (e: Exception) {
            Timber.w(e, "Failed to create backup for: ${file.path}")
        }
    }
}
