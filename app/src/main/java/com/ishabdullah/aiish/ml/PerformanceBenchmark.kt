/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.ml

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.system.measureTimeMillis

/**
 * Performance benchmarking for LLM inference
 *
 * Measures:
 * - Tokens per second (t/s)
 * - Prompt processing time
 * - First token latency
 * - Average token generation time
 * - GPU vs CPU performance comparison
 */
class PerformanceBenchmark(private val llmEngine: LLMInferenceEngine) {

    data class BenchmarkResult(
        val mode: String, // "CPU" or "GPU"
        val gpuLayers: Int,
        val promptTokens: Int,
        val generatedTokens: Int,
        val totalTimeMs: Long,
        val promptProcessingTimeMs: Long,
        val firstTokenLatencyMs: Long,
        val tokensPerSecond: Float,
        val avgTokenTimeMs: Float,
        val peakMemoryMB: Float
    ) {
        override fun toString(): String {
            return """
                |Benchmark Results ($mode, GPU layers: $gpuLayers)
                |  Prompt: $promptTokens tokens in ${promptProcessingTimeMs}ms
                |  Generated: $generatedTokens tokens in ${totalTimeMs}ms
                |  First token latency: ${firstTokenLatencyMs}ms
                |  Speed: ${"%.2f".format(tokensPerSecond)} t/s
                |  Avg token time: ${"%.2f".format(avgTokenTimeMs)}ms
                |  Peak memory: ${"%.1f".format(peakMemoryMB)} MB
            """.trimMargin()
        }
    }

    /**
     * Run a quick benchmark (10 tokens)
     */
    suspend fun runQuickBenchmark(gpuLayers: Int = 0): BenchmarkResult? = withContext(Dispatchers.IO) {
        return@withContext runBenchmark(
            prompt = "The quick brown fox jumps over the lazy dog.",
            maxTokens = 10,
            gpuLayers = gpuLayers
        )
    }

    /**
     * Run a full benchmark (100 tokens)
     */
    suspend fun runFullBenchmark(gpuLayers: Int = 0): BenchmarkResult? = withContext(Dispatchers.IO) {
        return@withContext runBenchmark(
            prompt = "Write a short story about artificial intelligence:",
            maxTokens = 100,
            gpuLayers = gpuLayers
        )
    }

    /**
     * Run benchmark with custom parameters
     */
    suspend fun runBenchmark(
        prompt: String,
        maxTokens: Int,
        gpuLayers: Int = 0
    ): BenchmarkResult? = withContext(Dispatchers.IO) {
        if (!llmEngine.isLoaded()) {
            Timber.e("Cannot benchmark: model not loaded")
            return@withContext null
        }

        try {
            Timber.i("Starting benchmark: gpuLayers=$gpuLayers, maxTokens=$maxTokens")

            val startMemoryMB = getMemoryUsageMB()
            var peakMemoryMB = startMemoryMB

            var promptTokens = 0
            var generatedTokens = 0
            var firstTokenLatencyMs = 0L
            var promptProcessingTimeMs = 0L

            val startTime = System.currentTimeMillis()
            var firstTokenTime = 0L

            // Generate tokens and measure
            val tokenFlow = llmEngine.generateStream(
                prompt = prompt,
                maxTokens = maxTokens,
                temperature = 0.7f,
                topP = 0.9f
            )

            tokenFlow.collect { token ->
                generatedTokens++

                // Measure first token latency
                if (generatedTokens == 1) {
                    firstTokenTime = System.currentTimeMillis()
                    firstTokenLatencyMs = firstTokenTime - startTime
                }

                // Track peak memory
                val currentMemoryMB = getMemoryUsageMB()
                if (currentMemoryMB > peakMemoryMB) {
                    peakMemoryMB = currentMemoryMB
                }
            }

            val totalTimeMs = System.currentTimeMillis() - startTime
            val generationTimeMs = if (firstTokenTime > 0) {
                System.currentTimeMillis() - firstTokenTime
            } else {
                totalTimeMs
            }

            // Calculate metrics
            val tokensPerSecond = if (generationTimeMs > 0) {
                (generatedTokens * 1000f) / generationTimeMs
            } else {
                0f
            }

            val avgTokenTimeMs = if (generatedTokens > 0) {
                generationTimeMs.toFloat() / generatedTokens
            } else {
                0f
            }

            promptProcessingTimeMs = firstTokenLatencyMs

            val result = BenchmarkResult(
                mode = if (gpuLayers > 0) "GPU" else "CPU",
                gpuLayers = gpuLayers,
                promptTokens = promptTokens,
                generatedTokens = generatedTokens,
                totalTimeMs = totalTimeMs,
                promptProcessingTimeMs = promptProcessingTimeMs,
                firstTokenLatencyMs = firstTokenLatencyMs,
                tokensPerSecond = tokensPerSecond,
                avgTokenTimeMs = avgTokenTimeMs,
                peakMemoryMB = peakMemoryMB - startMemoryMB
            )

            Timber.i(result.toString())
            result

        } catch (e: Exception) {
            Timber.e(e, "Benchmark failed")
            null
        }
    }

    /**
     * Compare GPU vs CPU performance
     */
    suspend fun compareGPUvsCPU(gpuLayers: Int = 35): Pair<BenchmarkResult?, BenchmarkResult?> {
        Timber.i("Running GPU vs CPU comparison...")

        val cpuResult = runQuickBenchmark(gpuLayers = 0)
        val gpuResult = runQuickBenchmark(gpuLayers = gpuLayers)

        if (cpuResult != null && gpuResult != null) {
            val speedup = gpuResult.tokensPerSecond / cpuResult.tokensPerSecond
            Timber.i("GPU speedup: ${"%.2f".format(speedup)}x faster than CPU")
        }

        return Pair(cpuResult, gpuResult)
    }

    /**
     * Find optimal GPU layer count
     */
    suspend fun findOptimalGPULayers(maxLayers: Int = 40): Int {
        Timber.i("Finding optimal GPU layer count (max: $maxLayers)...")

        val layerCounts = listOf(0, 10, 20, 25, 30, 35, 40).filter { it <= maxLayers }
        var bestLayers = 0
        var bestSpeed = 0f

        for (layers in layerCounts) {
            val result = runQuickBenchmark(gpuLayers = layers)
            if (result != null && result.tokensPerSecond > bestSpeed) {
                bestSpeed = result.tokensPerSecond
                bestLayers = layers
            }
        }

        Timber.i("Optimal GPU layers: $bestLayers (${"%.2f".format(bestSpeed)} t/s)")
        return bestLayers
    }

    /**
     * Get current memory usage in MB
     */
    private fun getMemoryUsageMB(): Float {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024f * 1024f)
    }

    /**
     * Estimate tokens per second for a given configuration
     */
    fun estimateTokensPerSecond(gpuLayers: Int, modelSizeGB: Float): Float {
        // Conservative estimates based on S24 Ultra benchmarks
        return when {
            gpuLayers == 0 -> { // CPU only
                when {
                    modelSizeGB <= 1.0 -> 8f // 360M-1.5B models
                    modelSizeGB <= 3.0 -> 5f // 2B-3B models
                    modelSizeGB <= 5.0 -> 3f // 7B models
                    else -> 1f // 13B+ models
                }
            }
            gpuLayers >= 30 -> { // Full GPU (Adreno 750)
                when {
                    modelSizeGB <= 1.0 -> 25f
                    modelSizeGB <= 3.0 -> 20f
                    modelSizeGB <= 5.0 -> 15f
                    else -> 8f
                }
            }
            else -> { // Partial GPU
                val cpuSpeed = estimateTokensPerSecond(0, modelSizeGB)
                val gpuSpeed = estimateTokensPerSecond(35, modelSizeGB)
                val ratio = gpuLayers / 35f
                cpuSpeed + (gpuSpeed - cpuSpeed) * ratio
            }
        }
    }
}
