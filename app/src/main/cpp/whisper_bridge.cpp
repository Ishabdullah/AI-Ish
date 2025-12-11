/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "WhisperBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

//=============================================================================
// WHISPER MODEL STATE
//=============================================================================

// TODO: Integrate whisper.cpp
// Global state for Whisper model
// static whisper_context* g_whisper_ctx = nullptr;
// static whisper_params g_whisper_params;
static std::string g_detected_language = "en";

//=============================================================================
// WHISPER JNI METHODS
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

    const char* path = env->GetStringUTFChars(model_path, nullptr);
    const char* lang = env->GetStringUTFChars(language, nullptr);

    LOGI("Loading Whisper model: %s (lang=%s)", path, lang);

    // TODO: Integrate actual whisper.cpp loading
    // Whisper model loading steps:
    // 1. whisper_init_from_file(path)
    // 2. Configure parameters (language, beam size, etc.)
    // 3. Store global context
    //
    // Example:
    // g_whisper_ctx = whisper_init_from_file(path);
    // if (g_whisper_ctx == nullptr) {
    //     LOGE("Failed to load Whisper model");
    //     return JNI_FALSE;
    // }
    //
    // g_whisper_params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    // g_whisper_params.language = lang;
    // g_whisper_params.print_realtime = false;
    // g_whisper_params.print_progress = false;
    // g_whisper_params.print_timestamps = false;

    g_detected_language = std::string(lang);

    env->ReleaseStringUTFChars(model_path, path);
    env->ReleaseStringUTFChars(language, lang);

    LOGI("Whisper model loaded successfully");
    return JNI_TRUE;  // Placeholder success
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

    jfloat* audio = env->GetFloatArrayElements(audio_data, nullptr);
    LOGI("Transcribing audio: %d samples, timestamps=%d", audio_length, enable_timestamps);

    // TODO: Integrate actual whisper.cpp transcription
    // Transcription steps:
    // 1. Get audio data from Java float array
    // 2. Run whisper_full() on audio
    // 3. Extract transcribed text
    // 4. Optionally extract timestamps
    //
    // Example:
    // if (whisper_full(g_whisper_ctx, g_whisper_params, audio, audio_length) != 0) {
    //     LOGE("Failed to transcribe audio");
    //     env->ReleaseFloatArrayElements(audio_data, audio, 0);
    //     return env->NewStringUTF("");
    // }
    //
    // const int n_segments = whisper_full_n_segments(g_whisper_ctx);
    // std::string result;
    // for (int i = 0; i < n_segments; i++) {
    //     const char* text = whisper_full_get_segment_text(g_whisper_ctx, i);
    //     result += text;
    // }

    env->ReleaseFloatArrayElements(audio_data, audio, 0);

    // Placeholder response
    std::string placeholder = "Whisper transcription will appear here once whisper.cpp is integrated. "
                              "Audio length: " + std::to_string(audio_length) + " samples.";
    return env->NewStringUTF(placeholder.c_str());
}

/**
 * Transcribe audio with streaming (for real-time)
 */
JNIEXPORT jstring JNICALL
Java_com_ishabdullah_aiish_audio_WhisperSTT_nativeTranscribeStreaming(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray audio_data,
        jint audio_length) {

    jfloat* audio = env->GetFloatArrayElements(audio_data, nullptr);
    LOGI("Streaming transcription: %d samples", audio_length);

    // TODO: Implement streaming transcription
    // For streaming, we need to:
    // 1. Process audio in chunks
    // 2. Return partial results
    // 3. Maintain context between chunks
    //
    // Whisper doesn't natively support streaming, but we can:
    // - Process overlapping windows
    // - Return partial transcriptions
    // - Use a sliding buffer approach

    env->ReleaseFloatArrayElements(audio_data, audio, 0);

    // Placeholder: Return empty for now
    return env->NewStringUTF("");
}

/**
 * Get detected language
 */
JNIEXPORT jstring JNICALL
Java_com_ishabdullah_aiish_audio_WhisperSTT_nativeGetLanguage(
        JNIEnv* env,
        jobject /* this */) {

    // TODO: Get actual detected language from Whisper
    // const char* lang = whisper_lang_str(whisper_full_lang_id(g_whisper_ctx));
    // return env->NewStringUTF(lang);

    return env->NewStringUTF(g_detected_language.c_str());
}

/**
 * Release Whisper model
 */
JNIEXPORT void JNICALL
Java_com_ishabdullah_aiish_audio_WhisperSTT_nativeReleaseWhisperModel(
        JNIEnv* env,
        jobject /* this */) {

    LOGI("Releasing Whisper model");

    // TODO: Integrate actual cleanup
    // if (g_whisper_ctx != nullptr) {
    //     whisper_free(g_whisper_ctx);
    //     g_whisper_ctx = nullptr;
    // }

    LOGI("Whisper model released");
}

} // extern "C"
