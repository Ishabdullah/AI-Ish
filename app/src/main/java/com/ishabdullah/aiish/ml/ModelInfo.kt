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
    INSTANT,
    DEEP,
    VISION
}

object ModelCatalog {
    val PHI4_MINI = ModelInfo(
        id = "phi4_mini_onnx",
        name = "Instant Mode",
        description = "Phi-4-mini ONNX • Lightning-fast responses",
        sizeMB = 520,
        downloadUrl = "https://huggingface.co/microsoft/phi-4/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/phi-4-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx",
        sha256 = "placeholder_sha256_phi4",
        filename = "phi4-mini-instruct.onnx",
        type = ModelType.INSTANT
    )

    val QWEN2_7B = ModelInfo(
        id = "qwen2_7b_gguf",
        name = "Deep Mode",
        description = "Qwen2-7B-Instruct Q4_K_M • Advanced reasoning",
        sizeMB = 4800,
        downloadUrl = "https://huggingface.co/Qwen/Qwen2-7B-Instruct-GGUF/resolve/main/qwen2-7b-instruct-q4_k_m.gguf",
        sha256 = "placeholder_sha256_qwen2",
        filename = "qwen2-7b-instruct-q4_k_m.gguf",
        type = ModelType.DEEP
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

    fun getAll() = listOf(PHI4_MINI, QWEN2_7B, MOONDREAM2, QWEN2_VL)
}
