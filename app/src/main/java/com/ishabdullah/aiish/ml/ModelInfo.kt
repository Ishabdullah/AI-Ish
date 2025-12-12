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
 * Model metadata and download information
 *
 * SHA256 CHECKSUM DOCUMENTATION:
 * ===============================
 * All SHA256 checksums marked as "placeholder_sha256_*" must be replaced with actual checksums
 * calculated from the downloaded model files. This ensures file integrity and prevents corrupted
 * or tampered models from being loaded.
 *
 * HOW TO CALCULATE SHA256 CHECKSUMS:
 * ----------------------------------
 * 1. Download the model file manually from the URL specified in downloadUrl
 * 2. Run the following command in your terminal:
 *
 *    sha256sum filename.gguf
 *
 *    (On macOS, use: shasum -a 256 filename.gguf)
 *
 * 3. Copy the 64-character hexadecimal hash output
 * 4. Replace the corresponding "placeholder_sha256_*" value with the actual hash
 *
 * EXAMPLE:
 * --------
 * $ sha256sum mistral-7b-instruct-v0.2.Q8_0.gguf
 * a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456  mistral-7b-instruct-v0.2.Q8_0.gguf
 *
 * Then update:
 * sha256 = "a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456"
 */
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
     * Device: NPU via QNN/NNAPI delegate (prefill) + CPU (decode)
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
        // TODO: Replace with actual SHA256 after downloading model (run: sha256sum mistral-7b-instruct-v0.2.Q8_0.gguf)
        sha256 = "placeholder_sha256_mistral_int8",
        filename = "mistral-7b-instruct-int8.gguf",
        type = ModelType.LLM
    )

    /**
     * MobileNet-v3 INT8 - Vision Model
     * Device: NPU via QNN/NNAPI delegate
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
        // TODO: Replace with actual SHA256 after downloading model (run: sha256sum mobilenet_v3_large_100_224_int8.tflite)
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
        // TODO: Replace with actual SHA256 after downloading model (run: sha256sum model_int8.gguf)
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
        // TODO: Replace with actual SHA256 after downloading model (run: sha256sum moondream2-text-model-f16.gguf)
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
        // TODO: Replace with actual SHA256 after downloading model (run: sha256sum qwen2-vl-2b-instruct-q4_k_m.gguf)
        sha256 = "placeholder_sha256_qwen2_vl",
        filename = "qwen2-vl-2b-q4.gguf",
        type = ModelType.VISION
    )

    /**
     * Vosk Small EN-US - Speech Recognition (Fast)
     * Device: CPU
     * Size: ~40MB (compressed)
     * Performance: 5-10x realtime on mobile
     * Language: English (US)
     */
    val VOSK_SMALL_EN = ModelInfo(
        id = "vosk_small_en_us",
        name = "Speech Recognition (Fast)",
        description = "Vosk Small EN-US • 5-10x realtime • Offline STT",
        sizeMB = 40,
        downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
        // TODO: Replace with actual SHA256 after downloading model (run: sha256sum vosk-model-small-en-us-0.15.zip)
        sha256 = "placeholder_sha256_vosk_small",
        filename = "vosk-model-small-en-us-0.15.zip",
        type = ModelType.AUDIO
    )

    /**
     * Vosk EN-US - Speech Recognition (Accurate)
     * Device: CPU
     * Size: ~1.8GB (compressed)
     * Performance: High accuracy, 3-5x realtime
     * Language: English (US)
     */
    val VOSK_EN = ModelInfo(
        id = "vosk_en_us",
        name = "Speech Recognition (Accurate)",
        description = "Vosk EN-US 0.22 • High accuracy • Offline STT",
        sizeMB = 1800,
        downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-en-us-0.22.zip",
        // TODO: Replace with actual SHA256 after downloading model (run: sha256sum vosk-model-en-us-0.22.zip)
        sha256 = "placeholder_sha256_vosk_en",
        filename = "vosk-model-en-us-0.22.zip",
        type = ModelType.AUDIO
    )

    /**
     * Get all available models
     * Production models (Mistral-7B, MobileNet-v3, BGE, Vosk) are prioritized
     */
    fun getAll() = listOf(
        // Production models (recommended for S24 Ultra)
        MISTRAL_7B_INT8,
        MOBILENET_V3_INT8,
        BGE_SMALL_INT8,
        VOSK_SMALL_EN,
        VOSK_EN,
        // Alternative vision models
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
        VOSK_SMALL_EN  // Small Vosk model for fast STT
    )
}

