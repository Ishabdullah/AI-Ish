/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.ml

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * GPU Manager for detecting and configuring GPU acceleration
 *
 * Supports:
 * - Adreno 750 (Snapdragon 8 Gen 3) - Primary target
 * - Adreno 740 (Snapdragon 8 Gen 2)
 * - Adreno 730 (Snapdragon 8 Gen 1)
 * - Mali GPUs (limited support)
 *
 * Uses OpenCL backend for GPU inference acceleration
 */
class GPUManager {

    data class GPUInfo(
        val isAvailable: Boolean,
        val vendor: String,
        val renderer: String,
        val version: String,
        val computeUnits: Int,
        val recommendedLayers: Int,
        val supportsOpenCL: Boolean
    )

    companion object {
        init {
            try {
                System.loadLibrary("aiish_native")
                Timber.i("GPUManager: Native library loaded")
            } catch (e: UnsatisfiedLinkError) {
                Timber.e(e, "GPUManager: Failed to load native library")
            }
        }

        // Adreno GPU detection patterns
        private val ADRENO_PATTERNS = mapOf(
            "Adreno (TM) 750" to GPUProfile(
                name = "Adreno 750",
                computeUnits = 12,
                recommendedLayers = 35, // For 7B models
                maxLayers = 40,
                isHighEnd = true
            ),
            "Adreno (TM) 740" to GPUProfile(
                name = "Adreno 740",
                computeUnits = 10,
                recommendedLayers = 30,
                maxLayers = 35,
                isHighEnd = true
            ),
            "Adreno (TM) 730" to GPUProfile(
                name = "Adreno 730",
                computeUnits = 8,
                recommendedLayers = 25,
                maxLayers = 30,
                isHighEnd = true
            ),
            "Adreno (TM) 725" to GPUProfile(
                name = "Adreno 725",
                computeUnits = 6,
                recommendedLayers = 20,
                maxLayers = 25,
                isHighEnd = false
            ),
            "Adreno (TM) 710" to GPUProfile(
                name = "Adreno 710",
                computeUnits = 6,
                recommendedLayers = 15,
                maxLayers = 20,
                isHighEnd = false
            )
        )

        // Mali GPU detection patterns
        private val MALI_PATTERNS = mapOf(
            "Mali-G715" to GPUProfile(
                name = "Mali-G715",
                computeUnits = 11,
                recommendedLayers = 25,
                maxLayers = 30,
                isHighEnd = true
            ),
            "Mali-G710" to GPUProfile(
                name = "Mali-G710",
                computeUnits = 10,
                recommendedLayers = 20,
                maxLayers = 25,
                isHighEnd = true
            )
        )
    }

    data class GPUProfile(
        val name: String,
        val computeUnits: Int,
        val recommendedLayers: Int,
        val maxLayers: Int,
        val isHighEnd: Boolean
    )

    // Native methods for GPU detection
    private external fun nativeIsGPUAvailable(): Boolean
    private external fun nativeGetGPUVendor(): String
    private external fun nativeGetGPURenderer(): String
    private external fun nativeGetGPUVersion(): String
    private external fun nativeGetComputeUnits(): Int
    private external fun nativeSupportsOpenCL(): Boolean
    private external fun nativeInitOpenCL(): Int
    private external fun nativeCleanupOpenCL()

    /**
     * Detect GPU capabilities
     */
    suspend fun detectGPU(): GPUInfo = withContext(Dispatchers.IO) {
        try {
            Timber.i("Detecting GPU capabilities...")

            val isAvailable = nativeIsGPUAvailable()
            if (!isAvailable) {
                Timber.w("GPU not available")
                return@withContext GPUInfo(
                    isAvailable = false,
                    vendor = "Unknown",
                    renderer = "Unknown",
                    version = "Unknown",
                    computeUnits = 0,
                    recommendedLayers = 0,
                    supportsOpenCL = false
                )
            }

            val vendor = nativeGetGPUVendor()
            val renderer = nativeGetGPURenderer()
            val version = nativeGetGPUVersion()
            val computeUnits = nativeGetComputeUnits()
            val supportsOpenCL = nativeSupportsOpenCL()

            // Determine recommended layers based on GPU
            val profile = detectGPUProfile(renderer)
            val recommendedLayers = profile?.recommendedLayers ?: getRecommendedLayersGeneric(computeUnits)

            Timber.i("GPU detected: $vendor $renderer (CUs: $computeUnits, OpenCL: $supportsOpenCL)")
            Timber.i("Recommended layers: $recommendedLayers")

            GPUInfo(
                isAvailable = true,
                vendor = vendor,
                renderer = renderer,
                version = version,
                computeUnits = computeUnits,
                recommendedLayers = recommendedLayers,
                supportsOpenCL = supportsOpenCL
            )

        } catch (e: Exception) {
            Timber.e(e, "Error detecting GPU")
            GPUInfo(
                isAvailable = false,
                vendor = "Error",
                renderer = "Error",
                version = "Error",
                computeUnits = 0,
                recommendedLayers = 0,
                supportsOpenCL = false
            )
        }
    }

    /**
     * Detect GPU profile from renderer string
     */
    private fun detectGPUProfile(renderer: String): GPUProfile? {
        // Check Adreno GPUs
        ADRENO_PATTERNS.entries.forEach { (pattern, profile) ->
            if (renderer.contains(pattern, ignoreCase = true)) {
                Timber.i("Detected Adreno GPU: ${profile.name}")
                return profile
            }
        }

        // Check Mali GPUs
        MALI_PATTERNS.entries.forEach { (pattern, profile) ->
            if (renderer.contains(pattern, ignoreCase = true)) {
                Timber.i("Detected Mali GPU: ${profile.name}")
                return profile
            }
        }

        // Fallback for unknown Adreno
        if (renderer.contains("Adreno", ignoreCase = true)) {
            Timber.w("Unknown Adreno GPU: $renderer, using conservative settings")
            return GPUProfile(
                name = renderer,
                computeUnits = 6,
                recommendedLayers = 15,
                maxLayers = 20,
                isHighEnd = false
            )
        }

        // Fallback for unknown Mali
        if (renderer.contains("Mali", ignoreCase = true)) {
            Timber.w("Unknown Mali GPU: $renderer, using conservative settings")
            return GPUProfile(
                name = renderer,
                computeUnits = 6,
                recommendedLayers = 15,
                maxLayers = 20,
                isHighEnd = false
            )
        }

        return null
    }

    /**
     * Get recommended layers based on compute units (generic fallback)
     */
    private fun getRecommendedLayersGeneric(computeUnits: Int): Int {
        return when {
            computeUnits >= 12 -> 35
            computeUnits >= 10 -> 30
            computeUnits >= 8 -> 25
            computeUnits >= 6 -> 20
            computeUnits >= 4 -> 15
            else -> 10
        }
    }

    /**
     * Check if device is high-end for GPU acceleration
     */
    fun isHighEndDevice(): Boolean {
        return when {
            // Snapdragon 8 Gen 3
            Build.MODEL.contains("SM-S928", ignoreCase = true) -> true // S24 Ultra
            Build.MODEL.contains("SM-S926", ignoreCase = true) -> true // S24+
            Build.MODEL.contains("SM-S921", ignoreCase = true) -> true // S24

            // Snapdragon 8 Gen 2
            Build.MODEL.contains("SM-S918", ignoreCase = true) -> true // S23 Ultra
            Build.MODEL.contains("SM-S916", ignoreCase = true) -> true // S23+

            // Add more high-end models as needed
            else -> false
        }
    }

    /**
     * Get recommended GPU layers for a specific model size
     */
    fun getRecommendedLayers(modelSizeGB: Float, gpuInfo: GPUInfo): Int {
        if (!gpuInfo.isAvailable || !gpuInfo.supportsOpenCL) {
            return 0 // CPU only
        }

        // Scale layers based on model size
        return when {
            modelSizeGB <= 1.0 -> gpuInfo.recommendedLayers // Small models (360M-1.5B)
            modelSizeGB <= 3.0 -> (gpuInfo.recommendedLayers * 0.9).toInt() // Medium (2B-3B)
            modelSizeGB <= 5.0 -> (gpuInfo.recommendedLayers * 0.8).toInt() // Large (7B)
            else -> (gpuInfo.recommendedLayers * 0.6).toInt() // Very large (13B+)
        }
    }

    /**
     * Check if GPU acceleration is recommended for this device
     */
    suspend fun shouldUseGPU(): Boolean {
        val gpuInfo = detectGPU()
        return gpuInfo.isAvailable &&
               gpuInfo.supportsOpenCL &&
               gpuInfo.recommendedLayers > 0
    }

    /**
     * Initialize OpenCL for GPU inference
     * Call before loading model with GPU layers
     */
    fun initOpenCL(): Boolean {
        try {
            val result = nativeInitOpenCL()
            if (result == 0) {
                Timber.i("OpenCL initialized successfully")
                return true
            } else {
                Timber.e("OpenCL initialization failed: $result")
                return false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize OpenCL")
            return false
        }
    }

    /**
     * Cleanup OpenCL resources
     */
    fun cleanup() {
        try {
            nativeCleanupOpenCL()
            Timber.i("OpenCL cleanup complete")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup OpenCL")
        }
    }
}
