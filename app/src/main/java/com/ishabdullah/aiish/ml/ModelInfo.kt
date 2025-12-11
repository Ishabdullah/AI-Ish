/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.ml

data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val sizeMB: Long,
    val downloadUrl: String,
    val sha256: String,
    val filename: String,
    val type: ModelType
)

enum class ModelType {
    LLM,        // Primary language model
    VISION,     // Vision/image model
    EMBEDDING,  // Embedding model
    AUDIO       // Audio model (STT)
}

object ModelCatalog {
    // =========================================================================
    // PRODUCTION MODELS (Samsung S24 Ultra Optimized)
    // =========================================================================

    /**
     * Mistral-7B-Instruct INT8 - Primary LLM
     * Device: NPU Hexagon v81 (prefill) + CPU (decode)
     * Quantization: INT8 for minimal memory and max throughput
     * Memory: ~3.5GB
     * Performance: ~25-35 tokens/sec on S24 Ultra
     */
    val MISTRAL_7B_INT8 = ModelInfo(
        id = "mistral_7b_int8",
        name = "Mistral-7B INT8 (Production)",
        description = "Mistral-7B-Instruct INT8 • NPU-optimized • 25-35 t/s",
        sizeMB = 3500,
        downloadUrl = "https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q8_0.gguf",
        sha256 = "placeholder_sha256_mistral_int8",
        filename = "mistral-7b-instruct-int8.gguf",
        type = ModelType.LLM
    )

    /**
     * MobileNet-v3 INT8 - Vision Model
     * Device: NPU Hexagon v81
     * Quantization: INT8 for fast inference
     * Memory: ~500MB
     * Performance: ~60 FPS on S24 Ultra NPU
     */
    val MOBILENET_V3_INT8 = ModelInfo(
        id = "mobilenet_v3_int8",
        name = "MobileNet-v3 INT8 (Production)",
        description = "MobileNet-v3-Large INT8 • NPU-optimized • 60 FPS",
        sizeMB = 500,
        downloadUrl = "https://huggingface.co/google/mobilenet_v3_large_100_224/resolve/main/mobilenet_v3_large_100_224_int8.tflite",
        sha256 = "placeholder_sha256_mobilenet",
        filename = "mobilenet-v3-large-int8.tflite",
        type = ModelType.VISION
    )

    /**
     * BGE-Small INT8 - Embedding Model
     * Device: CPU cores 0-3 (async)
     * Quantization: INT8/FP16 hybrid
     * Memory: ~300MB
     * Performance: ~500 embeddings/sec on efficiency cores
     */
    val BGE_SMALL_INT8 = ModelInfo(
        id = "bge_small_int8",
        name = "BGE-Small INT8 (Production)",
        description = "BGE-Small-EN INT8 • CPU-optimized • Fast embeddings",
        sizeMB = 300,
        downloadUrl = "https://huggingface.co/BAAI/bge-small-en-v1.5/resolve/main/model_int8.gguf",
        sha256 = "placeholder_sha256_bge",
        filename = "bge-small-en-int8.gguf",
        type = ModelType.EMBEDDING
    )

    val MOONDREAM2 = ModelInfo(
        id = "moondream2_gguf",
        name = "Vision Mode",
        description = "Moondream2 GGUF • Real-time vision analysis",
        sizeMB = 1900,
        downloadUrl = "https://huggingface.co/vikhyatk/moondream2/resolve/main/moondream2-text-model-f16.gguf",
        sha256 = "placeholder_sha256_moondream",
        filename = "moondream2-q4.gguf",
        type = ModelType.VISION
    )

    val QWEN2_VL = ModelInfo(
        id = "qwen2_vl_2b",
        name = "Advanced Vision Mode",
        description = "Qwen2-VL-2B Q4 • Superior multimodal understanding",
        sizeMB = 3700,
        downloadUrl = "https://huggingface.co/Qwen/Qwen2-VL-2B-Instruct-GGUF/resolve/main/qwen2-vl-2b-instruct-q4_k_m.gguf",
        sha256 = "placeholder_sha256_qwen2_vl",
        filename = "qwen2-vl-2b-q4.gguf",
        type = ModelType.VISION
    )

    val WHISPER_TINY = ModelInfo(
        id = "whisper_tiny",
        name = "Speech Recognition (Fast)",
        description = "Whisper-Tiny int8 • 5-10x realtime on mobile",
        sizeMB = 145,
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
        sha256 = "placeholder_sha256_whisper_tiny",
        filename = "whisper-tiny-int8.bin",
        type = ModelType.AUDIO
    )

    val WHISPER_BASE = ModelInfo(
        id = "whisper_base",
        name = "Speech Recognition (Accurate)",
        description = "Whisper-Base int8 • Better accuracy, 3-5x realtime",
        sizeMB = 290,
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
        sha256 = "placeholder_sha256_whisper_base",
        filename = "whisper-base-int8.bin",
        type = ModelType.AUDIO
    )

    /**
     * Get all available models
     * Production models (Mistral-7B, MobileNet-v3, BGE) are prioritized
     */
    fun getAll() = listOf(
        // Production models (recommended for S24 Ultra)
        MISTRAL_7B_INT8,
        MOBILENET_V3_INT8,
        BGE_SMALL_INT8,
        WHISPER_TINY,
        WHISPER_BASE,
        // Legacy models (deprecated, kept for compatibility)
        MOONDREAM2,
        QWEN2_VL
    )

    /**
     * Get production models only
     */
    fun getProductionModels() = listOf(
        MISTRAL_7B_INT8,
        MOBILENET_V3_INT8,
        BGE_SMALL_INT8,
        WHISPER_TINY
    )
}

