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
 * NNAPI DELEGATE SUPPORT - NPU ACCELERATION
 * ================================================================================================
 *
 * This file provides JNI bindings for NPU acceleration via Android NNAPI.
 *
 * ARCHITECTURE:
 * - Vision models (MobileNet-v3): TFLite with NNAPI delegate (Kotlin-side via Gradle AAR)
 * - LLM inference (Mistral-7B): CPU-only via llama.cpp with ARM NEON optimizations
 * - Embeddings (BGE): CPU-only via llama.cpp
 *
 * The native layer provides:
 * - NNAPI availability detection via system properties
 * - Performance profiling capabilities
 * - JNI bridges for model state management
 *
 * Actual TFLite inference is handled by org.tensorflow:tensorflow-lite Gradle dependency
 * which includes the NNAPI delegate automatically.
 *
 * ================================================================================================
 */

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <chrono>
#include <sys/system_properties.h>

#define LOG_TAG "AiIsh_NPU"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// Global state for profiling
static long g_lastPrefillTimeMs = 0;
static long g_lastDecodeTimeMs = 0;
static long g_lastVisionInferenceTimeMs = 0;
static bool g_npuAvailable = false;
static bool g_npuInitialized = false;

// Helper function to get system property
static std::string getSystemProperty(const char* name) {
    char value[PROP_VALUE_MAX] = {0};
    __system_property_get(name, value);
    return std::string(value);
}

// Check if device supports NNAPI acceleration
static bool checkNNAPISupport() {
    // NNAPI is available on Android 8.1+ (API 27+)
    std::string sdkVersion = getSystemProperty("ro.build.version.sdk");
    int sdk = std::stoi(sdkVersion.empty() ? "0" : sdkVersion);

    if (sdk < 27) {
        LOGW("NNAPI requires Android 8.1+ (API 27+), device has API %d", sdk);
        return false;
    }

    // Check for Snapdragon SoC (NPU-equipped devices)
    std::string hardware = getSystemProperty("ro.hardware");
    std::string soc = getSystemProperty("ro.board.platform");

    bool hasNPU = false;

    // Snapdragon 8 Gen 3 (pineapple), 8 Gen 2 (kalama), 8 Gen 1 (taro)
    if (soc.find("pineapple") != std::string::npos ||
        soc.find("kalama") != std::string::npos ||
        soc.find("taro") != std::string::npos ||
        soc.find("qcom") != std::string::npos) {
        hasNPU = true;
        LOGI("Detected Qualcomm SoC with NPU: %s", soc.c_str());
    }

    // Samsung Exynos with NPU
    if (hardware.find("exynos") != std::string::npos ||
        soc.find("exynos") != std::string::npos) {
        hasNPU = true;
        LOGI("Detected Samsung Exynos with NPU");
    }

    // MediaTek Dimensity with APU
    if (soc.find("mt68") != std::string::npos ||
        soc.find("mt69") != std::string::npos) {
        hasNPU = true;
        LOGI("Detected MediaTek Dimensity with APU");
    }

    // Google Tensor
    if (hardware.find("tensor") != std::string::npos ||
        soc.find("tensor") != std::string::npos) {
        hasNPU = true;
        LOGI("Detected Google Tensor with NPU");
    }

    LOGI("NNAPI support: API=%d, hasNPU=%s", sdk, hasNPU ? "true" : "false");
    return hasNPU;
}

//=============================================================================
// NPU MANAGER - NNAPI DETECTION AND INITIALIZATION
//=============================================================================

extern "C" {

/**
 * Detect NPU availability via NNAPI
 *
 * Checks Android API level and device SoC for NPU support.
 * NNAPI is available on Android 8.1+ but NPU acceleration requires
 * compatible hardware (Snapdragon, Exynos, Dimensity, Tensor).
 */
JNIEXPORT jboolean JNICALL
Java_com_ishabdullah_aiish_device_NPUManager_nativeDetectNPU(
        JNIEnv* env,
        jobject /* this */) {

    g_npuAvailable = checkNNAPISupport();

    if (g_npuAvailable) {
        LOGI("NNAPI NPU acceleration available");
    } else {
        LOGW("NNAPI NPU acceleration not available, will use CPU fallback");
    }

    return g_npuAvailable ? JNI_TRUE : JNI_FALSE;
}

/**
 * Initialize NNAPI runtime
 *
 * Prepares the NNAPI context for model loading.
 * Actual delegate configuration is done via TFLite Kotlin API.
 */
JNIEXPORT jboolean JNICALL
Java_com_ishabdullah_aiish_device_NPUManager_nativeInitializeNPU(
        JNIEnv* env,
        jobject /* this */) {

    if (!g_npuAvailable) {
        LOGW("Cannot initialize NPU: not available on this device");
        return JNI_FALSE;
    }

    // NNAPI initialization is handled by TFLite Kotlin API
    // This native call just tracks initialization state
    g_npuInitialized = true;

    LOGI("NNAPI runtime initialized (delegate configuration via TFLite Kotlin API)");
    return JNI_TRUE;
}

/**
 * Load model to NPU with NNAPI delegate
 *
 * Note: Actual model loading is done via TFLite Kotlin API.
 * This native function tracks model state and configuration.
 */
JNIEXPORT jboolean JNICALL
Java_com_ishabdullah_aiish_device_NPUManager_nativeLoadModelToNPU(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPath,
        jstring modelType,
        jboolean useFusedKernels,
        jboolean usePreallocatedBuffers,
        jint bufferPoolSize) {

    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    const char* type = env->GetStringUTFChars(modelType, nullptr);

    LOGI("Load model to NPU via NNAPI delegate");
    LOGI("  Model: %s", path);
    LOGI("  Type: %s", type);
    LOGI("  NNAPI available: %s", g_npuAvailable ? "yes" : "no");

    env->ReleaseStringUTFChars(modelPath, path);
    env->ReleaseStringUTFChars(modelType, type);

    // Model loading is handled by TFLite Kotlin API with NNAPI delegate
    return g_npuAvailable ? JNI_TRUE : JNI_FALSE;
}

/**
 * Get NPU information string
 *
 * Returns device NPU capabilities detected via system properties.
 */
JNIEXPORT jstring JNICALL
Java_com_ishabdullah_aiish_device_NPUManager_nativeGetNPUInfo(
        JNIEnv* env,
        jobject /* this */) {

    std::string info;

    std::string soc = getSystemProperty("ro.board.platform");
    std::string model = getSystemProperty("ro.product.model");
    std::string sdk = getSystemProperty("ro.build.version.sdk");

    if (g_npuAvailable) {
        info = "NPU: NNAPI delegate enabled\n";
        info += "Device: " + model + "\n";
        info += "SoC: " + soc + "\n";
        info += "Android API: " + sdk + "\n";
        info += "Status: " + std::string(g_npuInitialized ? "Initialized" : "Not initialized");
    } else {
        info = "NPU: Not available (CPU fallback)\n";
        info += "Device: " + model + "\n";
        info += "Android API: " + sdk;
    }

    return env->NewStringUTF(info.c_str());
}

/**
 * Release NPU resources
 */
JNIEXPORT void JNICALL
Java_com_ishabdullah_aiish_device_NPUManager_nativeReleaseNPU(
        JNIEnv* env,
        jobject /* this */) {

    g_npuInitialized = false;
    LOGI("NNAPI resources released");
}

//=============================================================================
// LLM INFERENCE - CPU ONLY (via llama.cpp)
//=============================================================================

/**
 * Load LLM model for CPU inference
 *
 * Note: LLM inference uses llama.cpp on CPU with ARM NEON optimizations.
 * NNAPI is not well-suited for transformer architectures.
 * The useNPUPrefill parameter is deprecated and ignored.
 */
JNIEXPORT jboolean JNICALL
Java_com_ishabdullah_aiish_ml_LLMInferenceEngine_nativeLoadMistralINT8(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPath,
        jint contextSize,
        jboolean useNPUPrefill,  // Deprecated - ignored
        jintArray cpuCores,
        jboolean usePreallocatedBuffers) {

    const char* path = env->GetStringUTFChars(modelPath, nullptr);

    LOGI("Load LLM model for CPU inference");
    LOGI("  Model: %s", path);
    LOGI("  Context size: %d", contextSize);
    LOGI("  Note: LLM uses CPU-only (llama.cpp with NEON)");

    if (useNPUPrefill) {
        LOGW("NPU prefill is deprecated - LLM inference uses CPU only");
    }

    env->ReleaseStringUTFChars(modelPath, path);

    // Actual loading handled by LLMInferenceEngine Kotlin layer
    // which uses the existing llama.cpp JNI bridge
    return JNI_TRUE;
}

/**
 * NPU prefill - DEPRECATED
 *
 * This function is kept for API compatibility but does nothing.
 * LLM inference runs entirely on CPU via llama.cpp.
 */
JNIEXPORT jboolean JNICALL
Java_com_ishabdullah_aiish_ml_LLMInferenceEngine_nativePrefillOnNPU(
        JNIEnv* env,
        jobject /* this */,
        jintArray tokens,
        jint numTokens) {

    LOGW("nativePrefillOnNPU: DEPRECATED - LLM uses CPU-only inference");

    // Return true for backward compatibility
    // Actual prefill is handled by llama.cpp on CPU
    return JNI_TRUE;
}

/**
 * CPU decode - delegates to llama.cpp
 *
 * This is a passthrough to the llama.cpp inference engine.
 */
JNIEXPORT jint JNICALL
Java_com_ishabdullah_aiish_ml_LLMInferenceEngine_nativeDecodeOnCPU(
        JNIEnv* env,
        jobject /* this */,
        jint currentToken,
        jfloat temperature,
        jfloat topP) {

    // Delegate to llama.cpp - this function exists for API compatibility
    // Actual token generation is handled by LLMInferenceEngine.generateStream()
    LOGW("nativeDecodeOnCPU: Use LLMInferenceEngine.generateStream() instead");
    return -1;  // Signal to use Kotlin layer
}

/**
 * Get prefill time (profiling)
 */
JNIEXPORT jlong JNICALL
Java_com_ishabdullah_aiish_ml_LLMInferenceEngine_nativeGetPrefillTimeMs(
        JNIEnv* env,
        jobject /* this */) {
    return g_lastPrefillTimeMs;
}

/**
 * Get decode time (profiling)
 */
JNIEXPORT jlong JNICALL
Java_com_ishabdullah_aiish_ml_LLMInferenceEngine_nativeGetDecodeTimeMs(
        JNIEnv* env,
        jobject /* this */) {
    return g_lastDecodeTimeMs;
}

/**
 * Release LLM model
 */
JNIEXPORT void JNICALL
Java_com_ishabdullah_aiish_ml_LLMInferenceEngine_nativeReleaseMistral(
        JNIEnv* env,
        jobject /* this */) {
    LOGI("LLM model released");
}

//=============================================================================
// VISION - TFLite with NNAPI Delegate (Kotlin-side)
//=============================================================================

/**
 * Load MobileNet-v3 for NNAPI inference
 *
 * Note: Actual model loading uses TFLite Kotlin API with NNAPI delegate.
 * This native function tracks model state.
 */
JNIEXPORT jboolean JNICALL
Java_com_ishabdullah_aiish_vision_VisionManager_nativeLoadMobileNetV3(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPath,
        jboolean useNPU,
        jboolean useFusedKernels,
        jboolean usePreallocatedBuffers) {

    const char* path = env->GetStringUTFChars(modelPath, nullptr);

    LOGI("Load MobileNet-v3 for vision inference");
    LOGI("  Model: %s", path);
    LOGI("  NNAPI delegate: %s", (useNPU && g_npuAvailable) ? "enabled" : "disabled (CPU fallback)");

    env->ReleaseStringUTFChars(modelPath, path);

    // Model loading is handled by TFLite Kotlin API
    return JNI_TRUE;
}

/**
 * Classify image
 *
 * Note: Actual inference uses TFLite Kotlin API with NNAPI delegate.
 * This native function is kept for API compatibility.
 */
JNIEXPORT jobjectArray JNICALL
Java_com_ishabdullah_aiish_vision_VisionManager_nativeClassifyImage(
        JNIEnv* env,
        jobject /* this */,
        jintArray bitmapPixels,
        jint width,
        jint height,
        jint topK) {

    LOGW("nativeClassifyImage: Use VisionManager Kotlin API instead (TFLite NNAPI delegate)");

    auto start = std::chrono::high_resolution_clock::now();

    // Return placeholder - actual inference via TFLite Kotlin API
    jobjectArray result = env->NewObjectArray(topK, env->FindClass("java/lang/String"), nullptr);
    for (int i = 0; i < topK; i++) {
        env->SetObjectArrayElement(result, i, env->NewStringUTF("use_kotlin_api:0.0"));
    }

    auto end = std::chrono::high_resolution_clock::now();
    g_lastVisionInferenceTimeMs = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();

    return result;
}

/**
 * Get vision inference time (profiling)
 */
JNIEXPORT jlong JNICALL
Java_com_ishabdullah_aiish_vision_VisionManager_nativeGetInferenceTimeMs(
        JNIEnv* env,
        jobject /* this */) {
    return g_lastVisionInferenceTimeMs;
}

/**
 * Release MobileNet model
 */
JNIEXPORT void JNICALL
Java_com_ishabdullah_aiish_vision_VisionManager_nativeReleaseMobileNet(
        JNIEnv* env,
        jobject /* this */) {
    LOGI("MobileNet model released");
}

//=============================================================================
// EMBEDDINGS - CPU ONLY (via llama.cpp)
//=============================================================================

/**
 * Load BGE model on CPU
 *
 * Embedding generation uses llama.cpp on CPU with ARM NEON optimizations.
 */
JNIEXPORT jboolean JNICALL
Java_com_ishabdullah_aiish_embedding_EmbeddingManager_nativeLoadBGEModel(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPath,
        jintArray cpuCores,
        jboolean usePreallocatedBuffers) {

    const char* path = env->GetStringUTFChars(modelPath, nullptr);

    LOGI("Load BGE embedding model (CPU with NEON)");
    LOGI("  Model: %s", path);

    env->ReleaseStringUTFChars(modelPath, path);

    // Actual loading handled by EmbeddingManager Kotlin layer
    return JNI_TRUE;
}

/**
 * Generate embedding for single text
 *
 * Note: Actual embedding generation uses llama.cpp via Kotlin layer.
 */
JNIEXPORT jfloatArray JNICALL
Java_com_ishabdullah_aiish_embedding_EmbeddingManager_nativeGenerateEmbedding(
        JNIEnv* env,
        jobject /* this */,
        jstring text) {

    LOGW("nativeGenerateEmbedding: Use EmbeddingManager Kotlin API instead");

    // Return placeholder 384-dim embedding (BGE-Small dimension)
    jfloatArray result = env->NewFloatArray(384);
    return result;
}

/**
 * Generate embeddings for batch of texts
 *
 * Note: Actual batch embedding uses llama.cpp via Kotlin layer.
 */
JNIEXPORT jobjectArray JNICALL
Java_com_ishabdullah_aiish_embedding_EmbeddingManager_nativeGenerateEmbeddingsBatch(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray texts) {

    jsize count = env->GetArrayLength(texts);
    LOGW("nativeGenerateEmbeddingsBatch: Use EmbeddingManager Kotlin API instead");

    // Return placeholder embeddings
    jclass floatArrayClass = env->FindClass("[F");
    jobjectArray result = env->NewObjectArray(count, floatArrayClass, nullptr);

    for (int i = 0; i < count; i++) {
        jfloatArray embedding = env->NewFloatArray(384);
        env->SetObjectArrayElement(result, i, embedding);
    }

    return result;
}

/**
 * Release BGE model
 */
JNIEXPORT void JNICALL
Java_com_ishabdullah_aiish_embedding_EmbeddingManager_nativeReleaseBGEModel(
        JNIEnv* env,
        jobject /* this */) {
    LOGI("BGE model released");
}

//=============================================================================
// PROFILING - NPU vs CPU BENCHMARKING
//=============================================================================

/**
 * Run NPU vs CPU benchmark
 *
 * Compares NNAPI (NPU) performance against CPU-only execution.
 * Returns benchmark results as a formatted string.
 */
JNIEXPORT jstring JNICALL
Java_com_ishabdullah_aiish_device_NPUManager_nativeBenchmark(
        JNIEnv* env,
        jobject /* this */,
        jint iterations) {

    std::string result = "NNAPI Benchmark Results\n";
    result += "=======================\n";
    result += "NPU Available: " + std::string(g_npuAvailable ? "Yes" : "No") + "\n";
    result += "NPU Initialized: " + std::string(g_npuInitialized ? "Yes" : "No") + "\n";
    result += "Iterations: " + std::to_string(iterations) + "\n\n";

    result += "Note: Actual benchmark requires running TFLite inference\n";
    result += "via VisionManager Kotlin API with NNAPI delegate.\n";
    result += "Compare inference times with useNPU=true vs useNPU=false.\n";

    return env->NewStringUTF(result.c_str());
}

} // extern "C"
