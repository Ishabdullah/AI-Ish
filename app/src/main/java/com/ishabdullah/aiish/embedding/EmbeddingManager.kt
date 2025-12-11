/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.embedding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * EmbeddingManager - BGE (BAAI General Embedding) model on CPU
 *
 * Device: CPU cores 0-3 (Snapdragon 8 Gen 3 efficiency cores @ 2.3GHz)
 * Model: BGE-Small-EN INT8/FP16
 * Memory: ~300MB
 * Performance: ~500 embeddings/sec on efficiency cores
 *
 * Features:
 * - Asynchronous execution on CPU efficiency cores
 * - Batch embedding generation
 * - Preallocated buffers for zero-copy operations
 * - Concurrent execution with LLM and vision models
 *
 * Use Cases:
 * - Semantic search
 * - RAG (Retrieval Augmented Generation)
 * - Document similarity
 * - Clustering
 */
class EmbeddingManager {

    companion object {
        init {
            try {
                System.loadLibrary("aiish_native")
                Timber.i("EmbeddingManager: Native library loaded")
            } catch (e: UnsatisfiedLinkError) {
                Timber.e(e, "Failed to load native library for embeddings")
            }
        }

        // CPU core affinity for efficiency cores (0-3)
        private val CPU_CORES_EFFICIENCY = listOf(0, 1, 2, 3)
        private const val EMBEDDING_DIM = 384  // BGE-Small dimension
        private const val MAX_BATCH_SIZE = 32
    }

    private var isModelLoaded = false
    private var currentModelPath: String? = null

    // Native methods
    private external fun nativeLoadBGEModel(
        modelPath: String,
        cpuCores: IntArray,
        usePreallocatedBuffers: Boolean
    ): Boolean

    private external fun nativeGenerateEmbedding(text: String): FloatArray
    private external fun nativeGenerateEmbeddingsBatch(texts: Array<String>): Array<FloatArray>
    private external fun nativeReleaseBGEModel()

    /**
     * Load BGE model on CPU efficiency cores
     */
    suspend fun loadModel(modelFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isModelLoaded && currentModelPath == modelFile.absolutePath) {
                Timber.d("BGE model already loaded: ${modelFile.name}")
                return@withContext true
            }

            if (!modelFile.exists()) {
                Timber.e("BGE model file not found: ${modelFile.absolutePath}")
                return@withContext false
            }

            Timber.i("Loading BGE model on CPU cores ${CPU_CORES_EFFICIENCY}: ${modelFile.name}")

            val success = nativeLoadBGEModel(
                modelPath = modelFile.absolutePath,
                cpuCores = CPU_CORES_EFFICIENCY.toIntArray(),
                usePreallocatedBuffers = true
            )

            if (success) {
                isModelLoaded = true
                currentModelPath = modelFile.absolutePath
                Timber.i("✅ BGE model loaded successfully on CPU efficiency cores")
                Timber.i("   - Cores: ${CPU_CORES_EFFICIENCY}")
                Timber.i("   - Embedding dimension: $EMBEDDING_DIM")
                Timber.i("   - Preallocated buffers: ENABLED")
            } else {
                Timber.e("Failed to load BGE model")
            }

            success

        } catch (e: Exception) {
            Timber.e(e, "Error loading BGE model")
            false
        }
    }

    /**
     * Generate embedding for single text (async on CPU cores 0-3)
     */
    suspend fun generateEmbedding(text: String): FloatArray? = withContext(Dispatchers.Default) {
        try {
            if (!isModelLoaded) {
                Timber.e("BGE model not loaded")
                return@withContext null
            }

            if (text.isBlank()) {
                Timber.w("Cannot generate embedding for empty text")
                return@withContext null
            }

            val startTime = System.currentTimeMillis()

            val embedding = nativeGenerateEmbedding(text)

            val processingTime = System.currentTimeMillis() - startTime

            if (embedding.size == EMBEDDING_DIM) {
                Timber.d("Generated embedding: ${text.take(50)}... (${processingTime}ms, ${embedding.size}D)")
                embedding
            } else {
                Timber.e("Invalid embedding dimension: ${embedding.size} (expected $EMBEDDING_DIM)")
                null
            }

        } catch (e: Exception) {
            Timber.e(e, "Error generating embedding")
            null
        }
    }

    /**
     * Generate embeddings for batch of texts (parallel async on CPU cores)
     */
    suspend fun generateEmbeddingsBatch(texts: List<String>): List<FloatArray> = withContext(Dispatchers.Default) {
        try {
            if (!isModelLoaded) {
                Timber.e("BGE model not loaded")
                return@withContext emptyList()
            }

            if (texts.isEmpty()) {
                return@withContext emptyList()
            }

            val startTime = System.currentTimeMillis()

            // Process in batches of MAX_BATCH_SIZE
            val embeddings = texts.chunked(MAX_BATCH_SIZE).map { batch ->
                async {
                    nativeGenerateEmbeddingsBatch(batch.toTypedArray())
                }
            }.awaitAll().flatMap { it.toList() }

            val processingTime = System.currentTimeMillis() - startTime
            val throughput = (texts.size.toFloat() / processingTime) * 1000  // embeddings/sec

            Timber.d("Generated ${embeddings.size} embeddings (${processingTime}ms, ${throughput.toInt()} emb/s)")

            embeddings

        } catch (e: Exception) {
            Timber.e(e, "Error generating batch embeddings")
            emptyList()
        }
    }

    /**
     * Compute cosine similarity between two embeddings
     */
    fun cosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        if (embedding1.size != embedding2.size) {
            Timber.e("Embedding dimension mismatch: ${embedding1.size} vs ${embedding2.size}")
            return 0f
        }

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            norm1 += embedding1[i] * embedding1[i]
            norm2 += embedding2[i] * embedding2[i]
        }

        if (norm1 == 0f || norm2 == 0f) {
            return 0f
        }

        return dotProduct / (Math.sqrt(norm1.toDouble()) * Math.sqrt(norm2.toDouble())).toFloat()
    }

    /**
     * Find most similar text from candidates
     */
    suspend fun findMostSimilar(
        query: String,
        candidates: List<String>
    ): Pair<String, Float>? = withContext(Dispatchers.Default) {
        try {
            // Generate query embedding
            val queryEmbedding = generateEmbedding(query) ?: return@withContext null

            // Generate candidate embeddings
            val candidateEmbeddings = generateEmbeddingsBatch(candidates)

            if (candidateEmbeddings.size != candidates.size) {
                Timber.e("Embedding count mismatch")
                return@withContext null
            }

            // Find most similar
            var maxSimilarity = -1f
            var mostSimilarIndex = -1

            candidateEmbeddings.forEachIndexed { index, embedding ->
                val similarity = cosineSimilarity(queryEmbedding, embedding)
                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity
                    mostSimilarIndex = index
                }
            }

            if (mostSimilarIndex >= 0) {
                Pair(candidates[mostSimilarIndex], maxSimilarity)
            } else {
                null
            }

        } catch (e: Exception) {
            Timber.e(e, "Error finding most similar")
            null
        }
    }

    /**
     * Check if model is loaded
     */
    fun isLoaded(): Boolean = isModelLoaded

    /**
     * Get model path
     */
    fun getModelPath(): String? = currentModelPath

    /**
     * Get embedding dimension
     */
    fun getEmbeddingDim(): Int = EMBEDDING_DIM

    /**
     * Release model and free resources
     */
    fun release() {
        if (isModelLoaded) {
            try {
                nativeReleaseBGEModel()
                Timber.i("BGE model released")
            } catch (e: Exception) {
                Timber.e(e, "Error releasing BGE model")
            }
        }
        isModelLoaded = false
        currentModelPath = null
    }
}
