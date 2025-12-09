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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * LLM Inference Engine using native llama.cpp via JNI
 *
 * Provides GGUF model loading and token-by-token streaming inference
 * Optimized for ARM64 with NEON SIMD instructions
 */
class LLMInferenceEngine {

    companion object {
        init {
            try {
                System.loadLibrary("aiish_native")
                Timber.i("Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Timber.e(e, "Failed to load native library")
            }
        }

        private const val DEFAULT_CONTEXT_SIZE = 4096
        private const val DEFAULT_TEMPERATURE = 0.7f
        private const val DEFAULT_TOP_P = 0.9f
        private const val DEFAULT_GPU_LAYERS = 0 // Will be set based on device capability
    }

    private var isModelLoaded = false
    private var contextSize = DEFAULT_CONTEXT_SIZE

    // Native methods
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

    /**
     * Load a GGUF model from file path
     *
     * @param modelPath Absolute path to .gguf model file
     * @param contextSize Maximum context window (default 4096)
     * @param gpuLayers Number of layers to offload to GPU (0 = CPU only)
     * @return true if successful, false otherwise
     */
    suspend fun loadModel(
        modelPath: String,
        contextSize: Int = DEFAULT_CONTEXT_SIZE,
        gpuLayers: Int = DEFAULT_GPU_LAYERS
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.i("Loading model: $modelPath")
            this@LLMInferenceEngine.contextSize = contextSize

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
            Timber.i("Model loaded successfully (ctx=$contextSize, gpu_layers=$gpuLayers)")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to load model")
            false
        }
    }

    /**
     * Generate text with token-by-token streaming
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
            Timber.d("Generating with prompt: ${prompt.take(100)}...")

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

        } catch (e: Exception) {
            Timber.e(e, "Error during generation")
            emit("[ERROR: ${e.message}]")
        }
    }.flowOn(Dispatchers.IO)

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
     * Free model and release resources
     */
    fun release() {
        if (isModelLoaded) {
            Timber.i("Releasing model")
            nativeFree()
            isModelLoaded = false
        }
    }

    /**
     * Cleanup on destruction
     */
    protected fun finalize() {
        release()
    }
}
