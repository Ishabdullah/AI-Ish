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
import android.media.AudioAttributes
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import kotlin.coroutines.resume

/**
 * TTSManager - Text-to-Speech using Android TTS
 *
 * Features:
 * - High-quality voice synthesis using Android TTS
 * - Multiple voice options (male/female)
 * - Speed and pitch control
 * - Queue management for long responses
 * - Utterance progress tracking
 * - Automatic fallback to default voice
 */
class TTSManager(private val context: Context) {

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentUtterance = MutableStateFlow("")
    val currentUtterance: StateFlow<String> = _currentUtterance.asStateFlow()

    // TTS Configuration
    private var speechRate = 1.0f        // 0.5 = slow, 1.0 = normal, 2.0 = fast
    private var pitch = 1.0f             // 0.5 = low, 1.0 = normal, 2.0 = high
    private var preferredLocale = Locale.US

    companion object {
        // Voice preferences
        const val VOICE_DEFAULT = "default"
        const val VOICE_MALE = "male"
        const val VOICE_FEMALE = "female"

        // Speech rates
        const val RATE_SLOW = 0.75f
        const val RATE_NORMAL = 1.0f
        const val RATE_FAST = 1.25f
        const val RATE_VERY_FAST = 1.5f
    }

    /**
     * Initialize TTS engine
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    setupTTS()
                    isInitialized = true
                    Timber.i("TTS initialized successfully")
                    continuation.resume(true)
                } else {
                    Timber.e("TTS initialization failed: $status")
                    continuation.resume(false)
                }
            }

            continuation.invokeOnCancellation {
                if (!continuation.isCompleted) {
                    continuation.resume(false)
                }
            }
        }
    }

    /**
     * Setup TTS configuration
     */
    private fun setupTTS() {
        textToSpeech?.apply {
            // Set language
            val result = setLanguage(preferredLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Timber.w("Locale $preferredLocale not supported, falling back to default")
                setLanguage(Locale.US)
            }

            // Set default speech rate and pitch
            setSpeechRate(speechRate)
            setPitch(pitch)

            // Set audio attributes for voice assistant
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                setAudioAttributes(audioAttributes)
            }

            // Set utterance progress listener
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    Timber.d("TTS started: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentUtterance.value = ""
                    Timber.d("TTS finished: $utteranceId")
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentUtterance.value = ""
                    Timber.e("TTS error: $utteranceId")
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    _currentUtterance.value = ""
                    Timber.e("TTS error: $utteranceId (code: $errorCode)")
                }
            })
        }
    }

    /**
     * Speak text
     */
    suspend fun speak(
        text: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH
    ): Boolean = withContext(Dispatchers.Main) {
        if (!isInitialized) {
            Timber.w("TTS not initialized")
            return@withContext false
        }

        if (text.isBlank()) {
            Timber.w("Cannot speak empty text")
            return@withContext false
        }

        try {
            _currentUtterance.value = text

            val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                null // Use bundle instead
            } else {
                hashMapOf(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to text.hashCode().toString())
            }

            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech?.speak(text, queueMode, null, text.hashCode().toString())
            } else {
                @Suppress("DEPRECATION")
                textToSpeech?.speak(text, queueMode, params)
            }

            if (result == TextToSpeech.SUCCESS) {
                Timber.d("Speaking: ${text.take(50)}...")
                true
            } else {
                Timber.e("Failed to speak text: $result")
                false
            }

        } catch (e: Exception) {
            Timber.e(e, "Error speaking text")
            false
        }
    }

    /**
     * Speak text with custom rate and pitch
     */
    suspend fun speakWithConfig(
        text: String,
        rate: Float = speechRate,
        pitch: Float = this.pitch
    ): Boolean {
        setSpeechRate(rate)
        setPitch(pitch)
        return speak(text)
    }

    /**
     * Stop speaking
     */
    fun stop() {
        try {
            textToSpeech?.stop()
            _isSpeaking.value = false
            _currentUtterance.value = ""
            Timber.d("TTS stopped")
        } catch (e: Exception) {
            Timber.e(e, "Error stopping TTS")
        }
    }

    /**
     * Set speech rate
     */
    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.1f, 3.0f)
        textToSpeech?.setSpeechRate(speechRate)
        Timber.d("Speech rate set to: $speechRate")
    }

    /**
     * Set pitch
     */
    fun setPitch(pitch: Float) {
        this.pitch = pitch.coerceIn(0.5f, 2.0f)
        textToSpeech?.setPitch(this.pitch)
        Timber.d("Pitch set to: ${this.pitch}")
    }

    /**
     * Set language
     */
    fun setLanguage(locale: Locale): Boolean {
        preferredLocale = locale
        val result = textToSpeech?.setLanguage(locale)
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    /**
     * Set voice (if supported)
     */
    fun setVoice(voiceType: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val voices = textToSpeech?.voices ?: return false

                // Find matching voice
                val selectedVoice = voices.find { voice ->
                    when (voiceType) {
                        VOICE_MALE -> voice.name.contains("male", ignoreCase = true) &&
                                     !voice.name.contains("female", ignoreCase = true)
                        VOICE_FEMALE -> voice.name.contains("female", ignoreCase = true)
                        else -> true
                    }
                }

                if (selectedVoice != null) {
                    val result = textToSpeech?.setVoice(selectedVoice)
                    Timber.i("Voice set to: ${selectedVoice.name}")
                    return result == TextToSpeech.SUCCESS
                }
            } catch (e: Exception) {
                Timber.e(e, "Error setting voice")
            }
        }
        return false
    }

    /**
     * Get available voices
     */
    fun getAvailableVoices(): List<String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                return textToSpeech?.voices?.map { it.name } ?: emptyList()
            } catch (e: Exception) {
                Timber.e(e, "Error getting voices")
            }
        }
        return emptyList()
    }

    /**
     * Get available languages
     */
    fun getAvailableLanguages(): Set<Locale> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                return textToSpeech?.availableLanguages ?: emptySet()
            } catch (e: Exception) {
                Timber.e(e, "Error getting languages")
            }
        }
        return emptySet()
    }

    /**
     * Check if TTS is initialized
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * Get current speech rate
     */
    fun getSpeechRate(): Float = speechRate

    /**
     * Get current pitch
     */
    fun getPitch(): Float = pitch

    /**
     * Get current locale
     */
    fun getLocale(): Locale = preferredLocale

    /**
     * Release TTS resources
     */
    fun release() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isInitialized = false
            _isSpeaking.value = false
            _currentUtterance.value = ""
            Timber.d("TTS released")
        } catch (e: Exception) {
            Timber.e(e, "Error releasing TTS")
        }
    }
}
