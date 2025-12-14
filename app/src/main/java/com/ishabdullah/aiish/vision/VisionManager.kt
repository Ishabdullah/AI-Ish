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
import com.ishabdullah.aiish.device.DeviceAllocationManager
import com.ishabdullah.aiish.device.NPUManager
import com.ishabdullah.aiish.ml.ModelCatalog
import com.ishabdullah.aiish.ml.ModelManager
import com.ishabdullah.aiish.data.local.preferences.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Vision inference result
 */
data class VisionResult(
    val description: String,
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val processingTimeMs: Long = 0,
    val usedRealInference: Boolean = false,
    val fps: Float = 0f  // For production mode performance tracking
)

/**
 * VisionManager - MobileNet-v3 INT8 Production + Legacy Models
 *
 * Production Mode (MobileNet-v3 INT8 via TFLite + NNAPI):
 * - Device: NPU via TFLite NNAPI delegate
 * - Model: MobileNet-v3-Large INT8 (TFLite format)
 * - Memory: ~500MB
 * - Performance: ~30-60 FPS (device dependent)
 * - Features: Image classification, object detection
 * - Backend: TFLite Kotlin API with NNAPI delegate (Gradle dependency)
 *
 * Legacy Mode (Moondream2/Qwen2-VL):
 * - Device: CPU/GPU via llama.cpp
 * - Features: Full multimodal (VQA, descriptions, OCR)
 */
class VisionManager(private val context: Context, private val preferencesManager: PreferencesManager) {
    private var modelManager: ModelManager? = null
    private val visionEngine = VisionInferenceEngine()  // Legacy engine
    private var modelFile: File? = null
    private var isModelLoaded = false
    private var useProductionMode = false  // MobileNet-v3 on NPU

    // Device managers (production mode)
    private var npuManager: NPUManager? = null
    private var deviceAllocator: DeviceAllocationManager? = null

    // Performance tracking
    private var lastInferenceTimeMs = 0L
    private var lastFPS = 0f

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _lastResult = MutableStateFlow<VisionResult?>(null)
    val lastResult: StateFlow<VisionResult?> = _lastResult.asStateFlow()

    // Native methods for MobileNet-v3 on NPU
    private external fun nativeLoadMobileNetV3(
        modelPath: String,
        useNPU: Boolean,
        useFusedKernels: Boolean,
        usePreallocatedBuffers: Boolean
    ): Boolean

    private external fun nativeClassifyImage(
        bitmapPixels: IntArray,
        width: Int,
        height: Int,
        topK: Int
    ): Array<String>  // Returns ["label1:0.95", "label2:0.03", ...]

    private external fun nativeGetInferenceTimeMs(): Long
    private external fun nativeReleaseMobileNet()

    /**
     * Initialize MobileNet-v3 INT8 in PRODUCTION mode (NPU)
     */
    suspend fun initializeProduction(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isModelLoaded && useProductionMode) {
                return@withContext true
            }

            Timber.i("═══════════════════════════════════════════════════════════")
            Timber.i("Loading MobileNet-v3 INT8 (PRODUCTION MODE)")
            Timber.i("═══════════════════════════════════════════════════════════")

            // Determine storage directory for ModelManager
            val useExternalStorage = preferencesManager.useExternalStorage.first()
            val storageDir = if (useExternalStorage) {
                context.getExternalFilesDir(null) ?: context.filesDir
            } else {
                context.filesDir
            }
            modelManager = ModelManager(context, storageDir)

            // Determine storage directory for ModelManager
            val useExternalStorage = preferencesManager.useExternalStorage.first()
            val storageDir = if (useExternalStorage) {
                context.getExternalFilesDir(null) ?: context.filesDir
            } else {
                context.filesDir
            }
            modelManager = ModelManager(context, storageDir)

            modelFile = modelManager!!.getModelFile(ModelCatalog.MOBILENET_V3_INT8)
            if (modelFile == null || !modelFile!!.exists()) {
                Timber.w("MobileNet-v3 model not downloaded")
                return@withContext false
            }

            // Initialize device managers
            npuManager = NPUManager()
            deviceAllocator = DeviceAllocationManager()

            // Initialize NPU
            val npuInitialized = npuManager?.initialize() ?: false
            if (!npuInitialized) {
                Timber.w("NPU initialization failed, falling back to legacy mode")
                return@withContext initializeLegacy(0)
            }

            // Get allocation for vision model
            val allocation = deviceAllocator?.allocateDevice(
                DeviceAllocationManager.ModelType.VISION
            )

            Timber.i("Device allocation:")
            Timber.i("  └─ Vision: NPU (TFLite NNAPI delegate)")

            // Load MobileNet-v3 with NNAPI delegate
            val success = nativeLoadMobileNetV3(
                modelPath = modelFile!!.absolutePath,
                useNPU = true,
                useFusedKernels = allocation?.useNPUFusedKernels ?: true,
                usePreallocatedBuffers = allocation?.usePreallocatedBuffers ?: true
            )

            if (success) {
                isModelLoaded = true
                useProductionMode = true

                Timber.i("✅ MobileNet-v3 INT8 loaded successfully")
                Timber.i("   - Memory: ~500MB (INT8)")
                Timber.i("   - Mode: PRODUCTION (NNAPI)")
                Timber.i("   - Device: NPU via TFLite NNAPI delegate")
                Timber.i("   - Performance target: ~30-60 FPS")
                Timber.i("═══════════════════════════════════════════════════════════")
            } else {
                Timber.e("Failed to load MobileNet-v3 with NNAPI")
            }

            success

        } catch (e: Exception) {
            Timber.e(e, "Error initializing vision model in production mode")
            false
        }
    }

    /**
     * Initialize legacy vision model (Moondream2/Qwen2-VL)
     */
    suspend fun initialize(gpuLayers: Int = 0): Boolean = initializeLegacy(gpuLayers)

    private suspend fun initializeLegacy(gpuLayers: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isModelLoaded && visionEngine.isLoaded() && !useProductionMode) {
                return@withContext true
            }

            Timber.i("Loading vision model (LEGACY mode)")

            // Determine storage directory for ModelManager
            val useExternalStorage = preferencesManager.useExternalStorage.first()
            val storageDir = if (useExternalStorage) {
                context.getExternalFilesDir(null) ?: context.filesDir
            } else {
                context.filesDir
            }
            modelManager = ModelManager(context, storageDir)
            modelFile = modelManager!!.getModelFile(ModelCatalog.MOONDREAM2)
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
                useProductionMode = false
                Timber.i("Vision model initialized (LEGACY): ${modelFile!!.name} (GPU layers: $gpuLayers)")
            } else {
                Timber.e("Failed to load vision model into inference engine")
            }

            success

        } catch (e: Exception) {
            Timber.e(e, "Error initializing vision model")
            false
        }
    }

    /**
     * Analyze image using production (MobileNet-v3 NPU) or legacy mode
     */
    suspend fun analyzeImage(
        bitmap: Bitmap,
        prompt: String = "Describe this image in detail."
    ): VisionResult = withContext(Dispatchers.IO) {
        try {
            _isProcessing.value = true

            if (!isModelLoaded) {
                // Auto-initialize based on available models
                val productionAvailable = modelManager.isModelDownloaded(ModelCatalog.MOBILENET_V3_INT8)
                val initialized = if (productionAvailable) {
                    initializeProduction()
                } else {
                    initialize()
                }

                if (!initialized) {
                    return@withContext VisionResult(
                        description = "Vision model not available. Please download it from settings.",
                        confidence = 0f,
                        usedRealInference = false
                    )
                }
            }

            val result = if (useProductionMode) {
                analyzeImageProduction(bitmap)
            } else {
                analyzeImageLegacy(bitmap, prompt)
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
     * Production mode: MobileNet-v3 INT8 classification on NPU
     */
    private suspend fun analyzeImageProduction(bitmap: Bitmap): VisionResult = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()

            // Convert bitmap to pixel array
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // Run classification on NPU (returns top 5 predictions)
            val predictions = nativeClassifyImage(
                bitmapPixels = pixels,
                width = width,
                height = height,
                topK = 5
            )

            val processingTime = System.currentTimeMillis() - startTime
            lastInferenceTimeMs = nativeGetInferenceTimeMs()
            lastFPS = if (lastInferenceTimeMs > 0) {
                1000f / lastInferenceTimeMs
            } else {
                0f
            }

            // Parse predictions (format: "label:confidence")
            val topPrediction = predictions.firstOrNull()?.split(":") ?: listOf("unknown", "0")
            val label = topPrediction[0]
            val confidence = topPrediction.getOrNull(1)?.toFloatOrNull() ?: 0f

            // Build description from top predictions
            val description = buildString {
                append("I can see ")
                append(formatLabel(label))
                append(" (${(confidence * 100).toInt()}% confidence)")

                if (predictions.size > 1) {
                    append(". Also detected: ")
                    append(predictions.drop(1).take(2).joinToString(", ") {
                        val parts = it.split(":")
                        "${formatLabel(parts[0])} (${(parts.getOrNull(1)?.toFloatOrNull() ?: 0f * 100).toInt()}%)"
                    })
                }
            }

            VisionResult(
                description = description,
                confidence = confidence,
                processingTimeMs = lastInferenceTimeMs,
                usedRealInference = true,
                fps = lastFPS
            )

        } catch (e: Exception) {
            Timber.e(e, "Error in production vision inference")
            VisionResult(
                description = "Error analyzing image: ${e.message}",
                confidence = 0f,
                usedRealInference = false
            )
        }
    }

    /**
     * Legacy mode: Moondream2/Qwen2-VL inference
     */
    private suspend fun analyzeImageLegacy(bitmap: Bitmap, prompt: String): VisionResult {
        // Use real vision inference
        val inferenceResult = visionEngine.describeImage(
            bitmap = bitmap,
            prompt = prompt,
            maxTokens = 150,
            temperature = 0.7f
        )

        return if (inferenceResult.success) {
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
    }

    /**
     * Format ImageNet label (e.g., "golden_retriever" -> "a golden retriever")
     */
    private fun formatLabel(label: String): String {
        return "a " + label.replace("_", " ").lowercase()
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
        // Initialize modelManager if it hasn't been yet
        if (modelManager == null) {
            val useExternalStorage = runBlocking { preferencesManager.useExternalStorage.first() }
            val storageDir = if (useExternalStorage) {
                context.getExternalFilesDir(null) ?: context.filesDir
            } else {
                context.filesDir
            }
            modelManager = ModelManager(context, storageDir)
        }
        val production = modelManager!!.isModelDownloaded(ModelCatalog.MOBILENET_V3_INT8)
        val legacy = modelManager!!.isModelDownloaded(ModelCatalog.MOONDREAM2)
        return production || legacy
    }

    /**
     * Check if production mode is active
     */
    fun isProductionMode(): Boolean = useProductionMode

    /**
     * Get performance stats
     */
    fun getPerformanceStats(): String {
        return if (useProductionMode) {
            """
            |Performance Stats (Production Mode):
            |  - Inference time: ${lastInferenceTimeMs}ms
            |  - FPS: ${lastFPS.toInt()}
            |  - Device: NPU (TFLite NNAPI delegate)
            |  - Model: MobileNet-v3 INT8 (TFLite)
            """.trimMargin()
        } else {
            "Performance stats only available in production mode"
        }
    }

    /**
     * Get last FPS (production mode only)
     */
    fun getLastFPS(): Float = lastFPS

    /**
     * Get last inference time (production mode only)
     */
    fun getLastInferenceTimeMs(): Long = lastInferenceTimeMs

    fun release() {
        if (useProductionMode) {
            nativeReleaseMobileNet()
            npuManager?.release()
        } else {
            visionEngine.release()
        }

        isModelLoaded = false
        useProductionMode = false
        modelFile = null
        npuManager = null
        deviceAllocator = null

        Timber.d("Vision model released")
    }
}
