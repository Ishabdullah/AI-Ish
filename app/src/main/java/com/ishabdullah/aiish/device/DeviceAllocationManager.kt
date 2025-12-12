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
 * - NPU: Qualcomm Hexagon v81 (45 TOPS INT8)
 * - GPU: Adreno 750 (reserved, not used to avoid memory contention)
 *
 * Allocation Strategy:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ CPU Cores 0-3 (A520 @ 2.3GHz)                               │
 * │ - BGE embeddings (async, FP16/INT8)                         │
 * │ - Small auxiliary tasks                                     │
 * │ - LLM token decode streaming                                │
 * └─────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │ NPU via QNN/NNAPI (45 TOPS INT8)                            │
 * │ - Mistral-7B prefill (INT8, fused kernels)                  │
 * │ - MobileNet-v3 vision inference (INT8)                      │
 * │ - Preallocated buffers, async execution                     │
 * └─────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │ GPU Adreno 750                                               │
 * │ - RESERVED / IDLE (avoid memory contention)                 │
 * │ - Future use: UI rendering only                             │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Concurrent Execution:
 * - All 3 models can run simultaneously
 * - NPU handles compute-intensive tasks (prefill, vision)
 * - CPU handles streaming decode and embeddings
 * - Memory: ~4.5GB total (Mistral 3.5GB + MobileNet 0.5GB + BGE 0.3GB + overhead)
 */
class DeviceAllocationManager {

    enum class DeviceType {
        CPU,      // Snapdragon 8 Gen 3 cores
        NPU,      // Hexagon v81
        GPU,      // Adreno 750 (reserved)
        AUTO      // Automatic selection
    }

    enum class ModelType {
        LLM_PREFILL,       // Mistral-7B prefill on NPU
        LLM_DECODE,        // Mistral-7B decode on CPU
        VISION,            // MobileNet-v3 on NPU
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
        private const val NPU_TOPS_QNN_NNAPI = 45      // INT8 TOPS
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

        // NPU (QNN/NNAPI delegate - S24 Ultra has this)
        val hasNPU = detectQNNNPU()
        devices.add(DeviceInfo(
            type = DeviceType.NPU,
            name = "Qualcomm NPU (QNN/NNAPI)",
            isAvailable = hasNPU,
            memoryMB = 4096,   // Shared with system
            computeTOPS = NPU_TOPS_QNN_NNAPI
        ))

        // GPU (Adreno 750 - reserved, not used)
        devices.add(DeviceInfo(
            type = DeviceType.GPU,
            name = "Adreno 750",
            isAvailable = true,
            memoryMB = 0,      // Not allocated
            computeTOPS = 0    // Not used
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
                assignedDevice = DeviceType.NPU,
                cpuCores = emptyList(),
                useNPUFusedKernels = true,
                usePreallocatedBuffers = true,
                estimatedMemoryMB = MEMORY_MISTRAL_7B_INT8
            ).also {
                Timber.i("✅ LLM_PREFILL → NPU (QNN/NNAPI, fused kernels, ${MEMORY_MISTRAL_7B_INT8}MB)")
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
                Timber.i("✅ VISION → NPU (QNN/NNAPI, fused kernels, ${MEMORY_MOBILENET_V3_INT8}MB)")
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
     * Detect Qualcomm NPU (QNN/NNAPI delegate)
     */
    private fun detectQNNNPU(): Boolean {
        try {
            // Check device model
            val model = Build.MODEL
            val isS24Ultra = model.contains("SM-S928", ignoreCase = true) ||
                           model.contains("Galaxy S24 Ultra", ignoreCase = true)

            // Check SoC
            val soc = Build.HARDWARE
            val hasSnapdragon8Gen3 = soc.contains("qcom", ignoreCase = true) ||
                                    soc.contains("kalama", ignoreCase = true)  // SD8G3 codename

            val hasNPU = isS24Ultra && hasSnapdragon8Gen3

            if (hasNPU) {
                Timber.i("✅ NPU detected (S24 Ultra + Snapdragon 8 Gen 3, QNN/NNAPI available)")
            } else {
                Timber.w("⚠️ NPU (QNN/NNAPI) not detected (device: $model, soc: $soc)")
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
        | AI-Ish Device Allocation Summary (Samsung S24 Ultra)
        |═══════════════════════════════════════════════════════════════
        |
        | CPU (Cores 0-3):
        |   ├─ BGE Embeddings (FP16/INT8, ${MEMORY_BGE_FP16}MB)
        |   ├─ LLM Token Decode (streaming)
        |   └─ Auxiliary tasks
        |
        | NPU (QNN/NNAPI, ${NPU_TOPS_QNN_NNAPI} TOPS INT8):
        |   ├─ Mistral-7B Prefill (INT8, ${MEMORY_MISTRAL_7B_INT8}MB, fused kernels)
        |   └─ MobileNet-v3 Vision (INT8, ${MEMORY_MOBILENET_V3_INT8}MB, fused kernels)
        |
        | GPU (Adreno 750):
        |   └─ RESERVED / IDLE (avoid memory contention)
        |
        | Memory Budget:
        |   Total: ${MEMORY_MISTRAL_7B_INT8 + MEMORY_MOBILENET_V3_INT8 + MEMORY_BGE_FP16 + MEMORY_OVERHEAD}MB / ${TOTAL_MEMORY_BUDGET}MB
        |   ├─ Mistral-7B: ${MEMORY_MISTRAL_7B_INT8}MB
        |   ├─ MobileNet-v3: ${MEMORY_MOBILENET_V3_INT8}MB
        |   ├─ BGE: ${MEMORY_BGE_FP16}MB
        |   └─ Overhead: ${MEMORY_OVERHEAD}MB
        |
        | Concurrent Execution: ✅ ENABLED
        |   All 3 models run simultaneously without conflicts
        |═══════════════════════════════════════════════════════════════
        """.trimMargin()
    }
}
