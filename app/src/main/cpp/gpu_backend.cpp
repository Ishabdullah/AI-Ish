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
 * GPU BACKEND DETECTION AND MANAGEMENT
 * ================================================================================================
 *
 * This file provides GPU acceleration detection and initialization via:
 * - OpenCL (for GPU compute on Qualcomm Adreno, ARM Mali, PowerVR)
 * - TensorFlow Lite GPU delegate (TfLiteGpuDelegateOptionsV2)
 *
 * CURRENT STATUS:
 * - CPU detection: FULLY IMPLEMENTED
 * - ARM NEON detection: FULLY IMPLEMENTED
 * - OpenCL: INTERFACE READY (headers vendored in opencl/)
 *
 * GPU ACCELERATION STRATEGY:
 * 1. LLM (llama.cpp): CPU with ARM NEON (GPU not beneficial for transformers)
 * 2. Vision (TFLite MobileNet): NNAPI delegate or GPU delegate
 * 3. STT (Vosk): CPU only
 * 4. Fallback: Optimized CPU with ARM NEON
 *
 * OpenCL Support:
 * - Headers vendored in opencl/ directory
 * - Set ENABLE_OPENCL=ON in CMakeLists.txt to enable
 * - Runtime links against libOpenCL.so (available on Qualcomm/ARM devices)
 *
 * ================================================================================================
 */

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <fstream>
#include <sstream>
#include <sys/system_properties.h>

#define LOG_TAG "AiIsh_GPU"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// OpenCL support (will be enabled when headers are vendored)
#ifdef ENABLE_OPENCL
#include <CL/cl.h>
static cl_platform_id g_cl_platform = nullptr;
static cl_device_id g_cl_device = nullptr;
static cl_context g_cl_context = nullptr;
static cl_command_queue g_cl_queue = nullptr;
static bool g_opencl_initialized = false;
#endif

//=============================================================================
// HELPER FUNCTIONS
//=============================================================================

/**
 * Read Android system property
 */
static std::string get_system_property(const char* key) {
    char value[PROP_VALUE_MAX] = {0};
    __system_property_get(key, value);
    return std::string(value);
}

/**
 * Read file content
 */
static std::string read_file(const char* path) {
    std::ifstream file(path);
    if (!file.is_open()) {
        return "";
    }

    std::stringstream buffer;
    buffer << file.rdbuf();
    return buffer.str();
}

/**
 * Detect CPU architecture and features
 */
static std::string detect_cpu_info() {
    std::string cpuinfo = read_file("/proc/cpuinfo");

    // Extract processor info
    std::string processor = "Unknown";
    std::string features;

    size_t proc_pos = cpuinfo.find("Hardware");
    if (proc_pos != std::string::npos) {
        size_t end_pos = cpuinfo.find("\n", proc_pos);
        processor = cpuinfo.substr(proc_pos, end_pos - proc_pos);
    }

    size_t feat_pos = cpuinfo.find("Features");
    if (feat_pos != std::string::npos) {
        size_t end_pos = cpuinfo.find("\n", feat_pos);
        features = cpuinfo.substr(feat_pos, end_pos - feat_pos);
    }

    return processor + " | " + features;
}

/**
 * Detect GPU from system properties and proc files
 */
static std::string detect_gpu_info() {
    // Try to get GPU renderer from system properties
    std::string gpu_vendor = get_system_property("ro.hardware.vulkan");
    std::string gpu_renderer = get_system_property("ro.hardware.egl");
    std::string soc = get_system_property("ro.hardware");
    std::string board = get_system_property("ro.product.board");

    // Build GPU info string
    std::string gpu_info;

    if (!soc.empty()) {
        gpu_info += "SoC: " + soc;
    }

    if (!board.empty()) {
        if (!gpu_info.empty()) gpu_info += " | ";
        gpu_info += "Board: " + board;
    }

    // Try to detect Adreno GPU (common on Qualcomm Snapdragon)
    if (soc.find("qcom") != std::string::npos ||
        soc.find("kalama") != std::string::npos ||  // Snapdragon 8 Gen 2
        soc.find("pineapple") != std::string::npos) { // Snapdragon 8 Gen 3
        if (!gpu_info.empty()) gpu_info += " | ";
        gpu_info += "GPU: Qualcomm Adreno";
    }
    // Mali (ARM/Samsung)
    else if (soc.find("exynos") != std::string::npos) {
        if (!gpu_info.empty()) gpu_info += " | ";
        gpu_info += "GPU: ARM Mali";
    }
    // PowerVR (MediaTek)
    else if (soc.find("mt") != std::string::npos) {
        if (!gpu_info.empty()) gpu_info += " | ";
        gpu_info += "GPU: PowerVR/Mali";
    }

    return gpu_info.empty() ? "Unknown GPU" : gpu_info;
}

//=============================================================================
// JNI METHODS
//=============================================================================

extern "C" {

/**
 * Check if GPU is available on device
 */
JNIEXPORT jboolean JNICALL
Java_com_ishabdullah_aiish_ml_GPUManager_nativeIsGPUAvailable(
        JNIEnv* env,
        jobject /* this */) {

    LOGI("Checking GPU availability...");

#ifdef ENABLE_OPENCL
    // Try OpenCL detection
    cl_uint num_platforms = 0;
    cl_int ret = clGetPlatformIDs(0, nullptr, &num_platforms);

    if (ret == CL_SUCCESS && num_platforms > 0) {
        LOGI("OpenCL detected: %d platforms available", num_platforms);
        return JNI_TRUE;
    } else {
        LOGI("OpenCL not available (error: %d)", ret);
        return JNI_FALSE;
    }
#else
    // Without OpenCL, detect based on architecture
    #ifdef __aarch64__
        std::string gpu_info = detect_gpu_info();
        LOGI("ARM64 detected, GPU info: %s", gpu_info.c_str());

        // Assume GPU available on ARM64 (most modern Android devices)
        return JNI_TRUE;
    #else
        LOGI("Not ARM64, GPU may not be available");
        return JNI_FALSE;
    #endif
#endif
}

/**
 * Get GPU vendor string
 */
JNIEXPORT jstring JNICALL
Java_com_ishabdullah_aiish_ml_GPUManager_nativeGetGPUVendor(
        JNIEnv* env,
        jobject /* this */) {

#ifdef ENABLE_OPENCL
    if (g_cl_platform != nullptr) {
        char vendor[128];
        clGetPlatformInfo(g_cl_platform, CL_PLATFORM_VENDOR, sizeof(vendor), vendor, nullptr);
        return env->NewStringUTF(vendor);
    }
#endif

    // Fallback: detect from system properties
    std::string soc = get_system_property("ro.hardware");

    if (soc.find("qcom") != std::string::npos) {
        return env->NewStringUTF("Qualcomm");
    } else if (soc.find("exynos") != std::string::npos) {
        return env->NewStringUTF("Samsung (ARM)");
    } else if (soc.find("mt") != std::string::npos) {
        return env->NewStringUTF("MediaTek");
    }

    return env->NewStringUTF("Unknown Vendor");
}

/**
 * Get GPU renderer string
 */
JNIEXPORT jstring JNICALL
Java_com_ishabdullah_aiish_ml_GPUManager_nativeGetGPURenderer(
        JNIEnv* env,
        jobject /* this */) {

#ifdef ENABLE_OPENCL
    if (g_cl_device != nullptr) {
        char name[128];
        clGetDeviceInfo(g_cl_device, CL_DEVICE_NAME, sizeof(name), name, nullptr);
        return env->NewStringUTF(name);
    }
#endif

    // Fallback: detect from system info
    std::string gpu_info = detect_gpu_info();
    return env->NewStringUTF(gpu_info.c_str());
}

/**
 * Get OpenCL/GPU version
 */
JNIEXPORT jstring JNICALL
Java_com_ishabdullah_aiish_ml_GPUManager_nativeGetGPUVersion(
        JNIEnv* env,
        jobject /* this */) {

#ifdef ENABLE_OPENCL
    if (g_cl_device != nullptr) {
        char version[128];
        clGetDeviceInfo(g_cl_device, CL_DEVICE_VERSION, sizeof(version), version, nullptr);
        return env->NewStringUTF(version);
    }
#endif

    // Without OpenCL, return architecture info
    #ifdef __aarch64__
        return env->NewStringUTF("ARM64-v8a (NEON supported)");
    #else
        return env->NewStringUTF("ARM32 (limited acceleration)");
    #endif
}

/**
 * Get number of compute units
 */
JNIEXPORT jint JNICALL
Java_com_ishabdullah_aiish_ml_GPUManager_nativeGetComputeUnits(
        JNIEnv* env,
        jobject /* this */) {

#ifdef ENABLE_OPENCL
    if (g_cl_device != nullptr) {
        cl_uint compute_units;
        clGetDeviceInfo(g_cl_device, CL_DEVICE_MAX_COMPUTE_UNITS,
                       sizeof(compute_units), &compute_units, nullptr);
        return compute_units;
    }
#endif

    // Fallback: estimate from CPU cores
    std::string cpuinfo = read_file("/proc/cpuinfo");
    int processor_count = 0;

    size_t pos = 0;
    while ((pos = cpuinfo.find("processor", pos)) != std::string::npos) {
        processor_count++;
        pos++;
    }

    // GPU compute units usually match or exceed CPU cores on modern SoCs
    return (processor_count > 0) ? processor_count : 8;
}

/**
 * Check if OpenCL is supported
 */
JNIEXPORT jboolean JNICALL
Java_com_ishabdullah_aiish_ml_GPUManager_nativeSupportsOpenCL(
        JNIEnv* env,
        jobject /* this */) {

    LOGI("Checking OpenCL support...");

#ifdef ENABLE_OPENCL
    cl_platform_id platform;
    cl_device_id device;
    cl_int ret = clGetPlatformIDs(1, &platform, nullptr);
    if (ret != CL_SUCCESS) {
        LOGI("OpenCL not available: no platforms found");
        return JNI_FALSE;
    }

    ret = clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, 1, &device, nullptr);
    if (ret != CL_SUCCESS) {
        LOGI("OpenCL GPU not available");
        return JNI_FALSE;
    }

    char version[128];
    clGetDeviceInfo(device, CL_DEVICE_OPENCL_C_VERSION, sizeof(version), version, nullptr);
    LOGI("OpenCL version: %s", version);
    return JNI_TRUE;
#else
    LOGI("OpenCL support not compiled in (ENABLE_OPENCL=OFF)");
    LOGI("To enable: vendor OpenCL headers and set ENABLE_OPENCL=ON in CMakeLists.txt");
    return JNI_FALSE;
#endif
}

/**
 * Initialize OpenCL context for GPU inference
 * Returns: 0 on success, negative on error
 */
JNIEXPORT jint JNICALL
Java_com_ishabdullah_aiish_ml_GPUManager_nativeInitOpenCL(
        JNIEnv* env,
        jobject /* this */) {

    LOGI("Initializing OpenCL context...");

#ifdef ENABLE_OPENCL
    if (g_opencl_initialized) {
        LOGI("OpenCL already initialized");
        return 0;
    }

    cl_int ret;

    // Get platform
    ret = clGetPlatformIDs(1, &g_cl_platform, nullptr);
    if (ret != CL_SUCCESS) {
        LOGE("Failed to get OpenCL platform: %d", ret);
        return -1;
    }

    // Get GPU device
    ret = clGetDeviceIDs(g_cl_platform, CL_DEVICE_TYPE_GPU, 1, &g_cl_device, nullptr);
    if (ret != CL_SUCCESS) {
        LOGE("Failed to get OpenCL GPU device: %d", ret);
        return -2;
    }

    // Create context
    g_cl_context = clCreateContext(nullptr, 1, &g_cl_device, nullptr, nullptr, &ret);
    if (ret != CL_SUCCESS) {
        LOGE("Failed to create OpenCL context: %d", ret);
        return -3;
    }

    // Create command queue
    g_cl_queue = clCreateCommandQueue(g_cl_context, g_cl_device, 0, &ret);
    if (ret != CL_SUCCESS) {
        LOGE("Failed to create OpenCL command queue: %d", ret);
        clReleaseContext(g_cl_context);
        return -4;
    }

    g_opencl_initialized = true;
    LOGI("OpenCL context initialized successfully");

    // Log device info
    char device_name[128];
    clGetDeviceInfo(g_cl_device, CL_DEVICE_NAME, sizeof(device_name), device_name, nullptr);
    LOGI("OpenCL device: %s", device_name);

    return 0;
#else
    LOGE("OpenCL not enabled at compile time");
    LOGE("To enable: Set ENABLE_OPENCL=ON in CMakeLists.txt and vendor OpenCL headers");
    return -999;
#endif
}

/**
 * Cleanup OpenCL resources
 */
JNIEXPORT void JNICALL
Java_com_ishabdullah_aiish_ml_GPUManager_nativeCleanupOpenCL(
        JNIEnv* env,
        jobject /* this */) {

    LOGI("Cleaning up OpenCL resources...");

#ifdef ENABLE_OPENCL
    if (g_cl_queue) {
        clReleaseCommandQueue(g_cl_queue);
        g_cl_queue = nullptr;
    }

    if (g_cl_context) {
        clReleaseContext(g_cl_context);
        g_cl_context = nullptr;
    }

    g_cl_platform = nullptr;
    g_cl_device = nullptr;
    g_opencl_initialized = false;

    LOGI("OpenCL cleanup complete");
#else
    LOGD("OpenCL not enabled, nothing to cleanup");
#endif
}

/**
 * Get detailed hardware information
 */
JNIEXPORT jstring JNICALL
Java_com_ishabdullah_aiish_ml_GPUManager_nativeGetHardwareInfo(
        JNIEnv* env,
        jobject /* this */) {

    std::string info;

    // Device info
    std::string manufacturer = get_system_property("ro.product.manufacturer");
    std::string model = get_system_property("ro.product.model");
    std::string soc = get_system_property("ro.hardware");

    info += "Device: " + manufacturer + " " + model + "\n";
    info += "SoC: " + soc + "\n";

    // CPU info
    std::string cpu_info = detect_cpu_info();
    info += "CPU: " + cpu_info + "\n";

    // GPU info
    std::string gpu_info = detect_gpu_info();
    info += "GPU: " + gpu_info + "\n";

    // Architecture
    #ifdef __aarch64__
        info += "Architecture: ARM64-v8a\n";
        info += "NEON: Supported\n";
    #else
        info += "Architecture: ARM32\n";
        info += "NEON: Limited\n";
    #endif

    // OpenCL status
    #ifdef ENABLE_OPENCL
        info += "OpenCL: Compiled IN\n";
    #else
        info += "OpenCL: Not compiled (requires headers)\n";
    #endif

    return env->NewStringUTF(info.c_str());
}

} // extern "C"
