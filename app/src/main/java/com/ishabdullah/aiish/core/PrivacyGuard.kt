package com.ishabdullah.aiish.core

import timber.log.Timber

/**
 * PrivacyGuard - Ensures user privacy and explicit permissions
 * Now backed by PermissionManager for enhanced safety
 */
class PrivacyGuard {

    // Use PermissionManager as backend for comprehensive permission handling
    private val permissionManager = PermissionManager()

    // Legacy granted permissions for backward compatibility
    private val grantedPermissions = mutableSetOf<String>()

    /**
     * Get the underlying PermissionManager for advanced operations
     */
    fun getPermissionManager(): PermissionManager = permissionManager

    /**
     * Check if an action is allowed (legacy method)
     */
    fun isActionAllowed(query: String): Boolean {
        val requiredPermission = detectRequiredPermission(query)

        if (requiredPermission == null) {
            return true
        }

        if (requiredPermission in grantedPermissions) {
            return true
        }

        Timber.w("Action requires permission: $requiredPermission")
        return false
    }

    /**
     * Grant permission (legacy method)
     */
    fun grantPermission(permission: String) {
        grantedPermissions.add(permission)

        // Also grant in PermissionManager
        val permType = mapLegacyPermissionToType(permission)
        if (permType != null) {
            permissionManager.grantSessionPermission(permType)
        }

        Timber.i("Permission granted: $permission")
    }

    /**
     * Revoke permission (legacy method)
     */
    fun revokePermission(permission: String) {
        grantedPermissions.remove(permission)

        // Also revoke in PermissionManager
        val permType = mapLegacyPermissionToType(permission)
        if (permType != null) {
            permissionManager.revokeSessionPermission(permType)
        }

        Timber.i("Permission revoked: $permission")
    }

    /**
     * Request permission with full preview (new method)
     */
    suspend fun requestPermissionWithPreview(
        request: PermissionManager.PermissionRequest
    ): Boolean {
        return permissionManager.requestPermission(request)
    }

    /**
     * Enable batch approval mode
     */
    fun enableBatchApproval() {
        permissionManager.enableBatchApproval()
    }

    /**
     * Disable batch approval mode
     */
    fun disableBatchApproval() {
        permissionManager.disableBatchApproval()
    }

    /**
     * Get audit trail of all permission decisions
     */
    fun getAuditTrail(): List<PermissionManager.PermissionDecision> {
        return permissionManager.getAuditTrail()
    }

    /**
     * Clear all session permissions
     */
    fun clearSession() {
        grantedPermissions.clear()
        permissionManager.clearSession()
    }

    private fun detectRequiredPermission(query: String): String? {
        val lowerQuery = query.lowercase()

        return when {
            lowerQuery.contains(Regex("\\b(write|edit|modify|delete).*file\\b")) -> "file_write"
            lowerQuery.contains(Regex("\\b(fetch|get|download|api)\\b")) -> "network_request"
            lowerQuery.contains(Regex("\\b(camera|photo|picture)\\b")) -> "camera_access"
            lowerQuery.contains(Regex("\\b(location|gps|weather)\\b")) -> "location_access"
            else -> null
        }
    }

    private fun mapLegacyPermissionToType(permission: String): PermissionManager.PermissionType? {
        return when (permission) {
            "file_write" -> PermissionManager.PermissionType.FILE_EDIT
            "network_request" -> PermissionManager.PermissionType.NETWORK_REQUEST
            "camera_access" -> PermissionManager.PermissionType.CAMERA_ACCESS
            "location_access" -> PermissionManager.PermissionType.LOCATION_ACCESS
            else -> null
        }
    }
}
