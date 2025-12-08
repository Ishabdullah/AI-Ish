package com.ishabdullah.aiish.knowledge

import com.ishabdullah.aiish.domain.models.KnowledgeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * KnowledgeScout - Real-time knowledge fetching system
 * Ported from Adaptheon with 30+ production-grade fetchers
 */
class KnowledgeScout {

    private val registry = FetcherRegistry()
    private val cache = mutableMapOf<String, KnowledgeResult>()

    suspend fun search(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            Timber.d("🔍 KnowledgeScout searching: $query")

            // Check cache first
            cache[query]?.let {
                Timber.d("✓ Cache hit")
                return@withContext listOf(it.content)
            }

            // Route to appropriate fetchers
            val results = registry.fetch(query)

            if (results.isNotEmpty()) {
                val topResult = results.first()
                cache[query] = topResult
                Timber.d("✓ Found ${results.size} results")
                return@withContext results.map { it.content }
            }

            Timber.w("✗ No results found")
            emptyList()

        } catch (e: Exception) {
            Timber.e(e, "Knowledge fetch failed")
            emptyList()
        }
    }

    fun clearCache() {
        cache.clear()
    }
}
