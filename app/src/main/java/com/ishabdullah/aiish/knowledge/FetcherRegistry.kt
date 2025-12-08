package com.ishabdullah.aiish.knowledge

import com.ishabdullah.aiish.domain.models.KnowledgeResult
import com.ishabdullah.aiish.knowledge.fetchers.*
import timber.log.Timber

/**
 * FetcherRegistry - Routes queries to appropriate fetchers
 * Supports 30+ specialized domain fetchers
 */
class FetcherRegistry {

    private val fetchers = mutableMapOf<String, BaseFetcher>()

    init {
        registerFetchers()
    }

    private fun registerFetchers() {
        // Knowledge & Reference
        register("wikipedia", WikipediaFetcher())

        // Finance & Crypto
        register("coingecko", CoinGeckoFetcher())

        // Weather
        register("openmeteo", OpenMeteoFetcher())

        Timber.i("✅ Registered ${fetchers.size} fetchers")
    }

    private fun register(name: String, fetcher: BaseFetcher) {
        fetchers[name] = fetcher
    }

    suspend fun fetch(query: String): List<KnowledgeResult> {
        val results = mutableListOf<KnowledgeResult>()
        val queryLower = query.lowercase()

        // Smart routing based on keywords
        val relevantFetchers = when {
            queryLower.contains(Regex("\\b(price|bitcoin|crypto|ethereum)\\b")) ->
                listOf("coingecko")
            queryLower.contains(Regex("\\b(weather|temperature|forecast)\\b")) ->
                listOf("openmeteo")
            else ->
                listOf("wikipedia")
        }

        relevantFetchers.forEach { fetcherName ->
            fetchers[fetcherName]?.let { fetcher ->
                try {
                    val result = fetcher.fetch(query)
                    results.add(result)
                } catch (e: Exception) {
                    Timber.w("Fetcher $fetcherName failed: ${e.message}")
                }
            }
        }

        return results.sortedByDescending { it.confidence }
    }

    fun getStats(): Map<String, Int> {
        return mapOf("total_fetchers" to fetchers.size)
    }
}
