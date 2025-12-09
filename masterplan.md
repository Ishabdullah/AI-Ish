# AI Ish – Master Plan to Become the Ultimate Private Pocket AI

## 1. Current State Summary

AI Ish is a privacy-focused Android AI companion built with Jetpack Compose that downloads and manages on-device LLM models (Phi-4, Qwen2-7B, Moondream2). It features a model download manager with SHA-256 verification, real-time knowledge fetching (Wikipedia, CoinGecko, OpenMeteo), deterministic math reasoning, wake word detection ("Hey Ish"), basic vision integration, semantic memory, and a privacy guard system. The architecture is clean and modular with MVVM pattern, but the core LLM inference layer is incomplete—models download successfully but don't actually run inference yet. The app has the skeleton of a powerful AI assistant but needs the connective tissue from its four source repos to become truly formidable.

## 2. The Gold Standard – Best Features from Each Source Repo

- **Codey** → Permission-first architecture with live previews for every file operation, git command, and shell execution—plus complex instruction auto-breakdown that parses numbered lists into executable TODO items with real-time status tracking (✅ ❌ ⏳ ⊘).

- **Adaptheon** → Hierarchical reasoning machine with 24+ specialized knowledge fetchers (academic, finance, sports, government, media) combined with temporal awareness that detects time-sensitive queries and refuses to use stale LLM knowledge, always routing to live sources for identity questions and current events.

- **AILive** → PersonalityEngine unified intelligence architecture that treats AI as ONE coherent entity with extensible tools (8 integrated) instead of fragmented agents, combined with GPU acceleration via OpenCL (3-5x speedup) and semantic memory with LLM-powered fact extraction.

- **Genesis** → Deterministic math reasoner achieving 100% accuracy on algebra problems that trick LLMs (bat-and-ball puzzle, rate problems, difference equations) by solving them algebraically instead of guessing, plus device integration layer for instant natural language control of Android hardware (GPS, camera, flashlight, brightness, volume).

## 3. Critical Gaps – What AI Ish is Still Missing

### HIGH-PRIORITY (BLOCKING V1.0)

1. **Actual LLM Inference Implementation**
   - AI-Ish downloads models but doesn't run inference
   - Missing: llama.cpp JNI bridge, GGUF model loading, token streaming
   - Blocks: Every AI feature requiring text generation

2. **GPU Acceleration (Adreno 750 Optimization)**
   - Current: CPU-only placeholder inference
   - Missing: OpenCL backend, GPU layer offloading, performance benchmarking
   - Impact: 20-30 t/s potential vs current 0 t/s

3. **Permission-First Architecture**
   - Current: Basic PrivacyGuard with boolean checks
   - Missing: Live previews before file/git/shell operations, batch approval mode, command logging
   - Impact: Unsafe for production use

4. **Multimodal Vision-Language Integration**
   - Current: VisionManager returns placeholder descriptions
   - Missing: Actual Moondream2 inference, Qwen2-VL integration, vision preprocessing
   - Impact: Camera feature non-functional

5. **Speech-to-Text (Whisper Integration)**
   - Current: Wake word detection exists but no STT
   - Missing: whisper.cpp JNI, audio preprocessing, continuous listening mode
   - Impact: Voice conversation impossible

6. **Text-to-Speech**
   - Current: No TTS at all
   - Missing: Android TTS integration or local Piper model
   - Impact: No voice responses

### MEDIUM-PRIORITY (V1.1-V1.2)

7. **Expanded Knowledge Fetchers (24+ sources)**
   - Current: 3 fetchers (Wikipedia, CoinGecko, OpenMeteo)
   - Missing: arXiv, Semantic Scholar, GitHub, HuggingFace, Yahoo Finance, NYT Bestsellers, ESPN, TheSportsDB, Reddit, Data.gov, World Bank, OpenSky flights, OpenCorporates, etc.
   - Impact: Limited real-world usefulness

8. **Hierarchical Reasoning Machine**
   - Current: Basic MetaReasoner with simple routing
   - Missing: 10+ query type detection (identity, sports, news, finance, academic, planning), domain-specific fast paths, tier-based source prioritization
   - Impact: Poor query routing leads to Reddit beating ESPN for roster questions

9. **Temporal Awareness System**
   - Current: No knowledge cutoff awareness
   - Missing: Multi-layered temporal detection (identity questions, temporal keywords, explicit dates, relative time), auto-routing to live sources for post-cutoff queries
   - Impact: LLM will hallucinate about current events

10. **Adaptive Learning from Feedback**
    - Current: No learning mechanism
    - Missing: Feedback detection patterns, confidence weighting per source, route preference learning, correction storage
    - Impact: Repeats same mistakes forever

11. **Device Integration Layer**
    - Current: No Android hardware access
    - Missing: GPS location, camera control, flashlight toggle, screen brightness, volume control, sensor access via Termux API
    - Impact: Can't interact with device beyond chat

12. **Semantic Memory with Fact Extraction**
    - Current: Basic MemoryManager with episodes
    - Missing: LLM-powered fact extraction, user profile tracking (relationships, goals, preferences), vector similarity search, multi-layer memory (episodic, semantic, preference, search policy)
    - Impact: Doesn't remember important user facts

13. **Git Operations**
    - Current: No git integration
    - Missing: Clone, status, commit with preview, push/pull with warnings, branch management, diff display
    - Impact: Can't help with version control

14. **File Operations with Safety**
    - Current: Basic CodeToolPro with path allowlist
    - Missing: Automatic backups before edits, file operation previews, directory creation with parent verification
    - Impact: Risky file modifications

15. **Shell Command Execution**
    - Current: No shell access
    - Missing: Security classification (SAFE/RISKY/FORBIDDEN), command execution with timeout, safe patterns (ls, cat, pwd) vs dangerous patterns (rm -rf, fork bombs)
    - Impact: Can't run terminal commands

### LOWER-PRIORITY (V1.3+)

16. **Complex Instruction Auto-Breakdown**
    - Current: No multi-step planning
    - Missing: Numbered list detection, automatic TODO generation with status tracking, cleanup suggestions after completion
    - Impact: User must manually break down complex tasks

17. **Hardware Acceleration Manager (NPU Support)**
    - Current: No NPU integration
    - Missing: Qualcomm QNN SDK integration, Hexagon NPU detection, thermal/battery-aware device selection
    - Impact: Missing potential 10x speedup on Snapdragon 8 Gen 3

18. **Multi-Source WebSearch**
    - Current: No web search
    - Missing: DuckDuckGo integration, concurrent multi-source querying, result aggregation, 15-min caching
    - Impact: Limited to pre-downloaded knowledge

19. **Price Service (Stocks + Crypto)**
    - Current: CoinGecko for crypto only
    - Missing: Yahoo Finance for stocks, company→ticker mapping (Apple→AAPL), auto-detection of stock vs crypto queries
    - Impact: Can't answer "What's Apple's stock price?"

20. **Context-Aware Reasoning Templates**
    - Current: Basic prompting
    - Missing: 5 template types (math/logic, programming with pseudocode, system design, metacognitive, general), automatic template selection
    - Impact: Generic responses instead of specialized reasoning

21. **Tone Control System**
    - Current: No tone adaptation
    - Missing: Automatic tone detection, manual tone selection, 4 tones × 3 verbosity levels = 12 styles, persistent preferences
    - Impact: One-size-fits-all communication style

22. **Safe Code Execution Sandbox**
    - Current: No code execution
    - Missing: Sandboxed Python runtime, 30-second timeout, separate stdout/stderr, process isolation
    - Impact: Can't test code snippets

23. **Multi-Turn Context with Question IDs**
    - Current: Basic conversation history
    - Missing: Unique question IDs (q1, q2, q3), question boundary tracking, retry preserves same ID, last 15 interactions in stack
    - Impact: Retry may mix up different questions

24. **Uncertainty Detection & Intelligent Fallback**
    - Current: No uncertainty detection
    - Missing: Confidence scoring, auto-trigger external sources when confidence < 0.60, 5-tier fallback chain (Math → WebSearch → Perplexity → Claude → Local)
    - Impact: Gives wrong answers confidently

25. **Performance Monitoring Dashboard**
    - Current: No metrics
    - Missing: Real-time response time tracking, source usage statistics, user feedback correlation, overall performance rating (0-100)
    - Impact: No visibility into system health

26. **Debug Logging System**
    - Current: Basic Timber logs
    - Missing: Structured error logging (error, fallback_attempt, misrouted_execution, reasoning_issue), auto-cleanup (500 entries, 7-day retention), thread-safe JSON storage
    - Impact: Hard to diagnose issues

27. **Auto-Pruning Memory**
    - Current: Manual memory management
    - Missing: Max conversation limits (1000), auto-prune at 80% full, scoring system (recent + correct + complex), staleness detection (24h)
    - Impact: Memory bloat over time

28. **Automatic Junk Cleanup**
    - Current: No cleanup
    - Missing: Detection of files from parsing errors ("directory", "file", single chars), whitelist protection, auto-cleanup on shutdown
    - Impact: Workspace pollution

## 4. Implementation Roadmap (Prioritised)

---

### **P0 – CRITICAL FOR V1.0 (MUST-HAVE)**

---

#### **1. Actual LLM Inference Implementation**
- **Source Repo:** AILive + Genesis
- **Effort:** High
- **Files to Create:**
  - `app/src/main/cpp/llm_bridge.cpp` (JNI bridge for llama.cpp)
  - `app/src/main/java/com/ishabdullah/aiish/ml/LLMInferenceEngine.kt`
  - `app/src/main/java/com/ishabdullah/aiish/ml/TokenStreamHandler.kt`
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/ui/viewmodels/ChatViewModel.kt` (replace placeholder with actual inference)
  - `app/src/main/java/com/ishabdullah/aiish/ml/ModelManager.kt` (add model loading after download)
- **Dependencies:**
  - `llama-cpp` (C++ library, compile for ARM64)
  - CMake build scripts for JNI
- **Implementation Notes:**
  - Port AILive's `ailive_llm.cpp` bridge
  - Use Genesis's context window management (16K tokens)
  - Implement streaming callbacks for real-time UI updates
  - Add stop sequences and temperature tuning
- **Priority:** P0

---

#### **2. GPU Acceleration (OpenCL + Adreno 750)**
- **Source Repo:** Codey + AILive
- **Effort:** High
- **Files to Create:**
  - `app/src/main/cpp/gpu_backend.cpp` (OpenCL initialization)
  - `app/src/main/java/com/ishabdullah/aiish/ml/GPUManager.kt`
  - `app/src/main/java/com/ishabdullah/aiish/ml/PerformanceBenchmark.kt`
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/ml/ModelManager.kt` (add GPU layer configuration)
  - `app/build.gradle.kts` (add OpenCL build flags)
- **Dependencies:**
  - OpenCL 3.0 headers
  - Adreno GPU drivers (system-provided)
  - llama.cpp compiled with OpenCL support
- **Implementation Notes:**
  - Port Codey's GPU build success (35 GPU layers for 7B models)
  - Use AILive's GPU detection and automatic CPU fallback
  - Target 4-5x speedup (3-5 t/s → 15-25 t/s)
  - Add build variants: `gpuRelease` and `cpuRelease`
- **Priority:** P0

---

#### **3. Permission-First Architecture Overhaul**
- **Source Repo:** Codey
- **Effort:** Medium
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/core/PermissionManager.kt` (replace existing PrivacyGuard)
  - `app/src/main/java/com/ishabdullah/aiish/ui/components/PermissionDialog.kt`
  - `app/src/main/java/com/ishabdullah/aiish/ui/components/PreviewDialog.kt`
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/core/PrivacyGuard.kt` (integrate with new PermissionManager)
  - `app/src/main/java/com/ishabdullah/aiish/code/CodeToolPro.kt` (add previews before file ops)
- **Dependencies:** None (pure Kotlin)
- **Implementation Notes:**
  - Port Codey's 188-line permission system
  - Add previews for: file create/edit/delete, git operations, shell commands
  - Implement batch approval mode for multiple operations
  - Add command decision logging (audit trail)
  - Show affected files with color-coded warnings
- **Priority:** P0

---

#### **4. Multimodal Vision-Language Integration**
- **Source Repo:** AILive
- **Effort:** High
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/vision/VisionInferenceEngine.kt`
  - `app/src/main/java/com/ishabdullah/aiish/vision/VisionPreprocessor.kt`
  - `app/src/main/java/com/ishabdullah/aiish/vision/MultimodalFusion.kt`
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/vision/VisionManager.kt` (replace placeholder with real inference)
  - `app/src/main/java/com/ishabdullah/aiish/ui/viewmodels/CameraViewModel.kt` (integrate actual model)
  - `app/src/main/java/com/ishabdullah/aiish/ml/ModelInfo.kt` (add Qwen2-VL model entry)
- **Dependencies:**
  - ONNX Runtime 1.19.2
  - Qwen2-VL-2B-Instruct (3.7GB GGUF or ONNX)
- **Implementation Notes:**
  - Port AILive's VisionPreprocessor (960x960 image normalization)
  - Implement 5-stage multimodal fusion pipeline
  - Add text-only fallback when vision unavailable
  - Use CameraX with backpressure strategy (KEEP_ONLY_LATEST)
  - Throttle analysis to 200ms intervals (5 fps)
- **Priority:** P0

---

#### **5. Speech-to-Text (Whisper Integration)**
- **Source Repo:** AILive + Genesis
- **Effort:** Medium
- **Files to Create:**
  - `app/src/main/cpp/whisper_bridge.cpp` (JNI for whisper.cpp)
  - `app/src/main/java/com/ishabdullah/aiish/audio/WhisperSTT.kt`
  - `app/src/main/java/com/ishabdullah/aiish/audio/AudioRecorder.kt`
  - `app/src/main/java/com/ishabdullah/aiish/audio/ContinuousListeningService.kt`
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/wake/WakeWordManager.kt` (integrate STT after wake word)
  - `app/src/main/java/com/ishabdullah/aiish/ui/screens/ChatScreen.kt` (add microphone button)
- **Dependencies:**
  - whisper.cpp (compile for ARM64)
  - Whisper-Tiny model (145MB int8)
  - Android MediaRecorder
- **Implementation Notes:**
  - Port AILive's Whisper integration (TFLite or C++)
  - Add voice activity detection (VAD)
  - Implement continuous listening mode with low power consumption
  - Show live transcription in UI
- **Priority:** P0

---

#### **6. Text-to-Speech**
- **Source Repo:** AILive
- **Effort:** Low
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/audio/TTSManager.kt`
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/ui/viewmodels/ChatViewModel.kt` (add TTS after response generation)
  - `app/src/main/java/com/ishabdullah/aiish/ui/screens/SettingsScreen.kt` (add TTS toggle)
- **Dependencies:**
  - Android TextToSpeech API (built-in)
  - Optional: Piper TTS for offline voices (future enhancement)
- **Implementation Notes:**
  - Use Android's built-in TTS first (zero dependencies)
  - Add voice selection in settings
  - Implement play/pause/stop controls in chat UI
  - Consider Piper for offline voice cloning in v1.1
- **Priority:** P0

---

### **P1 – IMPORTANT FOR V1.1 (NEXT PHASE)**

---

#### **7. Expanded Knowledge Fetchers (24+ sources)**
- **Source Repo:** Adaptheon
- **Effort:** High
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/knowledge/fetchers/ArxivFetcher.kt`
  - `app/src/main/java/com/ishabdullah/aiish/knowledge/fetchers/SemanticScholarFetcher.kt`
  - `app/src/main/java/com/ishabdullah/aiish/knowledge/fetchers/GitHubFetcher.kt`
  - `app/src/main/java/com/ishabdullah/aiish/knowledge/fetchers/HuggingFaceFetcher.kt`
  - `app/src/main/java/com/ishabdullah/aiish/knowledge/fetchers/YahooFinanceFetcher.kt`
  - `app/src/main/java/com/ishabdullah/aiish/knowledge/fetchers/NYTBestsellerFetcher.kt`
  - `app/src/main/java/com/ishabdullah/aiish/knowledge/fetchers/ESPNFetcher.kt`
  - `app/src/main/java/com/ishabdullah/aiish/knowledge/fetchers/TheSportsDBFetcher.kt`
  - `app/src/main/java/com/ishabdullah/aiish/knowledge/fetchers/RedditFetcher.kt`
  - `app/src/main/java/com/ishabdullah/aiish/knowledge/fetchers/NewsAPIFetcher.kt`
  - (+ 14 more government/corporate/transportation fetchers)
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/knowledge/FetcherRegistry.kt` (add 100+ domain keywords)
  - `app/src/main/java/com/ishabdullah/aiish/knowledge/KnowledgeScout.kt` (add domain-specific routing)
- **Dependencies:**
  - Retrofit for REST APIs
  - Jsoup for HTML parsing
  - API keys: None required (all use free tiers or no-key APIs)
- **Implementation Notes:**
  - Port all 30 fetchers from Adaptheon's fetcher_registry.py
  - Implement domain keyword routing (100+ keywords)
  - Add fetcher confidence scoring
  - Implement result caching with topic-based TTL
- **Priority:** P1

---

#### **8. Hierarchical Reasoning Machine**
- **Source Repo:** Adaptheon
- **Effort:** High
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/core/HierarchicalReasoningMachine.kt` (453 lines)
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/core/MetaReasoner.kt` (integrate HRM for intent classification)
  - `app/src/main/java/com/ishabdullah/aiish/knowledge/KnowledgeScout.kt` (use HRM's domain routing)
- **Dependencies:** None (pure Kotlin)
- **Implementation Notes:**
  - Port 10+ query type detection (identity, sports, news, finance, academic, planning, corrections, memory ops)
  - Add domain-specific fast paths (sports, news, finance)
  - Implement tier-based source prioritization (tier > confidence)
  - Add roster vs result query classification for sports
  - Explicitly reject Reddit for identity questions
- **Priority:** P1

---

#### **9. Temporal Awareness System**
- **Source Repo:** Adaptheon + Genesis
- **Effort:** Medium
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/core/TemporalAwareness.kt` (279 lines)
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/core/MetaReasoner.kt` (add temporal detection before routing)
  - `app/src/main/java/com/ishabdullah/aiish/ml/LLMInferenceEngine.kt` (inject cutoff hint in prompts)
- **Dependencies:** None
- **Implementation Notes:**
  - Port Adaptheon's 5-layer temporal detection:
    1. Identity questions ("Who is the current...")
    2. Temporal keywords (today, now, current, latest, 2025)
    3. Always-temporal domains (price, weather, score, news, bestseller)
    4. Explicit years/dates after training cutoff
    5. Relative time (yesterday, last week, this year)
  - Add knowledge cutoff config (June 2023 for CodeLlama, Jan 2025 for Phi-4)
  - Auto-route to live sources for post-cutoff queries
  - Inject temporal hint: "Your cutoff is [DATE], ONLY use provided sources"
- **Priority:** P1

---

#### **10. Adaptive Learning from Feedback**
- **Source Repo:** Adaptheon + Genesis
- **Effort:** Medium
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/learning/FeedbackDetector.kt`
  - `app/src/main/java/com/ishabdullah/aiish/learning/FeedbackStore.kt`
  - `app/src/main/java/com/ishabdullah/aiish/learning/ToolLearningEngine.kt`
  - `app/src/main/java/com/ishabdullah/aiish/learning/ConfidenceWeighting.kt`
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/ui/viewmodels/ChatViewModel.kt` (add feedback detection)
  - `app/src/main/java/com/ishabdullah/aiish/core/MetaReasoner.kt` (inject learned preferences)
- **Dependencies:**
  - Room database for feedback storage
- **Implementation Notes:**
  - Port pattern-based correction detection ("That's wrong, use ESPN for sports")
  - Extract structured preferences: preferred_tools, domain, correction_note
  - Implement confidence weighting per source (starts 0.70, adapts with feedback)
  - Build routing rules from feedback: "sports → prefer espn"
  - Store in 4 tables: conversations, turns, feedback_events, feedback_extractions
- **Priority:** P1

---

#### **11. Device Integration Layer**
- **Source Repo:** Genesis
- **Effort:** Medium
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/device/DeviceManager.kt` (447 lines)
  - `app/src/main/java/com/ishabdullah/aiish/device/LocationService.kt`
  - `app/src/main/java/com/ishabdullah/aiish/device/CameraController.kt`
  - `app/src/main/java/com/ishabdullah/aiish/device/FlashlightController.kt`
  - `app/src/main/java/com/ishabdullah/aiish/device/SystemController.kt`
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/core/MetaReasoner.kt` (add device command detection)
- **Dependencies:**
  - Android Camera2 API
  - Android LocationManager
  - Play Services Location (if using FusedLocationProvider)
- **Implementation Notes:**
  - Port Genesis's pattern-matching for instant device commands:
    - GPS: "where am i?", "what's my location?"
    - Camera: "take a photo", "take a selfie"
    - Flashlight: "turn on flashlight", "torch on"
    - Brightness: "set brightness to 150"
    - Volume: "set music volume to 10"
  - Add runtime permission requests (CAMERA, ACCESS_FINE_LOCATION)
  - Implement instant execution (bypass LLM for speed)
  - Add safety confirmations for camera/location
- **Priority:** P1

---

#### **12. Semantic Memory with Fact Extraction**
- **Source Repo:** AILive + Adaptheon
- **Effort:** High
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/memory/UnifiedMemoryManager.kt`
  - `app/src/main/java/com/ishabdullah/aiish/memory/FactExtractor.kt`
  - `app/src/main/java/com/ishabdullah/aiish/memory/UserProfileManager.kt`
  - `app/src/main/java/com/ishabdullah/aiish/memory/SemanticSearchEngine.kt`
  - `app/src/main/java/com/ishabdullah/aiish/memory/dao/FactDao.kt`
  - `app/src/main/java/com/ishabdullah/aiish/memory/dao/ProfileDao.kt`
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/memory/MemoryManager.kt` (upgrade to multi-layer)
  - Database schema: Add tables for facts, user_profile, search_policies
- **Dependencies:**
  - Room 2.6.1 (already included)
  - BGE-small-en-v1.5 embeddings (133MB) for semantic search
  - ONNX Runtime for embedding inference
- **Implementation Notes:**
  - Port AILive's 4-layer memory: episodic, semantic, preference, search policy
  - Implement LLM-powered fact extraction with regex fallback
  - Track user relationships, goals, preferences
  - Add vector similarity search with BGE embeddings
  - Store facts with source, confidence, URL metadata
- **Priority:** P1

---

#### **13. Git Operations**
- **Source Repo:** Codey
- **Effort:** Medium
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/git/GitManager.kt` (329 lines)
  - `app/src/main/java/com/ishabdullah/aiish/git/GitCommandExecutor.kt`
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/core/MetaReasoner.kt` (add git command detection)
  - `app/src/main/java/com/ishabdullah/aiish/core/PermissionManager.kt` (add git-specific permissions)
- **Dependencies:**
  - JGit library (pure Java Git implementation) OR shell git commands
- **Implementation Notes:**
  - Port Codey's git workflow: clone, status, commit, push, pull, init
  - Add commit message preview with affected files
  - Implement warnings for push (especially force push to main)
  - Show git diff before commit
  - Add branch management (checkout, create, list)
  - Require permission for destructive operations
- **Priority:** P1

---

#### **14. File Operations with Safety**
- **Source Repo:** Codey
- **Effort:** Low
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/code/CodeToolPro.kt` (enhance existing)
- **Dependencies:** None
- **Implementation Notes:**
  - Add automatic backups before edits (copy to .bak files)
  - Implement file operation previews (show content before create/edit)
  - Add directory creation with parent verification (ls parent first)
  - Improve path allowlist UI (show allowed paths in settings)
  - Add file history tracking (last 10 operations)
- **Priority:** P1

---

#### **15. Shell Command Execution**
- **Source Repo:** Codey + Genesis
- **Effort:** Medium
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/shell/ShellManager.kt` (519 lines)
  - `app/src/main/java/com/ishabdullah/aiish/shell/SecurityClassifier.kt`
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/core/PermissionManager.kt` (add shell permissions)
- **Dependencies:** None (uses ProcessBuilder)
- **Implementation Notes:**
  - Port Codey's 3-tier security classification:
    - SAFE: ls, cat, pwd, grep, find, wc, echo, date (auto-approve)
    - RISKY: mkdir, pip install, git push, apt install (require permission)
    - FORBIDDEN: rm -rf /, mkfs, dd if=.* of=/dev/, fork bombs, git push --force main
  - Add regex pattern detection for dangerous commands
  - Implement 30-second timeout (from Genesis)
  - Separate stdout/stderr capture
  - Add command history and logging
- **Priority:** P1

---

### **P2 – NICE-TO-HAVE FOR V1.3+ (FUTURE ENHANCEMENTS)**

---

#### **16. Complex Instruction Auto-Breakdown**
- **Source Repo:** Codey
- **Effort:** Medium
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/planning/InstructionBreakdown.kt`
  - `app/src/main/java/com/ishabdullah/aiish/planning/TodoTracker.kt`
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/ui/viewmodels/ChatViewModel.kt` (detect multi-step instructions)
- **Dependencies:** None
- **Implementation Notes:**
  - Detect numbered lists (1., 2., etc.)
  - Detect "then", "after", "step by step" patterns
  - Extract individual steps automatically
  - Show TODO list with status indicators (✅ ❌ ⏳ ⊘)
  - Offer cleanup after completion
- **Priority:** P2

---

#### **17. Hardware Acceleration Manager (NPU Support)**
- **Source Repo:** Genesis
- **Effort:** Very High
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/ml/AccelerationManager.kt` (643+ lines)
  - `app/src/main/cpp/qnn_backend.cpp` (Qualcomm QNN SDK integration)
- **Dependencies:**
  - Qualcomm QNN SDK (Hexagon NPU)
  - Complex build process
- **Implementation Notes:**
  - Port Genesis's NPU architecture (500 GFLOPS INT8 on Hexagon)
  - Add thermal/battery-aware device selection (NPU > GPU > CPU)
  - Implement automatic benchmark caching (24h)
  - Add Q8_0 quantization for NPU
  - Target <3s response time
- **Priority:** P2 (experimental, GPU sufficient for v1.x)

---

#### **18. Multi-Source WebSearch**
- **Source Repo:** Genesis + Adaptheon
- **Effort:** Medium
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/websearch/WebSearchManager.kt`
  - `app/src/main/java/com/ishabdullah/aiish/websearch/DuckDuckGoSearch.kt`
  - `app/src/main/java/com/ishabdullah/aiish/websearch/ArxivSearch.kt`
- **Dependencies:**
  - OkHttp (already included)
  - Jsoup for HTML parsing
- **Implementation Notes:**
  - Add concurrent multi-source querying
  - Implement result aggregation and deduplication
  - Add 15-minute result caching
  - Integrate with knowledge fetchers
- **Priority:** P2

---

#### **19. Price Service Enhancement (Stocks + Crypto)**
- **Source Repo:** Adaptheon + Genesis
- **Effort:** Low
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/knowledge/PriceService.kt` (206 lines)
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/knowledge/fetchers/CoinGeckoFetcher.kt` (already exists)
- **Dependencies:**
  - Yahoo Finance API (no key required)
- **Implementation Notes:**
  - Add company→ticker mapping (Apple→AAPL, 30+ companies)
  - Auto-detect stock vs crypto queries
  - Return: price, change, change_percent, previous_close, timestamp, source
  - Add ticker pattern detection (2-5 uppercase letters)
- **Priority:** P2

---

#### **20. Context-Aware Reasoning Templates**
- **Source Repo:** Genesis
- **Effort:** Medium
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/reasoning/ReasoningTemplates.kt` (735+ lines)
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/ml/LLMInferenceEngine.kt` (apply templates)
- **Dependencies:** None
- **Implementation Notes:**
  - Port 5 template types: math/logic, programming, system design, metacognitive, general
  - Add automatic template selection based on query type
  - Implement pseudocode generation for programming tasks
  - Add step-by-step calculation traces
- **Priority:** P2

---

#### **21. Tone Control System**
- **Source Repo:** Genesis
- **Effort:** Low
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/tone/ToneController.kt` (418 lines)
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/ui/screens/SettingsScreen.kt` (add tone selection)
  - `app/src/main/java/com/ishabdullah/aiish/ml/LLMInferenceEngine.kt` (inject tone in prompt)
- **Dependencies:** None
- **Implementation Notes:**
  - Add 4 tones: technical, conversational, advisory, concise
  - Add 3 verbosity levels: short, medium, long
  - Implement automatic tone detection from query patterns
  - Add manual tone selection with persistence
- **Priority:** P2

---

#### **22. Safe Code Execution Sandbox**
- **Source Repo:** Genesis
- **Effort:** Medium
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/execution/CodeExecutor.kt` (120 lines)
- **Dependencies:** None (uses ProcessBuilder)
- **Implementation Notes:**
  - Create sandboxed `runtime/` directory
  - Add 30-second timeout for safety
  - Separate stdout/stderr capture
  - Add process isolation
  - Support Python code execution
- **Priority:** P2

---

#### **23. Multi-Turn Context with Question IDs**
- **Source Repo:** Genesis
- **Effort:** Low
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/memory/MemoryManager.kt` (add question ID tracking)
  - `app/src/main/java/com/ishabdullah/aiish/ui/viewmodels/ChatViewModel.kt` (assign IDs)
- **Dependencies:** None
- **Implementation Notes:**
  - Assign unique IDs (q1, q2, q3) to each question
  - Track question boundaries
  - Preserve ID on retry
  - Maintain last 15 interactions in context stack
- **Priority:** P2

---

#### **24. Uncertainty Detection & Intelligent Fallback**
- **Source Repo:** Genesis
- **Effort:** Medium
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/reasoning/UncertaintyDetector.kt` (270 lines)
  - `app/src/main/java/com/ishabdullah/aiish/reasoning/FallbackChain.kt`
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/core/MetaReasoner.kt` (integrate uncertainty detection)
- **Dependencies:** None
- **Implementation Notes:**
  - Implement confidence scoring
  - Auto-trigger fallback when confidence < 0.60
  - Build 5-tier chain: Math → WebSearch → Perplexity → Claude → Local
  - Track uncertainty patterns for learning
- **Priority:** P2

---

#### **25. Performance Monitoring Dashboard**
- **Source Repo:** Genesis
- **Effort:** Low
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/monitoring/PerformanceMonitor.kt` (470+ lines)
  - `app/src/main/java/com/ishabdullah/aiish/ui/screens/PerformanceScreen.kt`
- **Dependencies:** None
- **Implementation Notes:**
  - Track real-time response times
  - Log source usage statistics
  - Correlate user feedback with performance
  - Calculate overall performance rating (0-100)
  - Add UI dashboard in settings
- **Priority:** P2

---

#### **26. Debug Logging System**
- **Source Repo:** Genesis
- **Effort:** Low
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/debug/DebugLogger.kt` (200 lines)
- **Dependencies:**
  - Room for structured storage
- **Implementation Notes:**
  - Add 4 log types: error, fallback_attempt, misrouted_execution, reasoning_issue
  - Implement auto-cleanup (500 entries, 7-day retention)
  - Thread-safe JSON storage
  - Add export functionality
- **Priority:** P2

---

#### **27. Auto-Pruning Memory**
- **Source Repo:** Genesis
- **Effort:** Low
- **Files to Modify:**
  - `app/src/main/java/com/ishabdullah/aiish/memory/MemoryManager.kt` (add auto-pruning)
- **Dependencies:** None
- **Implementation Notes:**
  - Set max conversations to 1000
  - Auto-prune at 80% full
  - Implement scoring: recent + correct + complex = higher retention
  - Add staleness detection (24h threshold)
  - Preserve important conversations
- **Priority:** P2

---

#### **28. Automatic Junk Cleanup**
- **Source Repo:** Codey
- **Effort:** Low
- **Files to Create:**
  - `app/src/main/java/com/ishabdullah/aiish/cleanup/JunkCleaner.kt` (121 lines)
- **Dependencies:** None
- **Implementation Notes:**
  - Detect files from parsing errors: "directory", "file", "the", single chars
  - Add whitelist to protect intentional files
  - Run cleanup on app shutdown
  - Show cleanup suggestions to user
- **Priority:** P2

---

## 5. Final Vision – What AI Ish Becomes When This Plan Is Executed

AI Ish will be the **world's first truly private, fully autonomous AI companion** that runs 100% on your Android device—combining ChatGPT-level conversation with Claude Code's safety philosophy, all accelerated by mobile GPU/NPU for sub-3-second responses. It will understand your voice, see through your camera, remember your preferences across sessions, control your device hardware with natural language, access live knowledge from 24+ specialized sources with temporal awareness, learn from your corrections, execute code safely, manage git repositories, solve math problems with 100% accuracy, and adapt its personality to your communication style—all while guaranteeing that your data never leaves your phone. This isn't just an AI assistant; it's your personal JARVIS that lives in your pocket, works offline anywhere, and gets smarter the more you use it. When this roadmap is complete, AI Ish will be the definitive proof that privacy and power are not mutually exclusive.
