/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File

/**
 * Permission-First Architecture Manager
 *
 * Inspired by Claude Code's safety-first approach:
 * - Every file operation shows preview before execution
 * - Git operations display affected files with warnings
 * - Shell commands are classified as SAFE/RISKY/FORBIDDEN
 * - Batch operations offer auto-approve mode
 * - All decisions logged for audit trail
 */
class PermissionManager {

    /**
     * Permission types that require user approval
     */
    enum class PermissionType {
        FILE_CREATE,
        FILE_READ,
        FILE_EDIT,
        FILE_DELETE,
        DIRECTORY_CREATE,
        GIT_CLONE,
        GIT_COMMIT,
        GIT_PUSH,
        GIT_PULL,
        SHELL_SAFE,      // ls, cat, pwd, etc.
        SHELL_RISKY,     // mkdir, pip install, git push
        SHELL_FORBIDDEN, // rm -rf /, fork bombs, etc.
        NETWORK_REQUEST,
        CAMERA_ACCESS,
        LOCATION_ACCESS
    }

    /**
     * Permission request with full context
     */
    data class PermissionRequest(
        val id: String = java.util.UUID.randomUUID().toString(),
        val type: PermissionType,
        val action: String,
        val targetPath: String? = null,
        val preview: String? = null,
        val affectedFiles: List<String> = emptyList(),
        val warningLevel: WarningLevel = WarningLevel.INFO,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Warning levels for operations
     */
    enum class WarningLevel {
        INFO,      // Safe operations
        CAUTION,   // Potentially impactful operations
        DANGER,    // Destructive operations
        FORBIDDEN  // Blocked operations
    }

    /**
     * Permission decision with audit trail
     */
    data class PermissionDecision(
        val requestId: String,
        val type: PermissionType,
        val action: String,
        val approved: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
        val reason: String? = null
    )

    // State management
    private val _currentRequest = MutableStateFlow<PermissionRequest?>(null)
    val currentRequest: StateFlow<PermissionRequest?> = _currentRequest.asStateFlow()

    private val _batchApprovalMode = MutableStateFlow(false)
    val batchApprovalMode: StateFlow<Boolean> = _batchApprovalMode.asStateFlow()

    // Granted permissions (session-based)
    private val grantedPermissions = mutableSetOf<PermissionType>()

    // Audit trail
    private val decisionLog = mutableListOf<PermissionDecision>()

    /**
     * Request permission with full preview
     */
    suspend fun requestPermission(request: PermissionRequest): Boolean {
        // Check if permission already granted for this session
        if (request.type in grantedPermissions) {
            logDecision(request, approved = true, reason = "Session permission granted")
            return true
        }

        // Check batch approval mode
        if (_batchApprovalMode.value && request.warningLevel != WarningLevel.FORBIDDEN) {
            logDecision(request, approved = true, reason = "Batch approval mode active")
            return true
        }

        // Forbidden operations are always blocked
        if (request.warningLevel == WarningLevel.FORBIDDEN) {
            Timber.e("FORBIDDEN operation blocked: ${request.action}")
            logDecision(request, approved = false, reason = "Forbidden operation")
            return false
        }

        // Set current request for UI to display
        _currentRequest.value = request

        // In a real app, this would wait for user input via UI
        // For now, we'll auto-approve non-dangerous operations
        val approved = when (request.warningLevel) {
            WarningLevel.INFO -> true
            WarningLevel.CAUTION -> false // Require explicit approval
            WarningLevel.DANGER -> false  // Require explicit approval
            WarningLevel.FORBIDDEN -> false
        }

        logDecision(request, approved)
        _currentRequest.value = null

        return approved
    }

    /**
     * Grant permission for current session
     */
    fun grantSessionPermission(type: PermissionType) {
        grantedPermissions.add(type)
        Timber.i("Session permission granted: $type")
    }

    /**
     * Revoke session permission
     */
    fun revokeSessionPermission(type: PermissionType) {
        grantedPermissions.remove(type)
        Timber.i("Session permission revoked: $type")
    }

    /**
     * Enable batch approval mode
     */
    fun enableBatchApproval() {
        _batchApprovalMode.value = true
        Timber.w("Batch approval mode ENABLED - all operations will auto-approve")
    }

    /**
     * Disable batch approval mode
     */
    fun disableBatchApproval() {
        _batchApprovalMode.value = false
        Timber.i("Batch approval mode DISABLED")
    }

    /**
     * Classify shell command safety level
     */
    fun classifyShellCommand(command: String): Pair<PermissionType, WarningLevel> {
        val cmd = command.trim().lowercase()

        // Forbidden patterns
        val forbiddenPatterns = listOf(
            Regex("""rm\s+-rf\s+/"""),           // rm -rf /
            Regex("""rm\s+-rf\s+\*"""),          // rm -rf *
            Regex("""mkfs"""),                   // Format filesystem
            Regex("""dd\s+if=.*of=/dev/"""),     // Direct disk write
            Regex(""":\(\)\{.*;\}"""),           // Fork bomb
            Regex("""git\s+push\s+.*--force.*main"""), // Force push to main
            Regex("""git\s+push\s+.*--force.*master""") // Force push to master
        )

        for (pattern in forbiddenPatterns) {
            if (pattern.containsMatchIn(cmd)) {
                return Pair(PermissionType.SHELL_FORBIDDEN, WarningLevel.FORBIDDEN)
            }
        }

        // Safe commands (read-only)
        val safeCommands = listOf(
            "ls", "cat", "pwd", "grep", "find", "wc", "head", "tail",
            "echo", "date", "whoami", "uname", "df", "du", "ps", "top",
            "git status", "git log", "git diff", "git branch"
        )

        for (safe in safeCommands) {
            if (cmd.startsWith(safe)) {
                return Pair(PermissionType.SHELL_SAFE, WarningLevel.INFO)
            }
        }

        // Risky commands (state-modifying)
        val riskyCommands = listOf(
            "mkdir", "touch", "cp", "mv", "chmod", "chown",
            "pip install", "npm install", "apt install",
            "git add", "git commit", "git push", "git pull", "git clone"
        )

        for (risky in riskyCommands) {
            if (cmd.startsWith(risky)) {
                return if (cmd.contains("git push") && !cmd.contains("--force")) {
                    Pair(PermissionType.GIT_PUSH, WarningLevel.CAUTION)
                } else {
                    Pair(PermissionType.SHELL_RISKY, WarningLevel.CAUTION)
                }
            }
        }

        // Default to risky for unknown commands
        return Pair(PermissionType.SHELL_RISKY, WarningLevel.CAUTION)
    }

    /**
     * Create permission request for file operation
     */
    fun createFileRequest(
        operation: String,
        filePath: String,
        content: String? = null
    ): PermissionRequest {
        val type = when (operation.lowercase()) {
            "create" -> PermissionType.FILE_CREATE
            "read" -> PermissionType.FILE_READ
            "edit", "modify" -> PermissionType.FILE_EDIT
            "delete" -> PermissionType.FILE_DELETE
            else -> PermissionType.FILE_EDIT
        }

        val warningLevel = when (type) {
            PermissionType.FILE_DELETE -> WarningLevel.DANGER
            PermissionType.FILE_EDIT -> WarningLevel.CAUTION
            else -> WarningLevel.INFO
        }

        val preview = if (content != null && content.length < 500) {
            content
        } else if (content != null) {
            "${content.take(500)}... (${content.length} chars total)"
        } else {
            null
        }

        return PermissionRequest(
            type = type,
            action = "$operation file: $filePath",
            targetPath = filePath,
            preview = preview,
            warningLevel = warningLevel
        )
    }

    /**
     * Create permission request for git operation
     */
    fun createGitRequest(
        operation: String,
        repository: String,
        affectedFiles: List<String> = emptyList(),
        commitMessage: String? = null
    ): PermissionRequest {
        val type = when (operation.lowercase()) {
            "clone" -> PermissionType.GIT_CLONE
            "commit" -> PermissionType.GIT_COMMIT
            "push" -> PermissionType.GIT_PUSH
            "pull" -> PermissionType.GIT_PULL
            else -> PermissionType.GIT_COMMIT
        }

        val warningLevel = when (type) {
            PermissionType.GIT_PUSH -> WarningLevel.DANGER
            PermissionType.GIT_COMMIT -> WarningLevel.CAUTION
            else -> WarningLevel.INFO
        }

        val preview = commitMessage ?: if (affectedFiles.isNotEmpty()) {
            "Affected files:\n${affectedFiles.take(10).joinToString("\n")}"
        } else {
            null
        }

        return PermissionRequest(
            type = type,
            action = "Git $operation: $repository",
            targetPath = repository,
            preview = preview,
            affectedFiles = affectedFiles,
            warningLevel = warningLevel
        )
    }

    /**
     * Log permission decision for audit trail
     */
    private fun logDecision(request: PermissionRequest, approved: Boolean, reason: String? = null) {
        val decision = PermissionDecision(
            requestId = request.id,
            type = request.type,
            action = request.action,
            approved = approved,
            reason = reason
        )

        decisionLog.add(decision)

        // Keep only last 100 decisions
        if (decisionLog.size > 100) {
            decisionLog.removeAt(0)
        }

        Timber.i("Permission decision: ${request.type} - ${if (approved) "APPROVED" else "DENIED"}")
    }

    /**
     * Get audit trail
     */
    fun getAuditTrail(): List<PermissionDecision> = decisionLog.toList()

    /**
     * Get decisions for specific permission type
     */
    fun getDecisionsForType(type: PermissionType): List<PermissionDecision> {
        return decisionLog.filter { it.type == type }
    }

    /**
     * Clear audit trail
     */
    fun clearAuditTrail() {
        decisionLog.clear()
        Timber.i("Audit trail cleared")
    }

    /**
     * Clear all session permissions
     */
    fun clearSession() {
        grantedPermissions.clear()
        disableBatchApproval()
        Timber.i("Session permissions cleared")
    }
}
