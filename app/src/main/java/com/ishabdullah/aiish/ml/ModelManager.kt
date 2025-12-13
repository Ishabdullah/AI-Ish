/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.ml

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.roundToInt

data class DownloadProgress(
    val modelId: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedMBps: Float,
    val isComplete: Boolean,
    val error: String? = null
) {
    val progressPercent: Int
        get() = if (totalBytes > 0) ((bytesDownloaded.toFloat() / totalBytes) * 100).roundToInt() else 0
}

class ModelManager(private val context: Context, private val storageDirectory: File) {

    companion object {
        private const val CONNECT_TIMEOUT_SEC = 30L
        private const val READ_TIMEOUT_SEC = 60L
        private const val WRITE_TIMEOUT_SEC = 30L
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val modelsDir: File = File(storageDirectory, "models").apply { mkdirs() }

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    suspend fun downloadModel(modelInfo: ModelInfo): Result<File> = withContext(Dispatchers.IO) {
        val outputFile = File(modelsDir, modelInfo.filename)

        // Check if already downloaded and valid
        if (outputFile.exists()) {
            Timber.d("Model ${modelInfo.id} already exists, verifying...")
            if (verifyChecksum(outputFile, modelInfo.sha256)) {
                _downloadProgress.value = DownloadProgress(
                    modelId = modelInfo.id,
                    bytesDownloaded = outputFile.length(),
                    totalBytes = outputFile.length(),
                    speedMBps = 0f,
                    isComplete = true
                )
                return@withContext Result.success(outputFile)
            }
            Timber.w("Checksum mismatch, re-downloading...")
            outputFile.delete()
        }

        var lastException: Exception? = null

        // Retry loop
        for (attempt in 1..MAX_RETRIES) {
            try {
                Timber.i("Downloading ${modelInfo.id} (attempt $attempt/$MAX_RETRIES)")

                val request = Request.Builder()
                    .url(modelInfo.downloadUrl)
                    .header("User-Agent", "AI-Ish/1.0")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("Download failed: HTTP ${response.code}")
                }

                val body = response.body ?: throw Exception("Empty response body")
                val totalBytes = body.contentLength().let { if (it <= 0) modelInfo.sizeMB * 1024 * 1024L else it }

                var downloadedBytes = 0L
                val startTime = System.currentTimeMillis()
                val buffer = ByteArray(16384)  // Larger buffer for faster downloads

                // Use temp file to avoid partial downloads
                val tempFile = File(modelsDir, "${modelInfo.filename}.tmp")

                FileOutputStream(tempFile).use { output ->
                    body.byteStream().use { input ->
                        var bytes: Int
                        var lastUpdateTime = startTime

                        while (input.read(buffer).also { bytes = it } != -1) {
                            output.write(buffer, 0, bytes)
                            downloadedBytes += bytes

                            val currentTime = System.currentTimeMillis()
                            // Update progress every 200ms for smoother UI
                            if (currentTime - lastUpdateTime > 200) {
                                val elapsedSeconds = (currentTime - startTime) / 1000f
                                val speedMBps = if (elapsedSeconds > 0) {
                                    (downloadedBytes / 1024f / 1024f) / elapsedSeconds
                                } else 0f

                                _downloadProgress.value = DownloadProgress(
                                    modelId = modelInfo.id,
                                    bytesDownloaded = downloadedBytes,
                                    totalBytes = totalBytes,
                                    speedMBps = speedMBps,
                                    isComplete = false
                                )
                                lastUpdateTime = currentTime
                            }
                        }
                    }
                }

                // Move temp file to final location
                if (outputFile.exists()) outputFile.delete()
                if (!tempFile.renameTo(outputFile)) {
                    tempFile.copyTo(outputFile, overwrite = true)
                    tempFile.delete()
                }

                // Verify checksum
                if (!verifyChecksum(outputFile, modelInfo.sha256)) {
                    outputFile.delete()
                    throw Exception("Checksum verification failed")
                }

                _downloadProgress.value = DownloadProgress(
                    modelId = modelInfo.id,
                    bytesDownloaded = downloadedBytes,
                    totalBytes = totalBytes,
                    speedMBps = 0f,
                    isComplete = true
                )

                Timber.i("Model ${modelInfo.id} downloaded successfully (${downloadedBytes / 1024 / 1024}MB)")
                return@withContext Result.success(outputFile)

            } catch (e: Exception) {
                lastException = e
                Timber.w(e, "Download attempt $attempt failed for ${modelInfo.id}")

                // Clean up partial downloads
                File(modelsDir, "${modelInfo.filename}.tmp").delete()

                if (attempt < MAX_RETRIES) {
                    Timber.i("Retrying in ${RETRY_DELAY_MS}ms...")
                    kotlinx.coroutines.delay(RETRY_DELAY_MS)
                }
            }
        }

        // All retries failed
        Timber.e(lastException, "All download attempts failed for ${modelInfo.id}")
        _downloadProgress.value = DownloadProgress(
            modelId = modelInfo.id,
            bytesDownloaded = 0,
            totalBytes = 0,
            speedMBps = 0f,
            isComplete = false,
            error = lastException?.message ?: "Download failed after $MAX_RETRIES attempts"
        )
        Result.failure(lastException ?: Exception("Download failed"))
    }

    fun cancelDownload() {
        client.dispatcher.cancelAll()
        _downloadProgress.value = null
    }

    fun isModelDownloaded(modelInfo: ModelInfo): Boolean {
        val file = File(modelsDir, modelInfo.filename)
        return file.exists() && file.length() > 0
    }

    fun getModelFile(modelInfo: ModelInfo): File? {
        val file = File(modelsDir, modelInfo.filename)
        return if (file.exists()) file else null
    }

    /**
     * Get absolute path to downloaded model file
     * Used for loading into LLMInferenceEngine
     */
    fun getModelPath(modelInfo: ModelInfo): String? {
        return getModelFile(modelInfo)?.absolutePath
    }

    /**
     * Get the models directory path
     */
    fun getModelsDirectory(): File = modelsDir

    private fun verifyChecksum(file: File, expectedSha256: String): Boolean {
        if (expectedSha256.startsWith("placeholder")) {
            Timber.w("Skipping checksum verification (placeholder hash)")
            return true
        }

        try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytes: Int
                while (input.read(buffer).also { bytes = it } != -1) {
                    digest.update(buffer, 0, bytes)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            return hash.equals(expectedSha256, ignoreCase = true)
        } catch (e: Exception) {
            Timber.e(e, "Error verifying checksum")
            return false
        }
    }
}
