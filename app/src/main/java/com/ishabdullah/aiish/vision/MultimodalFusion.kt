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

/**
 * MultimodalFusion - Combine text and vision modalities
 *
 * 5-Stage Pipeline (from AILive):
 * 1. Query Analysis - Determine if vision is needed
 * 2. Image Processing - Preprocess image if available
 * 3. Vision Inference - Generate image description
 * 4. Context Fusion - Combine image description with text query
 * 5. Response Generation - Generate final answer
 *
 * Example:
 * Query: "What's in this image?"
 * → Vision needed: YES
 * → Image description: "A cat sitting on a couch"
 * → Fused context: "The image shows: A cat sitting on a couch. Question: What's in this image?"
 * → Final response: "I can see a cat sitting on a couch."
 */
class MultimodalFusion(
    private val visionEngine: VisionInferenceEngine
) {

    /**
     * Query types that require vision
     */
    private val visionKeywords = listOf(
        "image", "picture", "photo", "see", "look", "show", "visible",
        "what's in", "what is in", "describe", "identify", "detect",
        "read this", "ocr", "text in"
    )

    /**
     * Process multimodal query
     */
    suspend fun processQuery(
        query: String,
        image: Bitmap? = null,
        textResponseGenerator: suspend (String) -> String
    ): MultimodalResponse {
        val startTime = System.currentTimeMillis()

        try {
            // Stage 1: Query Analysis
            val needsVision = requiresVision(query)
            Timber.d("Query analysis: needsVision=$needsVision")

            // Stage 2: Vision Processing (if needed and available)
            var visionResult: VisionInferenceResult? = null
            if (needsVision && image != null && visionEngine.isLoaded()) {
                visionResult = processVision(query, image)
                Timber.d("Vision result: ${visionResult.description.take(100)}")
            }

            // Stage 3: Context Fusion
            val fusedContext = fuseContext(query, visionResult)
            Timber.d("Fused context: ${fusedContext.take(150)}")

            // Stage 4: Response Generation
            val finalResponse = textResponseGenerator(fusedContext)

            val totalTime = System.currentTimeMillis() - startTime

            return MultimodalResponse(
                success = true,
                response = finalResponse,
                usedVision = visionResult != null,
                visionDescription = visionResult?.description,
                processingTimeMs = totalTime,
                error = null
            )

        } catch (e: Exception) {
            Timber.e(e, "Error in multimodal fusion")
            return MultimodalResponse(
                success = false,
                response = "Error processing multimodal query",
                usedVision = false,
                visionDescription = null,
                processingTimeMs = 0,
                error = e.message
            )
        }
    }

    /**
     * Process image with vision model
     */
    private fun processVision(query: String, image: Bitmap): VisionInferenceResult {
        // Determine vision task type
        val task = detectVisionTask(query)

        return when (task) {
            VisionTask.DESCRIBE -> visionEngine.describeImage(image)
            VisionTask.QUESTION_ANSWER -> visionEngine.answerQuestion(image, query)
            VisionTask.OBJECT_DETECTION -> visionEngine.detectObjects(image)
            VisionTask.TEXT_EXTRACTION -> visionEngine.extractText(image)
        }
    }

    /**
     * Detect what type of vision task is needed
     */
    private fun detectVisionTask(query: String): VisionTask {
        val lowerQuery = query.lowercase()

        return when {
            lowerQuery.contains("read") || lowerQuery.contains("text") || lowerQuery.contains("ocr") ->
                VisionTask.TEXT_EXTRACTION

            lowerQuery.contains("object") || lowerQuery.contains("detect") || lowerQuery.contains("find") ->
                VisionTask.OBJECT_DETECTION

            lowerQuery.contains("?") || lowerQuery.startsWith("what") || lowerQuery.startsWith("how") ->
                VisionTask.QUESTION_ANSWER

            else ->
                VisionTask.DESCRIBE
        }
    }

    /**
     * Determine if query requires vision processing
     */
    private fun requiresVision(query: String): Boolean {
        val lowerQuery = query.lowercase()
        return visionKeywords.any { lowerQuery.contains(it) }
    }

    /**
     * Fuse vision description with text query
     */
    private fun fuseContext(query: String, visionResult: VisionInferenceResult?): String {
        return if (visionResult != null && visionResult.success) {
            buildString {
                append("Image Context: ")
                append(visionResult.description)
                append("\n\n")

                if (visionResult.extractedText != null) {
                    append("Extracted Text: ")
                    append(visionResult.extractedText)
                    append("\n\n")
                }

                if (visionResult.detectedObjects.isNotEmpty()) {
                    append("Detected Objects: ")
                    append(visionResult.detectedObjects.joinToString(", "))
                    append("\n\n")
                }

                append("User Query: ")
                append(query)
            }
        } else {
            // Text-only fallback
            query
        }
    }

    /**
     * Generate text-only fallback when vision unavailable
     */
    fun generateVisionUnavailableFallback(query: String): String {
        return buildString {
            append("I cannot see the image right now. ")

            if (query.contains("?")) {
                append("Please ensure the vision model is downloaded and initialized. ")
                append("You can check model status in Settings.")
            } else {
                append("To analyze images, please download the Vision model from Settings first.")
            }
        }
    }

    /**
     * Vision task types
     */
    enum class VisionTask {
        DESCRIBE,           // General image description
        QUESTION_ANSWER,    // Answer specific questions about image
        OBJECT_DETECTION,   // List objects in image
        TEXT_EXTRACTION     // OCR - read text from image
    }
}

/**
 * Multimodal response
 */
data class MultimodalResponse(
    val success: Boolean,
    val response: String,
    val usedVision: Boolean,
    val visionDescription: String?,
    val processingTimeMs: Long,
    val error: String?
)
