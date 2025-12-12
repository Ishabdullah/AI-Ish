/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.device

import android.os.Build
import timber.log.Timber

/**
 * DeviceAllocationManager - Orchestrates compute resource allocation
 *
 * Samsung S24 Ultra Hardware:
 * - CPU: Snapdragon 8 Gen 3 (8 cores: 1x Cortex-X4 @ 3.3GHz, 3x A720 @ 3.2GHz, 4x A520 @ 2.3GHz)
 * - NPU: NNAPI delegate (hardware-agnostic, uses Hexagon on Snapdragon)
 * - GPU: Adreno 750 (Vulkan support enabled for GGML backends)
 *
 * Allocation Strategy:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ CPU (llama.cpp with ARM NEON)                               │
 * │ - LLM inference (Mistral-7B) - full inference on CPU        │
 * │ - BGE embeddings (async, FP16/INT8)                         │
 * │ - Small auxiliary tasks                                     │
 * └─────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │ NPU via NNAPI (TFLite delegate)                             │
 * │ - MobileNet-v3 vision inference (TFLite INT8)               │
 * │ - CNN models optimized for NNAPI                            │
 * │ - Note: NNAPI not suited for transformer (LLM) models       │
 * └─────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │ GPU Adreno 750 (Vulkan backend)                             │
 * │ - Available for GGML acceleration (llama.cpp/whisper.cpp)   │
 * │ - Fallback when NPU unavailable                             │
 * │ - Can be used alongside NPU for parallel workloads          │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Concurrent Execution:
 * - Vision models on NPU (NNAPI), LLM on CPU (llama.cpp)
 * - Memory: ~4.5GB total (Mistral 3.5GB + MobileNet 0.5GB + BGE 0.3GB + overhead)
 */
class DeviceAllocationManager {

    enum class DeviceType {
        CPU,      // Snapdragon 8 Gen 3 cores
        NPU,      // NNAPI delegate (Hexagon, Exynos, etc.)
        GPU,      // Adreno 750 (Vulkan backend)
        AUTO      // Automatic selection based on capabilities
    }

    enum class ModelType {
        @Deprecated("LLM uses CPU-only via llama.cpp")
        LLM_PREFILL,       // Deprecated - LLM now fully on CPU
        LLM_DECODE,        // Mistral-7B decode on CPU (now full inference)
        VISION,            // MobileNet-v3 on NPU (NNAPI)
        EMBEDDING          // BGE on CPU
    }

    data class DeviceInfo(
        val type: DeviceType,
        val name: String,
        val isAvailable: Boolean,
        val memoryMB: Int,
        val computeTOPS: Int  // INT8 TOPS
    )

    data class AllocationResult(
        val modelType: ModelType,
        val assignedDevice: DeviceType,
        val cpuCores: List<Int> = emptyList(),  // For CPU: specific cores to use
        val useNPUFusedKernels: Boolean = false,
        val usePreallocatedBuffers: Boolean = false,
        val estimatedMemoryMB: Int = 0
    )

    companion object {
        // CPU core allocation for S24 Ultra
        private val CPU_CORES_EFFICIENCY = listOf(0, 1, 2, 3)  // A520 cores for BGE
        private val CPU_CORES_PERFORMANCE = listOf(4, 5, 6)     // A720 cores (reserved)
        private val CPU_CORE_PRIME = 7                          // X4 core (reserved)

        // Memory budgets (INT8 quantized)
        private const val MEMORY_MISTRAL_7B_INT8 = 3500  // MB
        private const val MEMORY_MOBILENET_V3_INT8 = 500  // MB
        private const val MEMORY_BGE_FP16 = 300           // MB
        private const val MEMORY_OVERHEAD = 500           // MB (buffers, context)
        private const val TOTAL_MEMORY_BUDGET = 5000      // MB (safe limit for 12GB device)

        // Performance targets
        private const val NPU_TOPS_NNAPI = 45      // INT8 TOPS (varies by device)
    }

    /**
     * Detect available compute devices
     */
    fun detectDevices(): List<DeviceInfo> {
        val devices = mutableListOf<DeviceInfo>()

        // CPU (always available)
        devices.add(DeviceInfo(
            type = DeviceType.CPU,
            name = "Snapdragon 8 Gen 3 (8 cores)",
            isAvailable = true,
            memoryMB = 12288,  // 12GB RAM
            computeTOPS = 0    // N/A for CPU
        ))

        // NPU (NNAPI delegate - hardware varies by device)
        val hasNPU = detectNNAPINPU()
        devices.add(DeviceInfo(
            type = DeviceType.NPU,
            name = "NPU (NNAPI delegate)",
            isAvailable = hasNPU,
            memoryMB = 4096,   // Shared with system
            computeTOPS = NPU_TOPS_NNAPI
        ))

        // GPU (Adreno 750 - Vulkan backend enabled)
        devices.add(DeviceInfo(
            type = DeviceType.GPU,
            name = "Adreno 750 (Vulkan)",
            isAvailable = true,
            memoryMB = 2048,   // GPU can use shared memory
            computeTOPS = 0    // TFLOPS for GPU, not TOPS
        ))

        Timber.i("Detected devices: ${devices.map { "${it.name} (available=${it.isAvailable})" }}")
        return devices
    }

    /**
     * Allocate device for model
     */
    fun allocateDevice(modelType: ModelType): AllocationResult {
        return when (modelType) {
            ModelType.LLM_PREFILL -> AllocationResult(
                modelType = modelType,
                assignedDevice = DeviceType.CPU,  // LLM now fully on CPU
                cpuCores = CPU_CORES_EFFICIENCY,
                useNPUFusedKernels = false,
                usePreallocatedBuffers = true,
                estimatedMemoryMB = MEMORY_MISTRAL_7B_INT8
            ).also {
                Timber.w("⚠️ LLM_PREFILL is deprecated - LLM uses CPU via llama.cpp")
                Timber.i("✅ LLM → CPU (llama.cpp with NEON, ${MEMORY_MISTRAL_7B_INT8}MB)")
            }

            ModelType.LLM_DECODE -> AllocationResult(
                modelType = modelType,
                assignedDevice = DeviceType.CPU,
                cpuCores = CPU_CORES_EFFICIENCY,  // Use efficiency cores for decode
                useNPUFusedKernels = false,
                usePreallocatedBuffers = true,
                estimatedMemoryMB = 200  // Small memory for decode state
            ).also {
                Timber.i("✅ LLM_DECODE → CPU cores ${CPU_CORES_EFFICIENCY} (streaming)")
            }

            ModelType.VISION -> AllocationResult(
                modelType = modelType,
                assignedDevice = DeviceType.NPU,
                cpuCores = emptyList(),
                useNPUFusedKernels = true,
                usePreallocatedBuffers = true,
                estimatedMemoryMB = MEMORY_MOBILENET_V3_INT8
            ).also {
                Timber.i("✅ VISION → NPU (NNAPI delegate, ${MEMORY_MOBILENET_V3_INT8}MB)")
            }

            ModelType.EMBEDDING -> AllocationResult(
                modelType = modelType,
                assignedDevice = DeviceType.CPU,
                cpuCores = CPU_CORES_EFFICIENCY,  // Async on efficiency cores
                useNPUFusedKernels = false,
                usePreallocatedBuffers = true,
                estimatedMemoryMB = MEMORY_BGE_FP16
            ).also {
                Timber.i("✅ EMBEDDING → CPU cores ${CPU_CORES_EFFICIENCY} (async, ${MEMORY_BGE_FP16}MB)")
            }
        }
    }

    /**
     * Check if memory budget allows concurrent execution
     */
    fun checkConcurrentMemoryBudget(): Boolean {
        val totalMemory = MEMORY_MISTRAL_7B_INT8 +
                         MEMORY_MOBILENET_V3_INT8 +
                         MEMORY_BGE_FP16 +
                         MEMORY_OVERHEAD

        val withinBudget = totalMemory <= TOTAL_MEMORY_BUDGET

        if (withinBudget) {
            Timber.i("✅ Memory budget OK: ${totalMemory}MB / ${TOTAL_MEMORY_BUDGET}MB")
        } else {
            Timber.e("❌ Memory budget exceeded: ${totalMemory}MB > ${TOTAL_MEMORY_BUDGET}MB")
        }

        return withinBudget
    }

    /**
     * Get CPU affinity mask for cores
     */
    fun getCPUAffinityMask(cores: List<Int>): Long {
        var mask = 0L
        cores.forEach { core ->
            mask = mask or (1L shl core)
        }
        return mask
    }

    /**
     * Detect NPU via NNAPI delegate support
     *
     * NNAPI requires Android 8.1+ (API 27) and works with various NPUs:
     * - Qualcomm Hexagon (Snapdragon)
     * - Samsung Exynos NPU
     * - MediaTek APU (Dimensity)
     * - Google Tensor TPU
     */
    private fun detectNNAPINPU(): Boolean {
        try {
            // NNAPI requires API 27+
            if (Build.VERSION.SDK_INT < 27) {
                Timber.w("NNAPI requires Android 8.1+ (API 27), device has API ${Build.VERSION.SDK_INT}")
                return false
            }

            // Check device model and SoC for known NPU support
            val model = Build.MODEL
            val soc = Build.HARDWARE
            val board = Build.BOARD

            // Snapdragon devices (Hexagon NPU)
            val hasSnapdragon = soc.contains("qcom", ignoreCase = true) ||
                               board.contains("pineapple", ignoreCase = true) ||  // SD8G3
                               board.contains("kalama", ignoreCase = true) ||      // SD8G2
                               board.contains("taro", ignoreCase = true)           // SD8G1

            // Samsung Exynos (NPU)
            val hasExynos = soc.contains("exynos", ignoreCase = true)

            // MediaTek Dimensity (APU)
            val hasDimensity = soc.contains("mt68", ignoreCase = true) ||
                              soc.contains("mt69", ignoreCase = true)

            // Google Tensor
            val hasTensor = soc.contains("tensor", ignoreCase = true)

            val hasNPU = hasSnapdragon || hasExynos || hasDimensity || hasTensor

            if (hasNPU) {
                Timber.i("✅ NPU detected via NNAPI (device: $model, soc: $soc)")
            } else {
                Timber.w("⚠️ No known NPU detected (device: $model, soc: $soc) - will use CPU fallback")
            }

            return hasNPU

        } catch (e: Exception) {
            Timber.e(e, "Error detecting NPU")
            return false
        }
    }

    /**
     * Get allocation summary
     */
    fun getAllocationSummary(): String {
        return """
        |═══════════════════════════════════════════════════════════════
        | AI-Ish Device Allocation Summary
        |═══════════════════════════════════════════════════════════════
        |
        | CPU (llama.cpp with ARM NEON):
        |   ├─ Mistral-7B LLM (${MEMORY_MISTRAL_7B_INT8}MB) - full inference
        |   ├─ BGE Embeddings (${MEMORY_BGE_FP16}MB)
        |   └─ Auxiliary tasks
        |
        | NPU (NNAPI delegate):
        |   └─ MobileNet-v3 Vision (TFLite INT8, ${MEMORY_MOBILENET_V3_INT8}MB)
        |
        | GPU (Adreno/Mali/Tensor, Vulkan):
        |   └─ Available for GGML acceleration (fallback/parallel)
        |
        | Memory Budget:
        |   Total: ${MEMORY_MISTRAL_7B_INT8 + MEMORY_MOBILENET_V3_INT8 + MEMORY_BGE_FP16 + MEMORY_OVERHEAD}MB / ${TOTAL_MEMORY_BUDGET}MB
        |   ├─ Mistral-7B: ${MEMORY_MISTRAL_7B_INT8}MB (CPU)
        |   ├─ MobileNet-v3: ${MEMORY_MOBILENET_V3_INT8}MB (NPU)
        |   ├─ BGE: ${MEMORY_BGE_FP16}MB (CPU)
        |   └─ Overhead: ${MEMORY_OVERHEAD}MB
        |
        | Concurrent Execution: ✅ ENABLED
        |   LLM on CPU, Vision on NPU (NNAPI)
        |═══════════════════════════════════════════════════════════════
        """.trimMargin()
    }
}
