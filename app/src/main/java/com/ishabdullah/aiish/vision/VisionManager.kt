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
    val timestamp: Long = System.currentTimeMillis(),
    val processingTimeMs: Long = 0,
    val usedRealInference: Boolean = false
)

class VisionManager(private val context: Context) {

    private val modelManager = ModelManager(context)
    private val visionEngine = VisionInferenceEngine()
    private var modelFile: File? = null
    private var isModelLoaded = false

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _lastResult = MutableStateFlow<VisionResult?>(null)
    val lastResult: StateFlow<VisionResult?> = _lastResult.asStateFlow()

    suspend fun initialize(gpuLayers: Int = 0): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isModelLoaded && visionEngine.isLoaded()) {
                return@withContext true
            }

            modelFile = modelManager.getModelFile(ModelCatalog.MOONDREAM2)
            if (modelFile == null || !modelFile!!.exists()) {
                Timber.w("Vision model not downloaded")
                return@withContext false
            }

            // Load model into inference engine
            val success = visionEngine.loadModel(
                modelFile = modelFile!!,
                contextSize = 2048,
                gpuLayers = gpuLayers
            )

            if (success) {
                isModelLoaded = true
                Timber.i("Vision model initialized: ${modelFile!!.name} (GPU layers: $gpuLayers)")
            } else {
                Timber.e("Failed to load vision model into inference engine")
            }

            success

        } catch (e: Exception) {
            Timber.e(e, "Error initializing vision model")
            false
        }
    }

    suspend fun analyzeImage(
        bitmap: Bitmap,
        prompt: String = "Describe this image in detail."
    ): VisionResult = withContext(Dispatchers.IO) {
        try {
            _isProcessing.value = true

            if (!isModelLoaded || !visionEngine.isLoaded()) {
                if (!initialize()) {
                    return@withContext VisionResult(
                        description = "Vision model not available. Please download it from settings.",
                        confidence = 0f,
                        usedRealInference = false
                    )
                }
            }

            // Use real vision inference
            val inferenceResult = visionEngine.describeImage(
                bitmap = bitmap,
                prompt = prompt,
                maxTokens = 150,
                temperature = 0.7f
            )

            val result = if (inferenceResult.success) {
                VisionResult(
                    description = inferenceResult.description,
                    confidence = inferenceResult.confidence,
                    processingTimeMs = inferenceResult.processingTimeMs,
                    usedRealInference = true
                )
            } else {
                // Fallback to placeholder if inference fails
                Timber.w("Vision inference failed, using placeholder")
                VisionResult(
                    description = generatePlaceholderDescription(bitmap),
                    confidence = 0.5f,
                    usedRealInference = false
                )
            }

            _lastResult.value = result
            Timber.d("Vision analysis complete: ${result.description.take(100)}... (${result.processingTimeMs}ms)")
            result

        } catch (e: Exception) {
            Timber.e(e, "Error analyzing image")
            VisionResult(
                description = "Error analyzing image: ${e.message}",
                confidence = 0f,
                usedRealInference = false
            )
        } finally {
            _isProcessing.value = false
        }
    }

    /**
     * Answer a question about an image
     */
    suspend fun answerQuestion(bitmap: Bitmap, question: String): VisionResult = withContext(Dispatchers.IO) {
        try {
            _isProcessing.value = true

            if (!isModelLoaded || !visionEngine.isLoaded()) {
                if (!initialize()) {
                    return@withContext VisionResult(
                        description = "Vision model not available.",
                        confidence = 0f,
                        usedRealInference = false
                    )
                }
            }

            val inferenceResult = visionEngine.answerQuestion(bitmap, question)

            VisionResult(
                description = inferenceResult.description,
                confidence = inferenceResult.confidence,
                processingTimeMs = inferenceResult.processingTimeMs,
                usedRealInference = inferenceResult.success
            )

        } catch (e: Exception) {
            Timber.e(e, "Error answering question about image")
            VisionResult(
                description = "Error: ${e.message}",
                confidence = 0f,
                usedRealInference = false
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
        visionEngine.release()
        isModelLoaded = false
        modelFile = null
        Timber.d("Vision model released")
    }
}
