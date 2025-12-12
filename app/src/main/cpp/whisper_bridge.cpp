/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

/*
 * ================================================================================================
 * WHISPER.CPP JNI BRIDGE - FULL IMPLEMENTATION
 * ================================================================================================
 *
 * This file provides complete JNI bindings for whisper.cpp library to enable on-device
 * speech-to-text (STT) on Android devices. It supports:
 *
 * - Whisper model loading (tiny, base, small, medium, large)
 * - Real-time audio transcription
 * - Multi-language support with language detection
 * - ARM NEON optimizations for mobile CPUs
 * - GPU acceleration via OpenCL (when enabled)
 * - INT8 quantized models for mobile efficiency
 *
 * ================================================================================================
 */

#include <jni.h>
#include <string>
#include <vector>
#include <mutex>
#include <android/log.h>

// Include whisper.cpp headers
#include "whisper.h"

#define LOG_TAG "WhisperBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

//=============================================================================
// GLOBAL STATE
//=============================================================================

// Global Whisper context
static whisper_context* g_whisper_ctx = nullptr;
static std::mutex g_whisper_mutex;
static std::string g_detected_language = "en";

// Track initialization
static bool g_whisper_initialized = false;

//=============================================================================
// HELPER FUNCTIONS
//=============================================================================

/**
 * Get default whisper parameters for mobile
 */
static whisper_full_params get_default_params() {
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);

    // Mobile optimizations
    params.n_threads = 4;  // Optimize for mobile CPUs
    params.translate = false;
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.no_context = true;  // Disable context for faster processing
    params.single_segment = false;
    params.max_len = 0;  // No max segment length

    // Language settings (will be overridden)
    params.language = "en";
    params.detect_language = false;

    // Suppression
    params.suppress_blank = true;
    params.suppress_non_speech_tokens = true;

    // Beam search (greedy for speed on mobile)
    params.beam_size = 1;
    params.best_of = 1;

    // Temperature fallback
    params.temperature_inc = 0.2f;
    params.temperature = 0.0f;

    return params;
}

//=============================================================================
// JNI METHODS
//=============================================================================

extern "C" {

/**
 * Load Whisper model
 */
JNIEXPORT jboolean JNICALL
Java_com_ishabdullah_aiish_audio_WhisperSTT_nativeLoadWhisperModel(
        JNIEnv* env,
        jobject /* this */,
        jstring model_path,
        jstring language) {

    std::lock_guard<std::mutex> lock(g_whisper_mutex);

    const char* path = env->GetStringUTFChars(model_path, nullptr);
    const char* lang = env->GetStringUTFChars(language, nullptr);

    LOGI("Loading Whisper model: %s (lang=%s)", path, lang);

    // Free existing context if any
    if (g_whisper_ctx != nullptr) {
        LOGI("Freeing existing Whisper context...");
        whisper_free(g_whisper_ctx);
        g_whisper_ctx = nullptr;
    }

    // Setup context parameters
    whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;  // Set to true when OpenCL is enabled
    cparams.flash_attn = false;
    cparams.gpu_device = 0;
    cparams.dtw_token_timestamps = false;

    // Load model
    g_whisper_ctx = whisper_init_from_file_with_params(path, cparams);

    if (g_whisper_ctx == nullptr) {
        LOGE("Failed to load Whisper model");
        env->ReleaseStringUTFChars(model_path, path);
        env->ReleaseStringUTFChars(language, lang);
        return JNI_FALSE;
    }

    // Store language preference
    g_detected_language = std::string(lang);
    g_whisper_initialized = true;

    env->ReleaseStringUTFChars(model_path, path);
    env->ReleaseStringUTFChars(language, lang);

    LOGI("Whisper model loaded successfully");
    LOGI("Model vocab size: %d", whisper_n_vocab(g_whisper_ctx));
    LOGI("Model languages: %d", whisper_lang_max_id());

    return JNI_TRUE;
}

/**
 * Transcribe audio to text
 */
JNIEXPORT jstring JNICALL
Java_com_ishabdullah_aiish_audio_WhisperSTT_nativeTranscribe(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray audio_data,
        jint audio_length,
        jboolean enable_timestamps) {

    std::lock_guard<std::mutex> lock(g_whisper_mutex);

    if (g_whisper_ctx == nullptr) {
        LOGE("Cannot transcribe: Whisper model not loaded");
        return env->NewStringUTF("");
    }

    // Get audio data from Java
    jfloat* audio = env->GetFloatArrayElements(audio_data, nullptr);
    LOGD("Transcribing audio: %d samples, timestamps=%d", audio_length, enable_timestamps);

    // Setup parameters
    whisper_full_params params = get_default_params();
    params.language = g_detected_language.c_str();
    params.detect_language = false;
    params.print_timestamps = enable_timestamps;

    // Run transcription
    int result = whisper_full(g_whisper_ctx, params, audio, audio_length);

    env->ReleaseFloatArrayElements(audio_data, audio, 0);

    if (result != 0) {
        LOGE("Failed to transcribe audio (error code: %d)", result);
        return env->NewStringUTF("");
    }

    // Extract transcribed text
    const int n_segments = whisper_full_n_segments(g_whisper_ctx);
    LOGD("Transcription complete: %d segments", n_segments);

    std::string transcription;
    for (int i = 0; i < n_segments; i++) {
        const char* text = whisper_full_get_segment_text(g_whisper_ctx, i);

        if (enable_timestamps) {
            int64_t t0 = whisper_full_get_segment_t0(g_whisper_ctx, i);
            int64_t t1 = whisper_full_get_segment_t1(g_whisper_ctx, i);

            // Convert centiseconds to seconds
            float start_time = t0 / 100.0f;
            float end_time = t1 / 100.0f;

            char timestamp[64];
            snprintf(timestamp, sizeof(timestamp), "[%.2f -> %.2f] ", start_time, end_time);
            transcription += timestamp;
        }

        transcription += text;

        // Add space between segments (except last)
        if (i < n_segments - 1) {
            transcription += " ";
        }
    }

    LOGI("Transcription result: %s", transcription.c_str());
    return env->NewStringUTF(transcription.c_str());
}

/**
 * Transcribe audio with streaming (for real-time)
 * Note: Whisper doesn't natively support streaming, so we process chunks
 */
JNIEXPORT jstring JNICALL
Java_com_ishabdullah_aiish_audio_WhisperSTT_nativeTranscribeStreaming(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray audio_data,
        jint audio_length) {

    std::lock_guard<std::mutex> lock(g_whisper_mutex);

    if (g_whisper_ctx == nullptr) {
        LOGE("Cannot transcribe: Whisper model not loaded");
        return env->NewStringUTF("");
    }

    jfloat* audio = env->GetFloatArrayElements(audio_data, nullptr);
    LOGD("Streaming transcription: %d samples", audio_length);

    // For streaming, use simpler parameters for faster processing
    whisper_full_params params = get_default_params();
    params.language = g_detected_language.c_str();
    params.detect_language = false;
    params.single_segment = true;  // Process as single segment for streaming
    params.no_context = true;
    params.duration_ms = 0;  // Process full chunk

    // Run transcription
    int result = whisper_full(g_whisper_ctx, params, audio, audio_length);

    env->ReleaseFloatArrayElements(audio_data, audio, 0);

    if (result != 0) {
        LOGE("Failed to transcribe streaming audio");
        return env->NewStringUTF("");
    }

    // Get result from first segment
    const int n_segments = whisper_full_n_segments(g_whisper_ctx);
    if (n_segments == 0) {
        return env->NewStringUTF("");
    }

    const char* text = whisper_full_get_segment_text(g_whisper_ctx, 0);
    LOGD("Streaming result: %s", text);

    return env->NewStringUTF(text);
}

/**
 * Get detected language
 */
JNIEXPORT jstring JNICALL
Java_com_ishabdullah_aiish_audio_WhisperSTT_nativeGetLanguage(
        JNIEnv* env,
        jobject /* this */) {

    if (g_whisper_ctx == nullptr) {
        return env->NewStringUTF("en");
    }

    // Return the current language setting
    // For auto-detection, you would need to run whisper_full first and then
    // call whisper_full_lang_id to get detected language

    return env->NewStringUTF(g_detected_language.c_str());
}

/**
 * Detect language from audio sample
 * This is useful for auto-language detection
 */
JNIEXPORT jstring JNICALL
Java_com_ishabdullah_aiish_audio_WhisperSTT_nativeDetectLanguage(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray audio_data,
        jint audio_length) {

    std::lock_guard<std::mutex> lock(g_whisper_mutex);

    if (g_whisper_ctx == nullptr) {
        LOGE("Cannot detect language: Whisper model not loaded");
        return env->NewStringUTF("en");
    }

    jfloat* audio = env->GetFloatArrayElements(audio_data, nullptr);
    LOGD("Detecting language from %d samples", audio_length);

    // Use auto-detection mode
    whisper_full_params params = get_default_params();
    params.language = "auto";
    params.detect_language = true;
    params.duration_ms = 3000;  // Use first 3 seconds for detection

    // Run brief transcription for language detection
    int result = whisper_full(g_whisper_ctx, params, audio, audio_length);

    env->ReleaseFloatArrayElements(audio_data, audio, 0);

    if (result != 0) {
        LOGE("Failed to detect language");
        return env->NewStringUTF("en");
    }

    // Get detected language from first segment
    const int n_segments = whisper_full_n_segments(g_whisper_ctx);
    if (n_segments > 0) {
        int lang_id = whisper_full_lang_id(g_whisper_ctx);
        const char* lang = whisper_lang_str(lang_id);

        LOGI("Detected language: %s (id=%d)", lang, lang_id);
        g_detected_language = std::string(lang);

        return env->NewStringUTF(lang);
    }

    return env->NewStringUTF("en");
}

/**
 * Release Whisper model
 */
JNIEXPORT void JNICALL
Java_com_ishabdullah_aiish_audio_WhisperSTT_nativeReleaseWhisperModel(
        JNIEnv* env,
        jobject /* this */) {

    std::lock_guard<std::mutex> lock(g_whisper_mutex);

    LOGI("Releasing Whisper model");

    if (g_whisper_ctx != nullptr) {
        whisper_free(g_whisper_ctx);
        g_whisper_ctx = nullptr;
    }

    g_whisper_initialized = false;
    LOGI("Whisper model released");
}

/**
 * Get model information
 */
JNIEXPORT jstring JNICALL
Java_com_ishabdullah_aiish_audio_WhisperSTT_nativeGetModelInfo(
        JNIEnv* env,
        jobject /* this */) {

    if (g_whisper_ctx == nullptr) {
        return env->NewStringUTF("No model loaded");
    }

    char info[256];
    snprintf(info, sizeof(info),
             "Whisper Model Info:\n"
             "Vocab size: %d\n"
             "Text contexts: %d\n"
             "Audio contexts: %d\n"
             "Supported languages: %d",
             whisper_n_vocab(g_whisper_ctx),
             whisper_n_text_ctx(g_whisper_ctx),
             whisper_n_audio_ctx(g_whisper_ctx),
             whisper_lang_max_id() + 1);

    return env->NewStringUTF(info);
}

/**
 * Check if model is loaded
 */
JNIEXPORT jboolean JNICALL
Java_com_ishabdullah_aiish_audio_WhisperSTT_nativeIsModelLoaded(
        JNIEnv* env,
        jobject /* this */) {

    return (g_whisper_ctx != nullptr) ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
