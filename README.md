# AI-Ish: Local AI on Android

**AI-Ish** is a privacy-focused, offline-first Android application that brings powerful AI capabilities directly to your device. It leverages optimized native C++ libraries to run Large Language Models (LLMs), Speech-to-Text (STT), and Computer Vision models locally, without requiring an internet connection or cloud subscriptions.

## 🚀 Key Features

*   **Local LLM Inference:** Run GGUF-compatible models (like Mistral-7B, Llama-3) directly on your phone using an optimized `llama.cpp` backend.
    *   *Performance:* Optimized for ARMv8 processors with NEON SIMD acceleration.
    *   *Privacy:* No data ever leaves your device.
*   **Offline Speech Recognition:** High-accuracy, real-time speech-to-text using **Vosk**.
    *   Supports continuous listening and command recognition.
    *   Multiple language models supported.
*   **Computer Vision (Beta):**
    *   **Object Classification:** Real-time object detection using MobileNet-v3 (via TFLite/NNAPI).
    *   **Multimodal Chat (Experimental):** "Legacy" mode designed for Vision-Language Models (like Moondream/Qwen-VL) to chat about images (currently in development).
*   **Knowledge Integration:** Built-in "Knowledge Scout" to fetch real-time info (Crypto, Weather, Wikipedia) to augment the AI's responses (RAG-lite).
*   **Modern UI:** Built with Jetpack Compose for a smooth, native experience.
    *   Chat interface with history.
    *   Model management dashboard.
    *   Performance metrics monitoring.

## 🛠️ Technology Stack

*   **Language:** Kotlin (100%), C++ (JNI layer)
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Architecture:** MVVM, Clean Architecture
*   **Native Backends:**
    *   `llama.cpp`: For text generation.
    *   `Vosk`: For speech recognition.
    *   `TensorFlow Lite`: For efficient object classification.
*   **Database:** Room (SQLite)

## 📋 Prerequisites

*   **Android Studio:** Ladybug or newer (recommended).
*   **NDK:** Version 26.1.10909125 (or compatible).
*   **Device:** Android 10+ (API 29+), 8GB+ RAM recommended for 7B models.

## 🏗️ Build Instructions

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/yourusername/AI-Ish.git
    cd AI-Ish
    ```

2.  **Initialize Submodules:**
    This project depends on `llama.cpp` and other native libraries.
    ```bash
    git submodule update --init --recursive
    ```

3.  **Build with Gradle:**
    You can build the APK directly using the wrapper:
    ```bash
    ./gradlew assembleDebug
    ```

4.  **Install on Device:**
    ```bash
    adb install app/build/outputs/apk/debug/app-debug.apk
    ```

## 🧩 Project Structure

*   `app/src/main/java`: Kotlin source code (UI, ViewModels, Logic).
*   `app/src/main/cpp`: Native C++ code.
    *   `llm_bridge.cpp`: JNI bindings for `llama.cpp`.
    *   `llama.cpp/`: Submodule for the inference engine.
*   `app/src/main/assets`: Pre-packaged models (if any) and configuration.

## ⚠️ Known Limitations & Roadmap

*   **Vision Support:** The multimodal bridge (`nativeEncodeImage`) for talking to images with LLMs is currently a work-in-progress.
*   **GPU Acceleration:** OpenCL/Vulkan support is currently disabled in favor of CPU (NEON) stability.
*   **Whisper STT:** The codebase contains references to `whisper.cpp`, but the active implementation uses **Vosk** for better performance on mobile.

## 📄 License

Copyright (c) 2025 Ismail Abdullah. All rights reserved.
Unauthorized use, copying, modification, or distribution is strictly prohibited.