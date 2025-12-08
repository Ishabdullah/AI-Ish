package com.ishabdullah.aiish.core

import timber.log.Timber

/**
 * PrivacyGuard - Ensures user privacy and explicit permissions
 * No action is taken without user consent
 */
class PrivacyGuard {

    private val grantedPermissions = mutableSetOf<String>()

    /**
     * Check if an action is allowed
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

    fun grantPermission(permission: String) {
        grantedPermissions.add(permission)
        Timber.i("Permission granted: $permission")
    }

    fun revokePermission(permission: String) {
        grantedPermissions.remove(permission)
        Timber.i("Permission revoked: $permission")
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
}
