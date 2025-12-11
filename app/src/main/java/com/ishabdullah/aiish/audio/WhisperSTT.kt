/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * WhisperSTT - Speech-to-Text using Whisper model
 *
 * Supports:
 * - Whisper-Tiny (145MB int8) - Fast, 5-10x realtime on mobile
 * - Whisper-Base (290MB int8) - Better accuracy
 * - Real-time transcription with streaming
 * - Multiple languages (auto-detect or specify)
 * - Punctuation and capitalization
 */
class WhisperSTT {

    companion object {
        init {
            try {
                System.loadLibrary("aiish_native")
                Timber.i("WhisperSTT: Native library loaded")
            } catch (e: UnsatisfiedLinkError) {
                Timber.e(e, "Failed to load native library for Whisper")
            }
        }

        // Whisper model sizes
        const val MODEL_TINY = "tiny"    // 145MB, fastest
        const val MODEL_BASE = "base"    // 290MB, better accuracy
        const val MODEL_SMALL = "small"  // 967MB, high accuracy (too slow for mobile)

        // Languages
        const val LANG_AUTO = "auto"     // Auto-detect
        const val LANG_EN = "en"         // English
        const val LANG_ES = "es"         // Spanish
        const val LANG_FR = "fr"         // French
        const val LANG_DE = "de"         // German
        const val LANG_ZH = "zh"         // Chinese
        const val LANG_JA = "ja"         // Japanese
        const val LANG_AR = "ar"         // Arabic
    }

    private var isModelLoaded = false
    private var currentModelPath: String? = null
    private var currentLanguage = LANG_AUTO

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _liveTranscription = MutableStateFlow("")
    val liveTranscription: StateFlow<String> = _liveTranscription.asStateFlow()

    // Native methods
    private external fun nativeLoadWhisperModel(
        modelPath: String,
        language: String
    ): Boolean

    private external fun nativeTranscribe(
        audioData: FloatArray,
        audioLength: Int,
        enableTimestamps: Boolean
    ): String

    private external fun nativeTranscribeStreaming(
        audioData: FloatArray,
        audioLength: Int
    ): String

    private external fun nativeGetLanguage(): String
    private external fun nativeReleaseWhisperModel()

    /**
     * Load Whisper model
     */
    suspend fun loadModel(
        modelFile: File,
        language: String = LANG_AUTO
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isModelLoaded && currentModelPath == modelFile.absolutePath) {
                Timber.d("Whisper model already loaded: ${modelFile.name}")
                return@withContext true
            }

            if (!modelFile.exists()) {
                Timber.e("Whisper model file not found: ${modelFile.absolutePath}")
                return@withContext false
            }

            Timber.i("Loading Whisper model: ${modelFile.name} (${modelFile.length() / 1024 / 1024}MB)")

            val success = nativeLoadWhisperModel(
                modelPath = modelFile.absolutePath,
                language = language
            )

            if (success) {
                isModelLoaded = true
                currentModelPath = modelFile.absolutePath
                currentLanguage = language
                Timber.i("Whisper model loaded successfully (lang=$language)")
            } else {
                Timber.e("Failed to load Whisper model")
            }

            success

        } catch (e: Exception) {
            Timber.e(e, "Error loading Whisper model")
            false
        }
    }

    /**
     * Transcribe audio to text
     */
    suspend fun transcribe(
        audioData: FloatArray,
        enableTimestamps: Boolean = false
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        try {
            if (!isModelLoaded) {
                return@withContext TranscriptionResult(
                    success = false,
                    text = "",
                    language = "",
                    processingTimeMs = 0,
                    error = "Whisper model not loaded"
                )
            }

            _isProcessing.value = true
            val startTime = System.currentTimeMillis()

            val transcribedText = nativeTranscribe(
                audioData = audioData,
                audioLength = audioData.size,
                enableTimestamps = enableTimestamps
            )

            val processingTime = System.currentTimeMillis() - startTime
            val detectedLanguage = nativeGetLanguage()

            _isProcessing.value = false

            TranscriptionResult(
                success = true,
                text = transcribedText.trim(),
                language = detectedLanguage,
                processingTimeMs = processingTime,
                error = null
            )

        } catch (e: Exception) {
            _isProcessing.value = false
            Timber.e(e, "Error during transcription")
            TranscriptionResult(
                success = false,
                text = "",
                language = "",
                processingTimeMs = 0,
                error = e.message
            )
        }
    }

    /**
     * Transcribe audio from file
     */
    suspend fun transcribeFile(
        audioFile: File,
        enableTimestamps: Boolean = false
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        try {
            // TODO: Load audio file and convert to FloatArray
            // For now, return placeholder
            TranscriptionResult(
                success = false,
                text = "",
                language = "",
                processingTimeMs = 0,
                error = "File transcription not yet implemented"
            )
        } catch (e: Exception) {
            Timber.e(e, "Error transcribing file")
            TranscriptionResult(
                success = false,
                text = "",
                language = "",
                processingTimeMs = 0,
                error = e.message
            )
        }
    }

    /**
     * Transcribe audio with streaming (for real-time)
     */
    suspend fun transcribeStreaming(audioData: FloatArray): String = withContext(Dispatchers.IO) {
        try {
            if (!isModelLoaded) {
                return@withContext ""
            }

            val partial = nativeTranscribeStreaming(
                audioData = audioData,
                audioLength = audioData.size
            )

            _liveTranscription.value = partial
            partial

        } catch (e: Exception) {
            Timber.e(e, "Error during streaming transcription")
            ""
        }
    }

    /**
     * Set language for transcription
     */
    fun setLanguage(language: String) {
        currentLanguage = language
        Timber.d("Whisper language set to: $language")
    }

    /**
     * Get current language
     */
    fun getLanguage(): String = currentLanguage

    /**
     * Check if model is loaded
     */
    fun isLoaded(): Boolean = isModelLoaded

    /**
     * Get current model path
     */
    fun getModelPath(): String? = currentModelPath

    /**
     * Estimate processing speed
     * @return Speed multiplier (e.g., 5.0 = 5x realtime)
     */
    fun estimateSpeed(audioLengthSeconds: Float, processingTimeMs: Long): Float {
        val processingTimeSeconds = processingTimeMs / 1000f
        return audioLengthSeconds / processingTimeSeconds
    }

    /**
     * Release model and free resources
     */
    fun release() {
        if (isModelLoaded) {
            try {
                nativeReleaseWhisperModel()
                Timber.i("Whisper model released")
            } catch (e: Exception) {
                Timber.e(e, "Error releasing Whisper model")
            }
        }
        isModelLoaded = false
        currentModelPath = null
        _liveTranscription.value = ""
    }
}

/**
 * Transcription result
 */
data class TranscriptionResult(
    val success: Boolean,
    val text: String,
    val language: String,
    val processingTimeMs: Long,
    val error: String? = null,
    val timestamps: List<TimestampedSegment> = emptyList()
)

/**
 * Timestamped segment (for word-level timestamps)
 */
data class TimestampedSegment(
    val text: String,
    val startMs: Long,
    val endMs: Long
)
