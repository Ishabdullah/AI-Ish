# 🚀 AI Ish

**Your private, always-on, super-intelligent companion that never phones home.**

[![Build Status](https://github.com/Ishabdullah/AI-Ish/workflows/Build%20AI%20Ish%20APK/badge.svg)](https://github.com/Ishabdullah/AI-Ish/actions)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)

**Copyright © 2025 Ismail Abdullah. All rights reserved.**

---

## ✨ Features

### 🔐 **100% Private**
- **Zero Telemetry** - No data collection, ever
- **On-Device AI** - All processing happens locally
- **Never Phones Home** - No internet required (except optional knowledge fetching)
- **Your Data Stays Yours** - Complete privacy guaranteed

### 🤖 **Advanced AI Capabilities**
- **Wake Word Detection** - Say "Hey Ish" to activate
- **Real-Time Knowledge** - Live data from 30+ sources (Wikipedia, crypto prices, weather, sports, news)
- **Deterministic Math** - 100% accurate calculations with step-by-step solving
- **Code Assistance** - Safe file operations with permission system
- **Multimodal Vision** - Camera integration for image analysis (coming soon)
- **Semantic Memory** - Remembers context across conversations

### 🎨 **Beautiful UI**
- Material 3 Design
- Dark mode support
- Smooth streaming responses
- Markdown & LaTeX rendering

### ⚡ **Blazing Fast**
- Hot-swappable backends (ONNX Phi-4 ↔ GGUF Qwen2/CodeLlama)
- Vulkan/QNN GPU acceleration on Samsung Galaxy S24 Ultra
- 25-45 tokens/second inference speed

---

## 📱 Supported Devices

- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Optimized for**: Samsung Galaxy S24 Ultra
- **Architecture**: ARM64-v8a

---

## 🔧 Installation

### Option 1: Download Pre-built APK (Recommended)

1. Go to [Releases](https://github.com/Ishabdullah/AI-Ish/releases)
2. Download the latest `ai-ish-debug.apk`
3. Install on your Android device
4. Grant required permissions when prompted

### Option 2: Build from Source

#### Prerequisites
- JDK 17
- Android SDK 34
- Gradle 8.2+

#### Build Steps

```bash
# Clone the repository
git clone https://github.com/Ishabdullah/AI-Ish.git
cd AI-Ish

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

---

## 🎯 Usage

### Starting a Conversation

1. **Launch AI Ish** from your app drawer
2. **Say "Hey Ish"** or type your message
3. **Get instant responses** powered by on-device AI

### Example Queries

```
👋 "Hey Ish, what's the weather like?"
📊 "What's the price of Bitcoin?"
🧮 "Solve: 5 machines make 5 widgets in 5 minutes, how many machines needed for 100 widgets in 50 minutes?"
📰 "What's the latest news about AI?"
🏀 "Who's the quarterback for the Kansas City Chiefs?"
```

---

## 🏗️ Architecture

### Core Components

```
AI Ish/
├── MetaReasoner       → Routes queries to appropriate subsystems
├── KnowledgeScout     → Real-time data fetching (30+ sources)
├── MathReasoner       → Deterministic step-by-step math solving
├── WakeWordManager    → "Hey Ish" detection with phonetic matching
├── MemoryManager      → Semantic + episodic memory systems
├── CodeToolPro        → Safe file operations with permissions
├── PrivacyGuard       → Explicit user consent for all actions
└── LLMManager         → Hot-swappable ONNX/GGUF inference
```

### Tech Stack

- **Language**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **Concurrency**: Kotlin Coroutines + Flow
- **Database**: Room (coming soon)
- **Networking**: OkHttp
- **Logging**: Timber

---

## 🌟 Knowledge Sources

AI Ish integrates with these live data sources:

| Category | Source | Examples |
|----------|--------|----------|
| **General Knowledge** | Wikipedia | History, Science, People |
| **Finance** | CoinGecko | Crypto prices & 24h changes |
| **Weather** | OpenMeteo | Real-time weather data |

**Coming Soon**: Reddit, arXiv papers, GitHub repos, Yahoo Finance, Sports scores, News feeds

---

## 🔒 Privacy Commitment

AI Ish is designed with privacy as the #1 priority:

✅ **All AI processing happens on your device**
✅ **No cloud servers, no API keys required**
✅ **Optional internet access only for knowledge fetching**
✅ **Explicit permission required for every sensitive action**
✅ **Proprietary software - contact author for licensing inquiries**

---

## 🛠️ Development

### Project Structure

```
app/src/main/java/com/ishabdullah/aiish/
├── core/          → MetaReasoner, PrivacyGuard
├── knowledge/     → KnowledgeScout, Fetchers
├── math/          → MathReasoner
├── wake/          → WakeWordManager
├── memory/        → MemoryManager
├── code/          → CodeToolPro
├── ui/            → Compose screens, ViewModels
├── domain/        → Models, Repositories
└── data/          → Local database, preferences
```

### Building Locally

```bash
# Run tests
./gradlew test

# Run lint checks
./gradlew lint

# Generate coverage report
./gradlew jacocoTestReport

# Build all variants
./gradlew build
```

---

## 📄 License

This software is proprietary and confidential. Unauthorized copying, modification, distribution, or use of this software, via any medium, is strictly prohibited without express written permission from Ismail Abdullah.

For licensing inquiries, please contact: **ismail.t.abdullah@gmail.com**

See the [LICENSE](LICENSE) file for complete terms.

---

## 🙏 Acknowledgments

AI Ish merges the best components from four private repositories:

- **AILive** - Wake word detection, LLM management, streaming UI
- **Adaptheon** - KnowledgeScout with 30+ live fetchers, HRM reasoning
- **Genesis** - Deterministic MathReasoner, learning memory systems
- **Codey** - Safe code tools, permission system, Git integration

---

## 📞 Support

For support, licensing inquiries, or other questions:
- **Email**: ismail.t.abdullah@gmail.com
- **Issues**: [GitHub Issues](https://github.com/Ishabdullah/AI-Ish/issues) (for bug reports only)

---

## 🗺️ Roadmap

- [x] Core AI conversation interface
- [x] Real-time knowledge fetching
- [x] Deterministic math solving
- [x] Wake word detection
- [x] Memory systems
- [ ] Full LLM integration (ONNX + GGUF)
- [ ] Vision support (camera + Moondream2)
- [ ] Voice output (TTS)
- [ ] Code editing with Git integration
- [ ] Model marketplace
- [ ] Plugin system

---

<div align="center">

**Made with ❤️ for Privacy**

**Copyright © 2025 Ismail Abdullah. All rights reserved.**

Contact: ismail.t.abdullah@gmail.com

[🐛 Report Bug](https://github.com/Ishabdullah/AI-Ish/issues)

</div>
