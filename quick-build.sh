#!/data/data/com.termux/files/usr/bin/bash
#
# AI-Ish Quick Build Script (Termux)
# Fast rebuild for development iterations
#
# Copyright (c) 2025 Ismail Abdullah. All rights reserved.
#

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🚀 AI-Ish Quick Build${NC}"
echo ""

# Set environment
export ANDROID_HOME="$HOME/android-sdk"
export JAVA_HOME="$PREFIX"
export GRADLE_USER_HOME="$HOME/.gradle"

# Build debug APK (faster than release)
echo -e "${BLUE}▶ Building debug APK...${NC}"
./gradlew assembleDebug --no-daemon

# Find and report APK
APK_PATH=$(find app/build/outputs/apk/debug -name "*.apk" | head -n 1)

if [ -n "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo ""
    echo -e "${GREEN}✅ Build complete!${NC}"
    echo -e "  📦 APK: $APK_PATH"
    echo -e "  📊 Size: $APK_SIZE"

    # Copy to Downloads
    if [ -d "/sdcard/Download" ]; then
        DOWNLOAD_PATH="/sdcard/Download/ai-ish-debug-latest.apk"
        cp "$APK_PATH" "$DOWNLOAD_PATH"
        echo -e "  💾 Copied to: $DOWNLOAD_PATH"
    fi
else
    echo -e "${RED}❌ APK not found!${NC}"
    exit 1
fi

echo ""
