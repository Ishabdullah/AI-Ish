package com.ishabdullah.aiish.memory

import timber.log.Timber

/**
 * MemoryManager - Manages semantic and episodic memory
 * Ported from Adaptheon memory systems
 */
class MemoryManager {

    private val semanticMemory = mutableMapOf<String, String>()
    private val episodicMemory = mutableListOf<Episode>()

    fun storeSemanticFact(key: String, value: String) {
        semanticMemory[key] = value
        Timber.d("Stored semantic fact: $key")
    }

    fun retrieveSemanticFact(key: String): String? {
        return semanticMemory[key]
    }

    fun storeEpisode(query: String, response: String) {
        val episode = Episode(
            query = query,
            response = response,
            timestamp = System.currentTimeMillis()
        )
        episodicMemory.add(episode)
        Timber.d("Stored episode: ${query.take(50)}")
    }

    fun getRecentEpisodes(count: Int = 5): List<Episode> {
        return episodicMemory.takeLast(count)
    }

    fun clearMemory() {
        semanticMemory.clear()
        episodicMemory.clear()
        Timber.i("Memory cleared")
    }
}

data class Episode(
    val query: String,
    val response: String,
    val timestamp: Long
)
