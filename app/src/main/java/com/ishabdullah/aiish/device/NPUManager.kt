/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.device

import timber.log.Timber
import java.io.File

/**
 * NPUManager - Qualcomm Hexagon v81 NPU interface
 *
 * Hexagon v81 Specifications (S24 Ultra):
 * - 45 TOPS INT8 performance
 * - Dedicated tensor accelerator
 * - Optimized for INT8 quantized models
 * - Supports fused kernels and preallocated buffers
 *
 * Assigned Models:
 * 1. Mistral-7B INT8 (prefill only)
 * 2. MobileNet-v3 INT8 (full inference)
 *
 * Optimization Techniques:
 * - Fused kernels: Combine ops (MatMul+Add+ReLU) into single kernel
 * - Preallocated buffers: Reuse memory across inferences
 * - INT8 quantization: 4x memory reduction + 4x speed increase
 * - Asynchronous execution: Overlap compute with memory transfers
 */
class NPUManager {

    companion object {
        init {
            try {
                System.loadLibrary("aiish_native")
                Timber.i("NPUManager: Native library loaded")
            } catch (e: UnsatisfiedLinkError) {
                Timber.e(e, "Failed to load native library for NPU")
            }
        }

        // NPU configuration
        private const val NPU_HEXAGON_V81 = "hexagon_v81"
        private const val BUFFER_POOL_SIZE = 10  // Preallocated buffer pool
    }

    private var isNPUAvailable = false
    private var isInitialized = false

    // Native methods for Hexagon NPU
    private external fun nativeDetectHexagonNPU(): Boolean
    private external fun nativeInitializeNPU(): Boolean
    private external fun nativeLoadModelToNPU(
        modelPath: String,
        modelType: String,  // "llm_prefill" or "vision"
        useFusedKernels: Boolean,
        usePreallocatedBuffers: Boolean,
        bufferPoolSize: Int
    ): Boolean
    private external fun nativeGetNPUInfo(): String
    private external fun nativeReleaseNPU()

    /**
     * Initialize NPU
     */
    fun initialize(): Boolean {
        try {
            if (isInitialized) {
                Timber.d("NPU already initialized")
                return true
            }

            // Detect NPU
            isNPUAvailable = nativeDetectHexagonNPU()
            if (!isNPUAvailable) {
                Timber.w("Hexagon v81 NPU not available on this device")
                return false
            }

            // Initialize NPU runtime
            val success = nativeInitializeNPU()
            if (success) {
                isInitialized = true
                val npuInfo = nativeGetNPUInfo()
                Timber.i("✅ NPU initialized: $npuInfo")
            } else {
                Timber.e("Failed to initialize NPU runtime")
            }

            return success

        } catch (e: Exception) {
            Timber.e(e, "Error initializing NPU")
            return false
        }
    }

    /**
     * Load model to NPU with optimization
     */
    fun loadModel(
        modelFile: File,
        modelType: NPUModelType,
        useFusedKernels: Boolean = true,
        usePreallocatedBuffers: Boolean = true
    ): Boolean {
        try {
            if (!isInitialized) {
                Timber.e("NPU not initialized")
                return false
            }

            if (!modelFile.exists()) {
                Timber.e("Model file not found: ${modelFile.absolutePath}")
                return false
            }

            Timber.i("Loading model to NPU: ${modelFile.name} (type=${modelType.name})")

            val success = nativeLoadModelToNPU(
                modelPath = modelFile.absolutePath,
                modelType = modelType.name.lowercase(),
                useFusedKernels = useFusedKernels,
                usePreallocatedBuffers = usePreallocatedBuffers,
                bufferPoolSize = BUFFER_POOL_SIZE
            )

            if (success) {
                Timber.i("✅ Model loaded to NPU successfully")
                Timber.i("   - Fused kernels: ${if (useFusedKernels) "ENABLED" else "DISABLED"}")
                Timber.i("   - Preallocated buffers: ${if (usePreallocatedBuffers) "ENABLED ($BUFFER_POOL_SIZE buffers)" else "DISABLED"}")
            } else {
                Timber.e("Failed to load model to NPU")
            }

            return success

        } catch (e: Exception) {
            Timber.e(e, "Error loading model to NPU")
            return false
        }
    }

    /**
     * Check if NPU is available
     */
    fun isAvailable(): Boolean = isNPUAvailable

    /**
     * Check if NPU is initialized
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * Get NPU information
     */
    fun getNPUInfo(): String {
        return try {
            if (isInitialized) {
                nativeGetNPUInfo()
            } else {
                "NPU not initialized"
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting NPU info")
            "Error: ${e.message}"
        }
    }

    /**
     * Release NPU resources
     */
    fun release() {
        try {
            if (isInitialized) {
                nativeReleaseNPU()
                isInitialized = false
                Timber.i("NPU released")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error releasing NPU")
        }
    }

    /**
     * NPU model types
     */
    enum class NPUModelType {
        LLM_PREFILL,    // Mistral-7B prefill stage
        VISION          // MobileNet-v3 full inference
    }
}
