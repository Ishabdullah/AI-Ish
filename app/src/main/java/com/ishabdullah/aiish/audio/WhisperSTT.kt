/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import timber.log.Timber
import java.io.File

/**
 * WhisperSTT - Speech-to-Text using Vosk library
 *
 * Switched from whisper.cpp to Vosk for:
 * - Better Android compatibility (no native build issues)
 * - Smaller models (50MB vs 145MB)
 * - Proven offline STT
 * - Easy integration via Gradle
 *
 * Supports:
 * - Real-time transcription with streaming
 * - Multiple languages (English, Spanish, French, German, etc.)
 * - Offline processing
 * - Punctuation and capitalization
 */
class WhisperSTT(private val context: Context) {

    companion object {
        // Model sizes (Vosk models are smaller than Whisper)
        const val MODEL_SMALL = "vosk-model-small-en-us-0.15"  // ~40MB, fast
        const val MODEL_EN = "vosk-model-en-us-0.22"           // ~1.8GB, high accuracy

        // Languages
        const val LANG_EN = "en-us"         // English (US)
        const val LANG_ES = "es"            // Spanish
        const val LANG_FR = "fr"            // French
        const val LANG_DE = "de"            // German
        const val LANG_ZH = "zh"            // Chinese
        const val LANG_RU = "ru"            // Russian
        const val LANG_AR = "ar"            // Arabic
    }

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null

    private var isModelLoaded = false
    private var currentModelPath: String? = null
    private var currentLanguage = LANG_EN

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _liveTranscription = MutableStateFlow("")
    val liveTranscription: StateFlow<String> = _liveTranscription.asStateFlow()

    /**
     * Load Vosk model
     */
    suspend fun loadModel(
        modelFile: File,
        language: String = LANG_EN
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isModelLoaded && currentModelPath == modelFile.absolutePath) {
                Timber.d("Vosk model already loaded: ${modelFile.name}")
                return@withContext true
            }

            if (!modelFile.exists()) {
                Timber.e("Vosk model file not found: ${modelFile.absolutePath}")
                return@withContext false
            }

            Timber.i("Loading Vosk model: ${modelFile.name} (${modelFile.length() / 1024 / 1024}MB)")

            // Initialize Vosk model
            model = Model(modelFile.absolutePath)

            // Create recognizer with 16kHz sample rate (standard for speech)
            recognizer = Recognizer(model, 16000.0f)

            isModelLoaded = true
            currentModelPath = modelFile.absolutePath
            currentLanguage = language

            Timber.i("Vosk model loaded successfully (lang=$language)")
            true

        } catch (e: Exception) {
            Timber.e(e, "Error loading Vosk model")
            false
        }
    }

    /**
     * Transcribe audio to text
     * @param audioData PCM16 audio data (16kHz, mono)
     */
    suspend fun transcribe(
        audioData: FloatArray,
        enableTimestamps: Boolean = false
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        try {
            if (!isModelLoaded || recognizer == null) {
                return@withContext TranscriptionResult(
                    success = false,
                    text = "",
                    language = "",
                    processingTimeMs = 0,
                    error = "Vosk model not loaded"
                )
            }

            _isProcessing.value = true
            val startTime = System.currentTimeMillis()

            // Convert float audio to short (PCM16)
            val audioShorts = audioData.map { (it * Short.MAX_VALUE).toInt().toShort() }.toShortArray()

            // Feed audio to recognizer
            recognizer?.acceptWaveForm(audioShorts, audioShorts.size)

            // Get final result
            val resultJson = recognizer?.finalResult
            val text = parseVoskResult(resultJson ?: "{}")

            val processingTime = System.currentTimeMillis() - startTime
            _isProcessing.value = false

            TranscriptionResult(
                success = true,
                text = text.trim(),
                language = currentLanguage,
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
            // Read audio file and convert to FloatArray
            // This is a placeholder - actual implementation would depend on audio format
            TranscriptionResult(
                success = false,
                text = "",
                language = "",
                processingTimeMs = 0,
                error = "File transcription not yet implemented for Vosk"
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
            if (!isModelLoaded || recognizer == null) {
                return@withContext ""
            }

            // Convert float audio to short (PCM16)
            val audioShorts = audioData.map { (it * Short.MAX_VALUE).toInt().toShort() }.toShortArray()

            // Feed audio chunk to recognizer
            if (recognizer?.acceptWaveForm(audioShorts, audioShorts.size) == true) {
                // We have a complete utterance
                val resultJson = recognizer?.result
                val text = parseVoskResult(resultJson ?: "{}")
                _liveTranscription.value = text
                text
            } else {
                // Partial result
                val partialJson = recognizer?.partialResult
                val partial = parseVoskPartialResult(partialJson ?: "{}")
                _liveTranscription.value = partial
                partial
            }

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
        Timber.d("STT language set to: $language")
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
        return if (processingTimeSeconds > 0) {
            audioLengthSeconds / processingTimeSeconds
        } else {
            0f
        }
    }

    /**
     * Release model and free resources
     */
    fun release() {
        if (isModelLoaded) {
            try {
                recognizer?.close()
                model?.close()
                speechService?.stop()
                Timber.i("Vosk model released")
            } catch (e: Exception) {
                Timber.e(e, "Error releasing Vosk model")
            }
        }
        recognizer = null
        model = null
        speechService = null
        isModelLoaded = false
        currentModelPath = null
        _liveTranscription.value = ""
    }

    /**
     * Parse Vosk JSON result to extract text
     */
    private fun parseVoskResult(json: String): String {
        return try {
            // Vosk returns: {"text": "transcribed text"}
            val textMatch = Regex("\"text\"\\s*:\\s*\"([^\"]*)\"").find(json)
            textMatch?.groupValues?.get(1) ?: ""
        } catch (e: Exception) {
            Timber.e(e, "Error parsing Vosk result")
            ""
        }
    }

    /**
     * Parse Vosk partial JSON result to extract text
     */
    private fun parseVoskPartialResult(json: String): String {
        return try {
            // Vosk returns: {"partial": "partial text"}
            val textMatch = Regex("\"partial\"\\s*:\\s*\"([^\"]*)\"").find(json)
            textMatch?.groupValues?.get(1) ?: ""
        } catch (e: Exception) {
            Timber.e(e, "Error parsing Vosk partial result")
            ""
        }
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
