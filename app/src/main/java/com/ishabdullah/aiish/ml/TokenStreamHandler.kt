/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.ml

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Handles token streaming from LLM inference and provides UI-ready state updates
 *
 * Manages:
 * - Token accumulation for partial response display
 * - Streaming state (idle, streaming, complete, error)
 * - Token count tracking
 * - Performance metrics (tokens/sec)
 */
class TokenStreamHandler {

    sealed class StreamState {
        object Idle : StreamState()
        data class Streaming(val partialText: String, val tokenCount: Int) : StreamState()
        data class Complete(val fullText: String, val tokenCount: Int, val durationMs: Long) : StreamState()
        data class Error(val message: String) : StreamState()
    }

    private val _state = MutableStateFlow<StreamState>(StreamState.Idle)
    val state: StateFlow<StreamState> = _state.asStateFlow()

    private val _tokensPerSecond = MutableStateFlow(0f)
    val tokensPerSecond: StateFlow<Float> = _tokensPerSecond.asStateFlow()

    /**
     * Process a token stream from LLM inference engine
     *
     * @param tokenFlow Flow emitting tokens as they're generated
     * @return Final generated text
     */
    suspend fun processStream(tokenFlow: Flow<String>): String {
        val startTime = System.currentTimeMillis()
        val textBuilder = StringBuilder()
        var tokenCount = 0

        try {
            _state.value = StreamState.Streaming("", 0)

            tokenFlow.collect { token ->
                textBuilder.append(token)
                tokenCount++

                // Update streaming state
                _state.value = StreamState.Streaming(
                    partialText = textBuilder.toString(),
                    tokenCount = tokenCount
                )

                // Calculate tokens/sec
                val elapsedMs = System.currentTimeMillis() - startTime
                if (elapsedMs > 0) {
                    val tokensPerSec = (tokenCount * 1000f) / elapsedMs
                    _tokensPerSecond.value = tokensPerSec
                }

                Timber.v("Token $tokenCount: '$token' (${tokensPerSec.value.format(1)} t/s)")
            }

            // Generation complete
            val durationMs = System.currentTimeMillis() - startTime
            val finalText = textBuilder.toString()

            _state.value = StreamState.Complete(
                fullText = finalText,
                tokenCount = tokenCount,
                durationMs = durationMs
            )

            Timber.i("Stream complete: $tokenCount tokens in ${durationMs}ms (${tokensPerSecond.value.format(2)} t/s)")

            return finalText

        } catch (e: Exception) {
            Timber.e(e, "Error processing token stream")
            _state.value = StreamState.Error(e.message ?: "Unknown error")
            throw e
        }
    }

    /**
     * Reset handler state to idle
     */
    fun reset() {
        _state.value = StreamState.Idle
        _tokensPerSecond.value = 0f
        Timber.d("TokenStreamHandler reset")
    }

    /**
     * Check if currently streaming
     */
    fun isStreaming(): Boolean {
        return _state.value is StreamState.Streaming
    }

    /**
     * Get current partial text (if streaming)
     */
    fun getCurrentText(): String? {
        return when (val current = _state.value) {
            is StreamState.Streaming -> current.partialText
            is StreamState.Complete -> current.fullText
            else -> null
        }
    }

    private fun Float.format(decimals: Int): String {
        return "%.${decimals}f".format(this)
    }
}
