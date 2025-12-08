package com.ishabdullah.aiish.domain.models

/**
 * Configuration for LLM models
 */
data class ModelConfig(
    val name: String,
    val type: ModelType,
    val path: String,
    val contextLength: Int = 2048,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40
)

enum class ModelType {
    ONNX_PHI4,
    GGUF_QWEN2,
    GGUF_CODELLAMA
}
