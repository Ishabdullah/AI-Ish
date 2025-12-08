package com.ishabdullah.aiish.code

import timber.log.Timber
import java.io.File

/**
 * CodeToolPro - Safe code editing with permissions
 * Ported from Codey safety system
 */
class CodeToolPro {

    private val allowedPaths = mutableSetOf<String>()

    fun readFile(path: String): String? {
        return try {
            if (!isPathAllowed(path)) {
                Timber.w("Access denied: $path")
                return null
            }

            val file = File(path)
            if (!file.exists()) {
                Timber.w("File not found: $path")
                return null
            }

            file.readText()
        } catch (e: Exception) {
            Timber.e(e, "Error reading file: $path")
            null
        }
    }

    fun writeFile(path: String, content: String): Boolean {
        return try {
            if (!isPathAllowed(path)) {
                Timber.w("Write access denied: $path")
                return false
            }

            val file = File(path)
            file.writeText(content)
            Timber.i("File written: $path")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error writing file: $path")
            false
        }
    }

    fun allowPath(path: String) {
        allowedPaths.add(path)
        Timber.i("Path allowed: $path")
    }

    private fun isPathAllowed(path: String): Boolean {
        return allowedPaths.any { path.startsWith(it) }
    }
}
