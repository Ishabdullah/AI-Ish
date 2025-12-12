/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.ml

import com.ishabdullah.aiish.device.DeviceAllocationManager
import com.ishabdullah.aiish.device.NPUManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * LLM Inference Engine - Mistral-7B via llama.cpp
 *
 * Architecture: CPU-only inference with ARM NEON optimizations
 * ┌──────────────────────────────────────────────────────────────┐
 * │ CPU Inference (llama.cpp)                                    │
 * │ - Full inference on CPU with ARM NEON SIMD                   │
 * │ - Optimized for Snapdragon 8 Gen 3 cores                    │
 * │ - INT8/FP16 quantized models (GGUF format)                   │
 * │ - Streaming token generation                                 │
 * │ Performance: ~10-25 tokens/sec (device dependent)            │
 * └──────────────────────────────────────────────────────────────┘
 *
 * Note: NNAPI/NPU is not used for LLM inference. NNAPI is optimized
 * for CNN models but does not efficiently support transformer architectures.
 * Vision models (MobileNet-v3) use NNAPI via TFLite Kotlin API.
 *
 * Memory: ~3.5GB (INT8 quantized)
 */
class LLMInferenceEngine {

    companion object {
        init {
            try {
                System.loadLibrary("aiish_native")
                Timber.i("LLMInferenceEngine: Native library loaded")
            } catch (e: UnsatisfiedLinkError) {
                Timber.e(e, "Failed to load native library")
            }
        }

        private const val DEFAULT_CONTEXT_SIZE = 4096
        private const val DEFAULT_TEMPERATURE = 0.7f
        private const val DEFAULT_TOP_P = 0.9f

        // CPU core affinity for decode (efficiency cores 0-3)
        private val CPU_CORES_DECODE = listOf(0, 1, 2, 3)
    }

    private var isModelLoaded = false
    private var contextSize = DEFAULT_CONTEXT_SIZE
    @Deprecated("NPU mode deprecated - LLM uses CPU-only")
    private var useProductionMode = false  // Deprecated - kept for backward compatibility

    // Device managers
    private var npuManager: NPUManager? = null
    private var deviceAllocator: DeviceAllocationManager? = null

    // Performance tracking
    private var lastPrefillTimeMs = 0L
    private var lastDecodeTimeMs = 0L
    private var lastTokensPerSecond = 0f

    // Legacy native methods (backward compatibility)
    private external fun nativeLoadModel(
        modelPath: String,
        contextSize: Int,
        gpuLayers: Int
    ): Int

    private external fun nativeInitContext(contextSize: Int): Int
    private external fun nativeTokenize(text: String, tokensOut: IntArray): Int
    private external fun nativeGenerate(
        tokens: IntArray,
        numTokens: Int,
        temperature: Float,
        topP: Float
    ): Int
    private external fun nativeDecode(token: Int): String
    private external fun nativeIsEOS(token: Int): Boolean
    private external fun nativeFree()
    private external fun nativeGetVocabSize(): Int

    // CPU mode native methods (llama.cpp) - NPU params deprecated
    private external fun nativeLoadMistralINT8(
        modelPath: String,
        contextSize: Int,
        useNPUPrefill: Boolean,
        cpuCores: IntArray,
        usePreallocatedBuffers: Boolean
    ): Boolean

    private external fun nativePrefillOnNPU(
        tokens: IntArray,
        numTokens: Int
    ): Boolean

    private external fun nativeDecodeOnCPU(
        currentToken: Int,
        temperature: Float,
        topP: Float
    ): Int

    private external fun nativeGetPrefillTimeMs(): Long
    private external fun nativeGetDecodeTimeMs(): Long
    private external fun nativeReleaseMistral()

    /**
     * Load Mistral-7B INT8 model (CPU-only via llama.cpp)
     *
     * Note: Despite the "production" name, this method now uses CPU-only inference.
     * NNAPI/NPU is not suitable for transformer models like Mistral-7B.
     * The useNPUPrefill parameter is deprecated and ignored.
     *
     * @param modelFile Mistral-7B INT8 GGUF model file
     * @param contextSize Maximum context window (default 4096)
     * @return true if successful, false otherwise
     */
    suspend fun loadModelProduction(
        modelFile: File,
        contextSize: Int = DEFAULT_CONTEXT_SIZE
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!modelFile.exists()) {
                Timber.e("Model file not found: ${modelFile.absolutePath}")
                return@withContext false
            }

            Timber.i("═══════════════════════════════════════════════════════════")
            Timber.i("Loading Mistral-7B INT8 (CPU mode via llama.cpp)")
            Timber.i("═══════════════════════════════════════════════════════════")

            this@LLMInferenceEngine.contextSize = contextSize

            // Initialize device managers for tracking
            deviceAllocator = DeviceAllocationManager()

            // Get allocation for LLM (now CPU-only)
            @Suppress("DEPRECATION")
            val llmAllocation = deviceAllocator?.allocateDevice(
                DeviceAllocationManager.ModelType.LLM_DECODE
            )

            Timber.i("Device allocation:")
            Timber.i("  └─ LLM: CPU (llama.cpp with ARM NEON)")

            // Load model on CPU (useNPUPrefill=false, ignored anyway)
            val success = nativeLoadMistralINT8(
                modelPath = modelFile.absolutePath,
                contextSize = contextSize,
                useNPUPrefill = false,  // Deprecated - always CPU
                cpuCores = CPU_CORES_DECODE.toIntArray(),
                usePreallocatedBuffers = true
            )

            if (success) {
                isModelLoaded = true
                @Suppress("DEPRECATION")
                useProductionMode = true  // Keep for backward compatibility

                Timber.i("✅ Mistral-7B INT8 loaded successfully")
                Timber.i("   - Context size: $contextSize")
                Timber.i("   - Memory: ~3.5GB (INT8)")
                Timber.i("   - Mode: CPU (llama.cpp with ARM NEON)")
                Timber.i("   - Performance: ~10-25 tokens/sec")
                Timber.i("═══════════════════════════════════════════════════════════")
            } else {
                Timber.e("Failed to load model")
            }

            success

        } catch (e: Exception) {
            Timber.e(e, "Error loading model")
            false
        }
    }

    /**
     * Load a GGUF model from file path (LEGACY mode - backward compatibility)
     *
     * @param modelPath Absolute path to .gguf model file
     * @param contextSize Maximum context window (default 4096)
     * @param gpuLayers Number of layers to offload to GPU (0 = CPU only)
     * @return true if successful, false otherwise
     */
    suspend fun loadModel(
        modelPath: String,
        contextSize: Int = DEFAULT_CONTEXT_SIZE,
        gpuLayers: Int = 0
    ): Boolean = loadModelLegacy(modelPath, contextSize, gpuLayers)

    /**
     * Legacy model loading (CPU/GPU only, no NPU)
     */
    private suspend fun loadModelLegacy(
        modelPath: String,
        contextSize: Int,
        gpuLayers: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.i("Loading model (LEGACY mode): $modelPath")
            this@LLMInferenceEngine.contextSize = contextSize
            useProductionMode = false

            val result = nativeLoadModel(modelPath, contextSize, gpuLayers)
            if (result != 0) {
                Timber.e("nativeLoadModel returned error: $result")
                return@withContext false
            }

            val ctxResult = nativeInitContext(contextSize)
            if (ctxResult != 0) {
                Timber.e("nativeInitContext returned error: $ctxResult")
                return@withContext false
            }

            isModelLoaded = true

            if (gpuLayers > 0) {
                Timber.i("Model loaded successfully with GPU acceleration (ctx=$contextSize, gpu_layers=$gpuLayers)")
            } else {
                Timber.i("Model loaded successfully in CPU mode (ctx=$contextSize)")
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to load model")
            false
        }
    }

    /**
     * Generate text with token-by-token streaming
     *
     * Uses llama.cpp CPU inference with ARM NEON optimizations.
     *
     * @param prompt Input text prompt
     * @param maxTokens Maximum tokens to generate
     * @param temperature Sampling temperature (0.0-2.0)
     * @param topP Nucleus sampling threshold (0.0-1.0)
     * @param stopSequences List of strings that stop generation
     * @return Flow emitting generated tokens as strings
     */
    fun generateStream(
        prompt: String,
        maxTokens: Int = 512,
        temperature: Float = DEFAULT_TEMPERATURE,
        topP: Float = DEFAULT_TOP_P,
        stopSequences: List<String> = emptyList()
    ): Flow<String> = flow {
        if (!isModelLoaded) {
            Timber.w("Model not loaded, cannot generate")
            emit("[ERROR: Model not loaded]")
            return@flow
        }

        try {
            if (useProductionMode) {
                // PRODUCTION MODE: NPU prefill + CPU decode
                generateProductionStream(
                    prompt, maxTokens, temperature, topP, stopSequences
                ).collect { token -> emit(token) }
            } else {
                // LEGACY MODE: CPU/GPU generation
                generateLegacyStream(
                    prompt, maxTokens, temperature, topP, stopSequences
                ).collect { token -> emit(token) }
            }

        } catch (e: Exception) {
            Timber.e(e, "Error during generation")
            emit("[ERROR: ${e.message}]")
        }
    }.flowOn(Dispatchers.Default)

    /**
     * CPU generation via llama.cpp (formerly "production mode")
     *
     * Note: This now uses CPU-only inference. The NPU prefill stage is deprecated.
     */
    private fun generateProductionStream(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        stopSequences: List<String>
    ): Flow<String> = flow {
        Timber.d("CPU generation (llama.cpp): ${prompt.take(100)}...")

        val startTime = System.currentTimeMillis()

        // ========== TOKENIZE ==========
        val prefillStart = System.currentTimeMillis()

        val maxPromptTokens = 2048
        val promptTokens = IntArray(maxPromptTokens)
        val numPromptTokens = nativeTokenize(prompt, promptTokens)

        if (numPromptTokens <= 0) {
            Timber.e("Tokenization failed")
            emit("[ERROR: Tokenization failed]")
            return@flow
        }

        Timber.d("Tokenized: $numPromptTokens tokens")

        // Prefill via llama.cpp (CPU) - nativePrefillOnNPU is now CPU-based
        val prefillSuccess = nativePrefillOnNPU(
            promptTokens,
            numPromptTokens
        )

        if (!prefillSuccess) {
            Timber.e("Prefill failed")
            emit("[ERROR: Prefill failed]")
            return@flow
        }

        lastPrefillTimeMs = System.currentTimeMillis() - prefillStart
        Timber.i("✅ Prefill complete: ${lastPrefillTimeMs}ms for $numPromptTokens tokens")

        // ========== DECODE ON CPU ==========
        Timber.d("CPU decode streaming...")
        val decodeStart = System.currentTimeMillis()

        val generatedText = StringBuilder()
        var tokensGenerated = 0
        var currentToken = -1

        // Stream tokens one by one from CPU
        for (i in 0 until maxTokens) {
            // Generate next token on CPU
            val newToken = nativeDecodeOnCPU(
                currentToken,
                temperature,
                topP
            )

            // Check for EOS
            if (nativeIsEOS(newToken)) {
                Timber.d("EOS token generated, stopping")
                break
            }

            // Decode token to text
            val tokenText = nativeDecode(newToken)

            // Check stop sequences
            generatedText.append(tokenText)
            if (stopSequences.any { generatedText.toString().contains(it) }) {
                Timber.d("Stop sequence detected, stopping")
                break
            }

            // Emit token (streaming to UI)
            emit(tokenText)

            currentToken = newToken
            tokensGenerated++

            // Check context overflow
            if (numPromptTokens + tokensGenerated >= contextSize - 1) {
                Timber.w("Context size limit reached")
                break
            }
        }

        lastDecodeTimeMs = System.currentTimeMillis() - decodeStart
        val totalTime = System.currentTimeMillis() - startTime
        lastTokensPerSecond = if (lastDecodeTimeMs > 0) {
            (tokensGenerated.toFloat() / lastDecodeTimeMs) * 1000f
        } else {
            0f
        }

        Timber.i("✅ Generation complete:")
        Timber.i("   - Prefill: ${lastPrefillTimeMs}ms ($numPromptTokens tokens)")
        Timber.i("   - Decode: ${lastDecodeTimeMs}ms ($tokensGenerated tokens)")
        Timber.i("   - Throughput: ${lastTokensPerSecond.toInt()} tokens/sec")
        Timber.i("   - Total time: ${totalTime}ms")

    }.flowOn(Dispatchers.Default)

    /**
     * Legacy mode generation: CPU/GPU
     */
    private fun generateLegacyStream(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        stopSequences: List<String>
    ): Flow<String> = flow {
        Timber.d("Legacy generation: ${prompt.take(100)}...")

        // Tokenize prompt
        val maxPromptTokens = 2048
        val promptTokens = IntArray(maxPromptTokens)
        val numPromptTokens = nativeTokenize(prompt, promptTokens)

        if (numPromptTokens <= 0) {
            Timber.e("Tokenization failed")
            emit("[ERROR: Tokenization failed]")
            return@flow
        }

        Timber.d("Prompt tokenized: $numPromptTokens tokens")

        // Prepare for generation
        val allTokens = promptTokens.copyOf(maxPromptTokens + maxTokens)
        var currentLength = numPromptTokens
        val generatedText = StringBuilder()

        // Generate tokens one by one
        for (i in 0 until maxTokens) {
            // Generate next token
            val newToken = nativeGenerate(
                allTokens,
                currentLength,
                temperature,
                topP
            )

            // Check for EOS
            if (nativeIsEOS(newToken)) {
                Timber.d("EOS token generated, stopping")
                break
            }

            // Decode token to text
            val tokenText = nativeDecode(newToken)

            // Check stop sequences
            generatedText.append(tokenText)
            if (stopSequences.any { generatedText.toString().contains(it) }) {
                Timber.d("Stop sequence detected, stopping")
                break
            }

            // Emit token
            emit(tokenText)

            // Add to context
            allTokens[currentLength] = newToken
            currentLength++

            // Check context overflow
            if (currentLength >= contextSize - 1) {
                Timber.w("Context size limit reached")
                break
            }
        }

        Timber.d("Generation complete: ${generatedText.length} chars")

    }.flowOn(Dispatchers.Default)

    /**
     * Generate complete text (non-streaming)
     *
     * @param prompt Input text prompt
     * @param maxTokens Maximum tokens to generate
     * @param temperature Sampling temperature
     * @param topP Nucleus sampling threshold
     * @return Generated text
     */
    suspend fun generate(
        prompt: String,
        maxTokens: Int = 512,
        temperature: Float = DEFAULT_TEMPERATURE,
        topP: Float = DEFAULT_TOP_P
    ): String = withContext(Dispatchers.IO) {
        val result = StringBuilder()
        generateStream(prompt, maxTokens, temperature, topP)
            .collect { token ->
                result.append(token)
            }
        result.toString()
    }

    /**
     * Get model vocabulary size
     */
    fun getVocabSize(): Int {
        return if (isModelLoaded) nativeGetVocabSize() else 0
    }

    /**
     * Check if model is loaded
     */
    fun isLoaded(): Boolean = isModelLoaded

    /**
     * Check if production mode is active
     * @deprecated NPU mode is deprecated - LLM uses CPU-only
     */
    @Deprecated("NPU mode deprecated - LLM uses CPU-only")
    fun isProductionMode(): Boolean = useProductionMode

    /**
     * Get performance mode string
     */
    fun getPerformanceMode(): String {
        return when {
            !isModelLoaded -> "Not Loaded"
            else -> "CPU (llama.cpp with ARM NEON)"
        }
    }

    /**
     * Get last prefill time (production mode only)
     */
    fun getLastPrefillTimeMs(): Long = lastPrefillTimeMs

    /**
     * Get last decode time (production mode only)
     */
    fun getLastDecodeTimeMs(): Long = lastDecodeTimeMs

    /**
     * Get tokens per second (production mode only)
     */
    fun getTokensPerSecond(): Float = lastTokensPerSecond

    /**
     * Get performance stats
     */
    fun getPerformanceStats(): String {
        return if (isModelLoaded) {
            """
            |Performance Stats (CPU Mode):
            |  - Prefill: ${lastPrefillTimeMs}ms
            |  - Decode: ${lastDecodeTimeMs}ms
            |  - Throughput: ${lastTokensPerSecond.toInt()} tokens/sec
            |  - Mode: CPU (llama.cpp with ARM NEON)
            """.trimMargin()
        } else {
            "Performance stats not available - model not loaded"
        }
    }

    /**
     * Free model and release resources
     */
    fun release() {
        if (isModelLoaded) {
            Timber.i("Releasing LLM model")

            if (useProductionMode) {
                nativeReleaseMistral()
                npuManager?.release()
            } else {
                nativeFree()
            }

            isModelLoaded = false
            useProductionMode = false
            npuManager = null
            deviceAllocator = null
        }
    }

    /**
     * Cleanup on destruction
     */
    protected fun finalize() {
        release()
    }
}
