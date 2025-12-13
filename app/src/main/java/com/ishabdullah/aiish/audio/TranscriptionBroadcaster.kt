package com.ishabdullah.aiish.audio

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Singleton object to broadcast transcriptions from ContinuousListeningService
 * to other parts of the application, e.g., ChatViewModel.
 */
object TranscriptionBroadcaster {
    private val _transcriptionFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val transcriptionFlow = _transcriptionFlow.asSharedFlow()

    suspend fun emitTranscription(transcription: String) {
        _transcriptionFlow.emit(transcription)
    }
}
