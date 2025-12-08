package com.ishabdullah.aiish.knowledge.fetchers

import com.ishabdullah.aiish.domain.models.KnowledgeResult

/**
 * Base interface for all knowledge fetchers
 */
abstract class BaseFetcher {
    abstract suspend fun fetch(query: String): KnowledgeResult

    protected fun createResult(
        source: String,
        content: String,
        confidence: Float,
        metadata: Map<String, Any> = emptyMap()
    ): KnowledgeResult {
        return KnowledgeResult(
            source = source,
            content = content,
            confidence = confidence,
            metadata = metadata
        )
    }
}
