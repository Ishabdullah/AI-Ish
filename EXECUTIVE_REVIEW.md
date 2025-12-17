# Executive Code Audit & Roadmap

**Date:** December 17, 2025
**Project:** AI-Ish

## 📊 Executive Summary

The **AI-Ish** project represents a solid foundation for a local AI assistant on Android. The core text generation pipeline (Kotlin -> JNI -> llama.cpp) is functional and optimized for CPU execution. The pivot to **Vosk** for speech recognition provides a stable, working solution.

However, significant gaps exist between the defined architecture and the actual native implementation, particularly regarding **multimodal (vision) capabilities** and **hardware acceleration (GPU/NPU)**. The current codebase contains a notable amount of "dead" or stubbed code that mimics functionality without implementing it.

## 🚨 Critical Technical Gaps

### 1. Missing Multimodal/Vision Bridge (`llm_bridge.cpp`)
*   **Status:** 🔴 **Critical Gap**
*   **Issue:** The Kotlin `VisionInferenceEngine` expects to call native methods like `nativeEncodeImage` and `nativeGenerateFromImage`. In the C++ layer (`llm_bridge.cpp`), these functions are likely empty stubs or return mock data.
*   **Impact:** The "Legacy Mode" (Vision-Language Model support) will fail silently or crash if invoked. The app cannot actually "see" images using an LLM.
*   **Remediation:**
    *   Implement `llm_image_embed_make_with_bytes` logic in `llm_bridge.cpp` using the `llama.cpp` clip API.
    *   Ensure the `clip_ctx` is properly initialized alongside the `llama_context`.

### 2. Phantom Hardware Acceleration (`gpu_backend.cpp`, `npu_delegate.cpp`)
*   **Status:** 🟠 **Non-Functional**
*   **Issue:**
    *   `gpu_backend.cpp`: Contains logic to *detect* OpenCL/GPU, but actual GPU offloading is disabled in `CMakeLists.txt` (`AIISH_ENABLE_OPENCL OFF`).
    *   `npu_delegate.cpp`: This file appears to be entirely placeholder code. It logs "NPU detection" but performs no actual delegation to Android NNAPI or vendor-specific NPU SDKs (QNN, SNPE).
*   **Impact:** The app runs purely on CPU. While efficient (NEON), it leaves potential performance gains on the table and misleads the codebase reader about NPU capabilities.
*   **Remediation:**
    *   **Short Term:** Explicitly mark these components as experimental/disabled in code.
    *   **Long Term:** Re-enable OpenCL in CMake and debug the `llama.cpp` CL backend, or fully commit to NNAPI via the TFLite delegate (which `VisionManager` seems to try to use).

### 3. Audio Stack Fragmentation (Whisper vs. Vosk)
*   **Status:** 🟡 **Technical Debt**
*   **Issue:** The project file structure includes references to `whisper.cpp` (likely as a submodule or source folder), but the active code uses **Vosk** (`VoskSTT.kt`).
*   **Impact:** Bloats the repository size and build time (if compiled). Confuses developers about which STT engine is authoritative.
*   **Remediation:**
    *   Remove `whisper.cpp` from `CMakeLists.txt` and the filesystem.
    *   Standardize on Vosk for the v1.0 release.

## 🛣️ Remediation Plan (Step-by-Step)

### Phase 1: Cleanup & Stabilization (Immediate)
1.  **Purge Dead Code:** Remove `npu_delegate.cpp` references if no concrete plan exists to implement vendor-specific NPU code.
2.  **Formalize Vosk:** Delete `whisper.cpp` directories and references.
3.  **Documentation:** Update KDoc in `LLMInferenceEngine` to explicitly state "CPU-Only" to manage expectations.

### Phase 2: Enable Multimodal (High Priority)
1.  **Update CMake:** Ensure `llama.cpp` is built with `LLAMA_BUILD_SERVER=OFF` but `LLAMA_BUILD_EXAMPLES=ON` (or link specifically against `libllava`/`libclip`).
2.  **Implement JNI:** Write the actual C++ code in `llm_bridge.cpp` to:
    *   Load a CLIP projector/MMP model.
    *   Process image bytes into image embeddings.
    *   Feed embeddings into the `llama` context.

### Phase 3: Hardware Acceleration (Future)
1.  **GPU:** Flip `AIISH_ENABLE_OPENCL` to `ON` and test on a Snapdragon device. Monitor thermal throttling.
2.  **NPU:** Abandon custom `npu_delegate.cpp`. Instead, leverage the TFLite NNAPI delegate properly for the MobileNet vision models (which seems partially implemented in `VisionManager`).

## ✅ Conclusion
The project is ~70% complete. The "Brain" (LLM) and "Ears" (Vosk) are working. The "Eyes" (Vision) are the primary missing piece. Focusing on Phase 2 (Multimodal JNI implementation) will yield the highest value feature completion.