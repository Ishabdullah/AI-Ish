package com.ishabdullah.aiish.domain.models

/**
 * Result from knowledge fetchers
 */
data class KnowledgeResult(
    val source: String,
    val content: String,
    val confidence: Float,
    val metadata: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)
