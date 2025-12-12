# Copyright (c) 2025 Ismail Abdullah. All rights reserved.
#
# This software and all associated files are the exclusive property of Ismail Abdullah.
# Unauthorized use, copying, modification, or distribution is strictly prohibited.
#
# Contact: ismail.t.abdullah@gmail.com

cmake_minimum_required(VERSION 3.22.1)
project(aiish_native C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# Build type
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

# Optimization flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -DNDEBUG -fvisibility=hidden")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -DNDEBUG")

# ARM NEON optimizations for mobile
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a+fp+simd")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=armv8-a+fp+simd")
    add_definitions(-DGGML_USE_NEON)
endif()

# OpenCL support (will be enabled when OpenCL headers are vendored)
# For now, build without OpenCL but with placeholder interface
set(ENABLE_OPENCL OFF)

if(ENABLE_OPENCL)
    add_definitions(-DENABLE_OPENCL)
    add_definitions(-DGGML_USE_OPENCL)
    # OpenCL library path will be added when vendored
    # find_library(opencl-lib OpenCL)
endif()

# Find required libraries
find_library(log-lib log)
find_library(android-lib android)

#=============================================================================
# LLAMA.CPP INTEGRATION
#=============================================================================
#
# Full LLM inference is now ENABLED with llama.cpp.
# CPU-only inference is active (no GPU acceleration yet).
# Speech-to-text uses Vosk (Gradle dependency, no native build required).
#
#=============================================================================

# Disable llama.cpp features we don't need for Android
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "llama: build tests" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "llama: build examples" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "llama: build server" FORCE)
set(LLAMA_STANDALONE OFF CACHE BOOL "llama: standalone build" FORCE)

# Enable optimizations
set(GGML_NATIVE OFF CACHE BOOL "ggml: enable -march=native flag" FORCE)

# Backend configuration - CPU only for Android (Vulkan headers not available in NDK)
set(GGML_CPU ON CACHE BOOL "ggml: use CPU backend" FORCE)
set(GGML_METAL OFF CACHE BOOL "ggml: use Metal" FORCE)
set(GGML_CUDA OFF CACHE BOOL "ggml: use CUDA" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "ggml: use Vulkan" FORCE)
set(GGML_KOMPUTE OFF CACHE BOOL "ggml: use Kompute" FORCE)
set(GGML_SYCL OFF CACHE BOOL "ggml: use SYCL" FORCE)
set(GGML_RPC OFF CACHE BOOL "ggml: use RPC" FORCE)

# Android optimizations
if(ANDROID)
    set(GGML_BLAS OFF CACHE BOOL "ggml: use BLAS" FORCE)
endif()

# Add llama.cpp subdirectory
add_subdirectory(llama.cpp llama-build)

#=============================================================================
# AIISH NATIVE LIBRARY (JNI Bridge)
#=============================================================================

# Source files (llama.cpp for LLM, Vosk for STT via Kotlin/JNI)
set(SOURCES
    llm_bridge.cpp
    gpu_backend.cpp
    npu_delegate.cpp
)

# Create shared library
add_library(aiish_native SHARED ${SOURCES})

# Include directories
target_include_directories(aiish_native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/include
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/src
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/ggml/include
)

# Link libraries (Vosk is linked via Gradle, not here)
target_link_libraries(aiish_native
    llama
    ${log-lib}
    ${android-lib}
)

# Link OpenCL when enabled
if(ENABLE_OPENCL)
    # target_link_libraries(aiish_native ${opencl-lib})
endif()
