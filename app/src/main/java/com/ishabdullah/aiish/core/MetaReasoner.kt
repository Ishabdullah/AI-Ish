package com.ishabdullah.aiish.core

import com.ishabdullah.aiish.knowledge.KnowledgeScout
import com.ishabdullah.aiish.math.MathReasoner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * MetaReasoner - The brain of AI Ish
 * Routes queries to appropriate subsystems and orchestrates responses
 */
class MetaReasoner(
    private val knowledgeScout: KnowledgeScout,
    private val mathReasoner: MathReasoner
) {

    /**
     * Process user query and stream response
     */
    suspend fun processQuery(query: String): Flow<String> = flow {
        try {
            Timber.d("Processing query: $query")

            // Determine query type and route accordingly
            val queryType = classifyQuery(query)

            when (queryType) {
                QueryType.MATH -> {
                    // Use deterministic math reasoner
                    val result = mathReasoner.solve(query)
                    emit(result)
                }

                QueryType.KNOWLEDGE -> {
                    // Fetch real-time knowledge
                    emit("🔍 Fetching knowledge...\n\n")
                    val knowledge = knowledgeScout.search(query)

                    if (knowledge.isNotEmpty()) {
                        emit(knowledge.joinToString("\n\n"))
                    } else {
                        emit("No knowledge found for this query.")
                    }
                }

                QueryType.GENERAL -> {
                    // Standard response
                    emit("I'm AI Ish, your private on-device companion. How can I help?")
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Error processing query")
            emit("❌ Error: ${e.localizedMessage}")
        }
    }

    private fun classifyQuery(query: String): QueryType {
        val lowerQuery = query.lowercase()

        return when {
            // Math patterns
            lowerQuery.matches(Regex(".*\\b(solve|calculate|compute)\\b.*")) ||
                    lowerQuery.contains(Regex("[0-9]+\\s*[+\\-*/^]\\s*[0-9]+")) -> QueryType.MATH

            // Knowledge patterns
            lowerQuery.contains(Regex("\\b(price|weather|news|what is|who is)\\b")) -> QueryType.KNOWLEDGE

            else -> QueryType.GENERAL
        }
    }

    enum class QueryType {
        MATH,
        KNOWLEDGE,
        GENERAL
    }
}
