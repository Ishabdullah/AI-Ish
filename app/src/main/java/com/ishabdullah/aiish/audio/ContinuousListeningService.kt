/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ContinuousListeningService - Background speech recognition
 *
 * Features:
 * - Runs in foreground with notification
 * - Continuous audio monitoring with VAD
 * - Low power consumption (only process when voice detected)
 * - Integration with wake word detection
 * - Automatic restart on failure
 */
class ContinuousListeningService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "aiish_listening"
        private const val CHANNEL_NAME = "Voice Listening"

        // Service actions
        const val ACTION_START = "com.ishabdullah.aiish.START_LISTENING"
        const val ACTION_STOP = "com.ishabdullah.aiish.STOP_LISTENING"

        // Listening configuration
        private const val CHUNK_DURATION_MS = 1000L  // Process 1s chunks
        private const val MAX_SILENCE_MS = 3000L     // Stop after 3s silence

        fun start(context: Context) {
            val intent = Intent(context, ContinuousListeningService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ContinuousListeningService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var audioRecorder: AudioRecorder? = null
    private var voskSTT: VoskSTT? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastTranscription = MutableStateFlow("")
    val lastTranscription: StateFlow<String> = _lastTranscription.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        Timber.i("ContinuousListeningService created")

        // Initialize components
        audioRecorder = AudioRecorder()
        voskSTT = VoskSTT(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startListening()
            ACTION_STOP -> stopListening()
        }
        return START_STICKY  // Restart if killed by system
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startListening() {
        if (_isListening.value) {
            Timber.w("Already listening")
            return
        }

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification("Listening..."))

        _isListening.value = true
        Timber.i("Continuous listening started")

        serviceScope.launch {
            try {
                // Initialize audio recorder
                if (audioRecorder?.initialize() != true) {
                    Timber.e("Failed to initialize audio recorder")
                    stopSelf()
                    return@launch
                }

                // Start continuous listening loop
                continuousListeningLoop()

            } catch (e: Exception) {
                Timber.e(e, "Error in listening service")
                stopSelf()
            }
        }
    }

    private fun stopListening() {
        _isListening.value = false
        audioRecorder?.release()
        voskSTT?.release()
        stopForeground(true)
        stopSelf()
        Timber.i("Continuous listening stopped")
    }

    /**
     * Continuous listening loop with VAD
     */
    private suspend fun continuousListeningLoop() {
        while (_isListening.value) {
            try {
                // Record audio chunk
                audioRecorder?.startRecording()
                delay(CHUNK_DURATION_MS)
                val audioData = audioRecorder?.stopRecording()

                if (audioData != null && audioData.isNotEmpty()) {
                    // Check for voice activity
                    val hasVoice = detectVoiceActivity(audioData)

                    if (hasVoice) {
                        // Transcribe audio
                        processAudioChunk(audioData)
                    }
                }

                // Small delay to reduce CPU usage
                delay(100)

            } catch (e: Exception) {
                Timber.e(e, "Error in listening loop")
                delay(1000)  // Wait before retry
            }
        }
    }

    /**
     * Detect voice activity in audio
     */
    private fun detectVoiceActivity(audioData: ShortArray): Boolean {
        var sum = 0.0
        for (sample in audioData) {
            sum += sample * sample
        }
        val energy = Math.sqrt(sum / audioData.size).toFloat()
        return energy > AudioRecorder.VAD_ENERGY_THRESHOLD
    }

    /**
     * Process audio chunk with Whisper
     */
    private suspend fun processAudioChunk(audioData: ShortArray) {
        try {
            if (voskSTT?.isLoaded() != true) {
                Timber.w("Whisper model not loaded, skipping transcription")
                return
            }

            // Convert to float array
            val floatData = audioRecorder?.convertToFloat(audioData) ?: return

            // Transcribe
            val result = voskSTT?.transcribe(floatData, enableTimestamps = false)

            if (result?.success == true && result.text.isNotBlank()) {
                _lastTranscription.value = result.text
                updateNotification("Heard: ${result.text.take(50)}...")
                Timber.d("Transcription: ${result.text}")

                // TODO: Send transcription to chat or command processor
                // broadcastTranscription(result.text)
            }

        } catch (e: Exception) {
            Timber.e(e, "Error processing audio chunk")
        }
    }

    /**
     * Create notification for foreground service
     */
    private fun createNotification(contentText: String): Notification {
        createNotificationChannel()

        // TODO: Replace with actual MainActivity intent
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(),
            PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("AI Ish")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("AI Ish")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }
    }

    /**
     * Update notification with new text
     */
    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(text))
    }

    /**
     * Create notification channel (Android O+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Voice listening service"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isListening.value = false
        audioRecorder?.release()
        voskSTT?.release()
        Timber.i("ContinuousListeningService destroyed")
    }
}
