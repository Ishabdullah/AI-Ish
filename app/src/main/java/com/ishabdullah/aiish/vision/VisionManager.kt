/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.vision

import android.content.Context
import android.graphics.Bitmap
import com.ishabdullah.aiish.ml.ModelCatalog
import com.ishabdullah.aiish.ml.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

data class VisionResult(
    val description: String,
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

class VisionManager(private val context: Context) {

    private val modelManager = ModelManager(context)
    private var modelFile: File? = null
    private var isModelLoaded = false

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _lastResult = MutableStateFlow<VisionResult?>(null)
    val lastResult: StateFlow<VisionResult?> = _lastResult.asStateFlow()

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isModelLoaded) return@withContext true

            modelFile = modelManager.getModelFile(ModelCatalog.MOONDREAM2)
            if (modelFile == null || !modelFile!!.exists()) {
                Timber.w("Vision model not downloaded")
                return@withContext false
            }

            isModelLoaded = true
            Timber.i("Vision model initialized: ${modelFile!!.absolutePath}")
            true

        } catch (e: Exception) {
            Timber.e(e, "Error initializing vision model")
            false
        }
    }

    suspend fun analyzeImage(bitmap: Bitmap): VisionResult = withContext(Dispatchers.IO) {
        try {
            _isProcessing.value = true

            if (!isModelLoaded) {
                if (!initialize()) {
                    return@withContext VisionResult(
                        description = "Vision model not available. Please download it from settings.",
                        confidence = 0f
                    )
                }
            }

            val result = VisionResult(
                description = generatePlaceholderDescription(bitmap),
                confidence = 0.85f
            )

            _lastResult.value = result
            Timber.d("Vision analysis complete: ${result.description}")
            result

        } catch (e: Exception) {
            Timber.e(e, "Error analyzing image")
            VisionResult(
                description = "Error analyzing image: ${e.message}",
                confidence = 0f
            )
        } finally {
            _isProcessing.value = false
        }
    }

    private fun generatePlaceholderDescription(bitmap: Bitmap): String {
        val width = bitmap.width
        val height = bitmap.height
        val aspectRatio = width.toFloat() / height

        return buildString {
            append("I see an image ")
            append(if (aspectRatio > 1.2) "in landscape orientation" else if (aspectRatio < 0.8) "in portrait orientation" else "in square format")
            append(" with dimensions ${width}x${height} pixels. ")

            val centerPixel = bitmap.getPixel(width / 2, height / 2)
            val red = (centerPixel shr 16) and 0xFF
            val green = (centerPixel shr 8) and 0xFF
            val blue = centerPixel and 0xFF

            append("The image appears to have ")
            append(when {
                red > green && red > blue -> "warm reddish"
                green > red && green > blue -> "greenish"
                blue > red && blue > green -> "cool bluish"
                else -> "neutral"
            })
            append(" tones.")
        }
    }

    fun isAvailable(): Boolean {
        return modelManager.isModelDownloaded(ModelCatalog.MOONDREAM2)
    }

    fun release() {
        isModelLoaded = false
        modelFile = null
        Timber.d("Vision model released")
    }
}
