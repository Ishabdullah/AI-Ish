# 🏗️ AI-Ish Build Instructions

Complete guide for building AI-Ish APK on Samsung Galaxy S24 Ultra.

---

## 📋 Prerequisites

### Hardware Requirements
- **Device**: Samsung Galaxy S24 Ultra (Snapdragon 8 Gen 3)
- **RAM**: 12GB
- **Storage**: 8GB+ free space
- **Android**: Android 14 (API 34)

### Software Requirements (Termux)
```bash
# Install Termux from F-Droid (recommended)
# https://f-droid.org/en/packages/com.termux/

# Update packages
pkg update && pkg upgrade

# Install build tools
pkg install openjdk-17 gradle git
```

---

## 🚀 Quick Start (Termux)

### 1. Clone Repository
```bash
cd ~
git clone https://github.com/Ishabdullah/AI-Ish.git
cd AI-Ish
```

### 2. Build Production APK
```bash
./build-termux.sh
```

**Output**: `app/build/outputs/apk/release/app-release-unsigned.apk`

**Time**: 10-20 minutes (first build), 5-10 minutes (subsequent builds)

### 3. Build Debug APK (Faster)
```bash
./quick-build.sh
```

**Output**: `app/build/outputs/apk/debug/app-debug.apk`

**Time**: 5-10 minutes

---

## 🔧 Manual Build (Termux)

### Setup Environment
```bash
# Set environment variables
export ANDROID_HOME="$HOME/android-sdk"
export JAVA_HOME="$PREFIX"
export GRADLE_USER_HOME="$HOME/.gradle"

# Create Gradle properties
mkdir -p ~/.gradle
cat > ~/.gradle/gradle.properties <<EOF
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx2048m
org.gradle.parallel=false
android.useAndroidX=true
android.enableJetifier=true
EOF
```

### Build Commands
```bash
# Clean previous builds
./gradlew clean

# Build debug APK (faster, includes debug symbols)
./gradlew assembleDebug --no-daemon

# Build release APK (optimized, smaller size)
./gradlew assembleRelease --no-daemon

# Build with stacktrace (for debugging)
./gradlew assembleRelease --no-daemon --stacktrace

# Build with info logging
./gradlew assembleRelease --no-daemon --info
```

### Output Locations
```
app/build/outputs/apk/
├── debug/
│   └── app-debug.apk          (Debug build with symbols)
└── release/
    └── app-release-unsigned.apk  (Optimized production build)
```

---

## 🖥️ Desktop Build (Linux/macOS/Windows)

### Prerequisites
```bash
# Install JDK 17
# Ubuntu/Debian:
sudo apt install openjdk-17-jdk

# macOS:
brew install openjdk@17

# Windows: Download from https://adoptium.net/
```

### Build Steps
```bash
# Clone repository
git clone https://github.com/Ishabdullah/AI-Ish.git
cd AI-Ish

# Set ANDROID_HOME (if Android SDK installed)
export ANDROID_HOME=/path/to/android/sdk

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

---

## 🔍 Troubleshooting

### Build Fails with "Out of Memory"
```bash
# Increase Gradle heap size
echo "org.gradle.jvmargs=-Xmx4096m" >> ~/.gradle/gradle.properties

# Disable parallel builds
echo "org.gradle.parallel=false" >> ~/.gradle/gradle.properties

# Restart Termux
exit
# Open Termux again
```

### Build Fails with "Daemon Connection Failed"
```bash
# Kill Gradle daemon
./gradlew --stop

# Rebuild
./gradlew assembleRelease --no-daemon
```

### APK Not Found After Build
```bash
# Search for APK
find app/build/outputs/apk -name "*.apk"

# Check build logs
./gradlew assembleRelease --stacktrace
```

### Insufficient Storage
```bash
# Check available space
df -h $HOME

# Clean Gradle cache
rm -rf ~/.gradle/caches

# Clean build directories
./gradlew clean
```

### Permission Denied on Scripts
```bash
# Make scripts executable
chmod +x build-termux.sh quick-build.sh
```

---

## 📊 Build Variants

### Debug Build
- **Purpose**: Development and testing
- **Features**: Debug symbols, logging enabled
- **Size**: ~150MB
- **Performance**: Slower (no optimizations)
- **Command**: `./gradlew assembleDebug`

### Release Build
- **Purpose**: Production deployment
- **Features**: Optimized, minified, obfuscated
- **Size**: ~80MB (after ProGuard/R8)
- **Performance**: Full speed (NPU/CPU optimizations)
- **Command**: `./gradlew assembleRelease`

---

## 🎯 Production Optimization Flags

The production build includes these optimizations for S24 Ultra:

```gradle
buildTypes {
    release {
        minifyEnabled true
        shrinkResources true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt')

        ndk {
            abiFilters 'arm64-v8a'  // S24 Ultra architecture
        }

        // NPU/CPU optimizations
        externalNativeBuild {
            cmake {
                cppFlags "-O3 -DNDEBUG"
                arguments "-DANDROID_ARM_NEON=TRUE",
                          "-DENABLE_NPU=ON",
                          "-DENABLE_INT8=ON"
            }
        }
    }
}
```

---

## 📦 APK Signing (Optional)

### Create Keystore
```bash
keytool -genkey -v -keystore ai-ish-release.keystore \
    -alias ai-ish -keyalg RSA -keysize 2048 -validity 10000
```

### Sign APK
```bash
# Install apksigner (if needed)
pkg install apksigner

# Sign APK
apksigner sign --ks ai-ish-release.keystore \
    --out app-signed.apk \
    app/build/outputs/apk/release/app-release-unsigned.apk

# Verify signature
apksigner verify app-signed.apk
```

---

## 🧪 Testing Build

### Install on Device
```bash
# Via ADB (if enabled)
adb install app/build/outputs/apk/debug/app-debug.apk

# Via File Manager
# 1. Copy APK to /sdcard/Download
# 2. Open file manager
# 3. Tap APK to install
# 4. Allow installation from unknown sources
```

### Verify Production Optimizations
After installing, check logs to verify NPU/CPU allocation:

```bash
# Monitor logs
adb logcat | grep -E "AI-Ish|NPU|Mistral|MobileNet"

# Expected output:
# ✅ NPU Hexagon v81 detected
# ✅ Mistral-7B INT8 loaded (NPU + CPU)
# ✅ MobileNet-v3 INT8 loaded (NPU)
# ✅ BGE-Small INT8 loaded (CPU)
```

---

## 🚀 Performance Benchmarks

After building and installing, you should see these performance metrics on S24 Ultra:

| Component | Expected Performance |
|-----------|---------------------|
| LLM Prefill | 15-20ms (NPU) |
| LLM Decode | 25-35 tokens/sec (CPU) |
| Vision | ~60 FPS (NPU) |
| Embeddings | ~500 emb/s (CPU) |

---

## 📞 Support

Build issues? Contact:
- **Email**: ismail.t.abdullah@gmail.com
- **GitHub Issues**: [Report Build Problem](https://github.com/Ishabdullah/AI-Ish/issues)

---

**Copyright © 2025 Ismail Abdullah. All rights reserved.**
