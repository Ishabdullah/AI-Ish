/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.ml

/**
 * GPU configuration for model inference
 */
data class GPUConfig(
    val enabled: Boolean = true,
    val layers: Int = 0,
    val autoDetect: Boolean = true,
    val fallbackToCPU: Boolean = true
) {
    companion object {
        /**
         * Create GPU config with auto-detection
         */
        suspend fun createAuto(gpuManager: GPUManager, modelSizeGB: Float): GPUConfig {
            val gpuInfo = gpuManager.detectGPU()

            if (!gpuInfo.isAvailable || !gpuInfo.supportsOpenCL) {
                return GPUConfig(
                    enabled = false,
                    layers = 0,
                    autoDetect = true,
                    fallbackToCPU = true
                )
            }

            val recommendedLayers = gpuManager.getRecommendedLayers(modelSizeGB, gpuInfo)

            return GPUConfig(
                enabled = true,
                layers = recommendedLayers,
                autoDetect = true,
                fallbackToCPU = true
            )
        }

        /**
         * CPU-only configuration
         */
        fun cpuOnly() = GPUConfig(
            enabled = false,
            layers = 0,
            autoDetect = false,
            fallbackToCPU = true
        )

        /**
         * Maximum GPU acceleration
         */
        fun maxGPU(layers: Int = 35) = GPUConfig(
            enabled = true,
            layers = layers,
            autoDetect = false,
            fallbackToCPU = true
        )
    }

    /**
     * Get human-readable description
     */
    fun getDescription(): String {
        return when {
            !enabled -> "CPU Only"
            layers == 0 -> "CPU Only"
            layers < 20 -> "Hybrid Mode ($layers GPU layers)"
            else -> "GPU Accelerated ($layers layers)"
        }
    }

    /**
     * Estimate performance boost over CPU
     */
    fun getSpeedupEstimate(): Float {
        return when {
            layers == 0 -> 1.0f
            layers < 10 -> 1.5f
            layers < 20 -> 2.5f
            layers < 30 -> 3.5f
            else -> 4.5f
        }
    }
}
