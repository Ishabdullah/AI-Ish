/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AudioRecorder - Record audio from microphone
 *
 * Optimized for Whisper STT:
 * - 16kHz sample rate (Whisper's native rate)
 * - 16-bit PCM mono
 * - Efficient buffering for real-time processing
 * - Voice Activity Detection (VAD) support
 */
class AudioRecorder {

    companion object {
        // Audio configuration for Whisper
        const val SAMPLE_RATE = 16000        // 16kHz (Whisper native)
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BYTES_PER_SAMPLE = 2       // 16-bit = 2 bytes

        // Buffer configuration
        const val BUFFER_SIZE_MULTIPLIER = 2
        const val RECORDING_CHUNK_SIZE = 1024 // samples per chunk

        // VAD configuration
        const val VAD_ENERGY_THRESHOLD = 500f  // Adjust based on testing
        const val VAD_SILENCE_DURATION_MS = 1500L  // 1.5s silence = end of speech
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    private val _audioData = MutableStateFlow<ShortArray?>(null)
    val audioData: StateFlow<ShortArray?> = _audioData.asStateFlow()

    private val _isRecordingState = MutableStateFlow(false)
    val isRecordingState: StateFlow<Boolean> = _isRecordingState.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    /**
     * Initialize audio recorder
     */
    fun initialize(): Boolean {
        return try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            ) * BUFFER_SIZE_MULTIPLIER

            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Timber.e("Invalid buffer size: $bufferSize")
                return false
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            val state = audioRecord?.state
            if (state != AudioRecord.STATE_INITIALIZED) {
                Timber.e("AudioRecord initialization failed: state=$state")
                return false
            }

            Timber.i("AudioRecorder initialized: ${SAMPLE_RATE}Hz, buffer=$bufferSize")
            true

        } catch (e: SecurityException) {
            Timber.e(e, "Audio recording permission denied")
            false
        } catch (e: Exception) {
            Timber.e(e, "Error initializing AudioRecorder")
            false
        }
    }

    /**
     * Start recording audio
     */
    suspend fun startRecording() = withContext(Dispatchers.IO) {
        if (isRecording) {
            Timber.w("Already recording")
            return@withContext
        }

        if (audioRecord == null && !initialize()) {
            Timber.e("Failed to initialize audio recorder")
            return@withContext
        }

        try {
            audioRecord?.startRecording()
            isRecording = true
            _isRecordingState.value = true

            Timber.i("Recording started")

            // Start recording thread
            recordingThread = Thread {
                recordAudioData()
            }.apply { start() }

        } catch (e: Exception) {
            Timber.e(e, "Error starting recording")
            isRecording = false
            _isRecordingState.value = false
        }
    }

    /**
     * Stop recording audio
     */
    suspend fun stopRecording(): ShortArray? = withContext(Dispatchers.IO) {
        if (!isRecording) {
            Timber.w("Not recording")
            return@withContext null
        }

        try {
            isRecording = false
            _isRecordingState.value = false

            recordingThread?.join(1000)
            recordingThread = null

            audioRecord?.stop()

            val capturedAudio = _audioData.value
            Timber.i("Recording stopped: ${capturedAudio?.size ?: 0} samples")

            capturedAudio

        } catch (e: Exception) {
            Timber.e(e, "Error stopping recording")
            null
        }
    }

    /**
     * Record audio data in background thread
     */
    private fun recordAudioData() {
        val audioBuffer = ShortArray(RECORDING_CHUNK_SIZE)
        val recordedData = mutableListOf<Short>()
        var lastVoiceTimestamp = System.currentTimeMillis()
        val startTime = System.currentTimeMillis()

        try {
            while (isRecording) {
                val samplesRead = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0

                if (samplesRead > 0) {
                    // Append to recorded data
                    recordedData.addAll(audioBuffer.take(samplesRead))

                    // Update duration
                    val durationMs = System.currentTimeMillis() - startTime
                    _recordingDuration.value = durationMs

                    // Voice Activity Detection
                    val energy = calculateEnergy(audioBuffer, samplesRead)
                    if (energy > VAD_ENERGY_THRESHOLD) {
                        lastVoiceTimestamp = System.currentTimeMillis()
                    }

                    // Auto-stop on silence (optional)
                    val silenceDuration = System.currentTimeMillis() - lastVoiceTimestamp
                    if (silenceDuration > VAD_SILENCE_DURATION_MS && recordedData.size > SAMPLE_RATE) {
                        Timber.d("Voice activity ended (${silenceDuration}ms silence)")
                        break
                    }
                }
            }

            // Store final audio data
            _audioData.value = recordedData.toShortArray()

        } catch (e: Exception) {
            Timber.e(e, "Error during audio recording")
        }
    }

    /**
     * Calculate audio energy for VAD
     */
    private fun calculateEnergy(buffer: ShortArray, length: Int): Float {
        var sum = 0.0
        for (i in 0 until length) {
            sum += buffer[i] * buffer[i]
        }
        return Math.sqrt(sum / length).toFloat()
    }

    /**
     * Save audio to WAV file
     */
    suspend fun saveToWavFile(audioData: ShortArray, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            FileOutputStream(outputFile).use { fos ->
                // Write WAV header
                writeWavHeader(fos, audioData.size)

                // Write audio data
                val buffer = ByteBuffer.allocate(audioData.size * 2)
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                for (sample in audioData) {
                    buffer.putShort(sample)
                }
                fos.write(buffer.array())
            }

            Timber.i("Audio saved to: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            true

        } catch (e: Exception) {
            Timber.e(e, "Error saving audio to WAV")
            false
        }
    }

    /**
     * Write WAV file header
     */
    private fun writeWavHeader(fos: FileOutputStream, audioLength: Int) {
        val totalDataLen = audioLength * 2 + 36
        val channels = 1
        val byteRate = SAMPLE_RATE * channels * 2

        val header = ByteBuffer.allocate(44)
        header.order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        header.put("RIFF".toByteArray())
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray())

        // fmt chunk
        header.put("fmt ".toByteArray())
        header.putInt(16) // fmt chunk size
        header.putShort(1.toShort()) // audio format (PCM)
        header.putShort(channels.toShort())
        header.putInt(SAMPLE_RATE)
        header.putInt(byteRate)
        header.putShort((channels * 2).toShort()) // block align
        header.putShort(16.toShort()) // bits per sample

        // data chunk
        header.put("data".toByteArray())
        header.putInt(audioLength * 2)

        fos.write(header.array())
    }

    /**
     * Convert ShortArray to FloatArray for Whisper
     */
    fun convertToFloat(audioData: ShortArray): FloatArray {
        return FloatArray(audioData.size) { i ->
            audioData[i] / 32768f // Normalize to [-1, 1]
        }
    }

    /**
     * Release resources
     */
    fun release() {
        try {
            isRecording = false
            recordingThread?.join(1000)
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            _audioData.value = null
            _recordingDuration.value = 0L
            Timber.d("AudioRecorder released")
        } catch (e: Exception) {
            Timber.e(e, "Error releasing AudioRecorder")
        }
    }
}
