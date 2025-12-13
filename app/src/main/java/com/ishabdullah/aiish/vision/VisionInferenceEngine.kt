/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.vision

import android.graphics.Bitmap
import timber.log.Timber
import java.io.File

/**
 * VisionInferenceEngine - Run vision model inference
 *
 * Supports:
 * - Moondream2 (GGUF format via llama.cpp)
 * - Qwen2-VL (GGUF format via llama.cpp)
 *
 * Architecture:
 * - Uses existing llama.cpp JNI bridge (shared with text models)
 * - Vision models are multimodal - they take image embeddings + text prompt
 * - Image preprocessing → vision encoder → text decoder
 */
class VisionInferenceEngine {

    companion object {
        init {
            try {
                System.loadLibrary("aiish_native")
                Timber.i("VisionInferenceEngine: Native library loaded")
            } catch (e: UnsatisfiedLinkError) {
                Timber.e(e, "Failed to load native library for vision")
            }
        }
    }

    private var isModelLoaded = false
    private var currentModelPath: String? = null
    private val preprocessor = VisionPreprocessor(VisionPreprocessor.VisionModelType.MOONDREAM2)

    // Native methods (shared with LLMInferenceEngine)
    private external fun nativeLoadVisionModel(
        modelPath: String,
        contextSize: Int,
        gpuLayers: Int
    ): Boolean

    private external fun nativeEncodeImage(imageData: FloatArray): LongArray
    private external fun nativeGenerateFromImage(
        imageEmbeddings: LongArray,
        prompt: String,
        maxTokens: Int,
        temperature: Float
    ): String

    private external fun nativeReleaseVisionModel()

    /**
     * Load vision model from file
     */
    fun loadModel(
        modelFile: File,
        contextSize: Int = 4096,
        gpuLayers: Int = 0
    ): Boolean {
        return try {
            if (isModelLoaded && currentModelPath == modelFile.absolutePath) {
                Timber.d("Vision model already loaded: ${modelFile.name}")
                return true
            }

            if (!modelFile.exists()) {
                Timber.e("Vision model file not found: ${modelFile.absolutePath}")
                return false
            }

            Timber.i("Loading vision model: ${modelFile.name} (${modelFile.length() / 1024 / 1024}MB)")

            // Load model via JNI
            val success = nativeLoadVisionModel(
                modelPath = modelFile.absolutePath,
                contextSize = contextSize,
                gpuLayers = gpuLayers
            )

            if (success) {
                isModelLoaded = true
                currentModelPath = modelFile.absolutePath
                Timber.i("Vision model loaded successfully (ctx=$contextSize, gpu=$gpuLayers)")
            } else {
                Timber.e("Failed to load vision model")
            }

            success
        } catch (e: Exception) {
            Timber.e(e, "Error loading vision model")
            false
        }
    }

    /**
     * Generate description from image
     */
    fun describeImage(
        bitmap: Bitmap,
        prompt: String = "Describe this image in detail.",
        maxTokens: Int = 150,
        temperature: Float = 0.7f
    ): VisionInferenceResult {
        return try {
            if (!isModelLoaded) {
                return VisionInferenceResult(
                    success = false,
                    description = "Vision model not loaded",
                    confidence = 0f,
                    processingTimeMs = 0
                )
            }

            val startTime = System.currentTimeMillis()

            // Step 1: Preprocess image
            val preprocessedImage = preprocessor.preprocess(bitmap)
            Timber.d("Image preprocessed: ${preprocessedImage.size} floats")

            // Step 2: Encode image to embeddings
            val imageEmbeddings = nativeEncodeImage(preprocessedImage)
            Timber.d("Image encoded: ${imageEmbeddings.size} embeddings")

            // Step 3: Generate text from image + prompt
            val description = nativeGenerateFromImage(
                imageEmbeddings = imageEmbeddings,
                prompt = prompt,
                maxTokens = maxTokens,
                temperature = temperature
            )

            val processingTime = System.currentTimeMillis() - startTime

            VisionInferenceResult(
                success = true,
                description = description.trim(),
                confidence = 0.85f, // TODO: Extract from native model logits (requires native layer modification)
                processingTimeMs = processingTime
            )

        } catch (e: Exception) {
            Timber.e(e, "Error during vision inference")
            VisionInferenceResult(
                success = false,
                description = "Error: ${e.message}",
                confidence = 0f,
                processingTimeMs = 0
            )
        }
    }

    /**
     * Answer questions about an image
     */
    fun answerQuestion(
        bitmap: Bitmap,
        question: String,
        maxTokens: Int = 100,
        temperature: Float = 0.3f
    ): VisionInferenceResult {
        val prompt = "Question: $question\nAnswer:"
        return describeImage(bitmap, prompt, maxTokens, temperature)
    }

    /**
     * Detect objects in image
     */
    fun detectObjects(
        bitmap: Bitmap,
        maxTokens: Int = 80
    ): VisionInferenceResult {
        val prompt = "List all objects visible in this image:"
        return describeImage(bitmap, prompt, maxTokens, temperature = 0.3f)
    }

    /**
     * Extract text from image (OCR)
     */
    fun extractText(
        bitmap: Bitmap,
        maxTokens: Int = 200
    ): VisionInferenceResult {
        val prompt = "Extract all text visible in this image:"
        return describeImage(bitmap, prompt, maxTokens, temperature = 0.1f)
    }

    /**
     * Check if model is loaded
     */
    fun isLoaded(): Boolean = isModelLoaded

    /**
     * Get current model path
     */
    fun getModelPath(): String? = currentModelPath

    /**
     * Release model and free resources
     */
    fun release() {
        if (isModelLoaded) {
            try {
                nativeReleaseVisionModel()
                Timber.i("Vision model released")
            } catch (e: Exception) {
                Timber.e(e, "Error releasing vision model")
            }
        }
        isModelLoaded = false
        currentModelPath = null
    }
}

/**
 * Result of vision inference
 */
data class VisionInferenceResult(
    val success: Boolean,
    val description: String,
    val confidence: Float,
    val processingTimeMs: Long,
    val detectedObjects: List<String> = emptyList(),
    val extractedText: String? = null
)
