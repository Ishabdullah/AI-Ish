#!/data/data/com.termux/files/usr/bin/bash
#
# AI-Ish APK Build Script for Termux
# Build Android APK directly on your Samsung S24 Ultra
#
# Copyright (c) 2025 Ismail Abdullah. All rights reserved.
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print functions
print_header() {
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

print_step() {
    echo -e "${BLUE}▶ $1${NC}"
}

# Check if running in Termux
if [ ! -d "/data/data/com.termux" ]; then
    print_error "This script must be run in Termux!"
    exit 1
fi

print_header "AI-Ish APK Builder for Termux"
echo ""
print_info "Device: Samsung Galaxy S24 Ultra"
print_info "Build type: Production (NPU optimized)"
echo ""

# Step 1: Check prerequisites
print_step "Checking prerequisites..."

# Check Java
if ! command -v java &> /dev/null; then
    print_error "Java not found! Installing..."
    pkg install openjdk-17 -y
    print_success "Java installed"
else
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    print_success "Java found: $JAVA_VERSION"
fi

# Check Gradle
if ! command -v gradle &> /dev/null; then
    print_error "Gradle not found! Installing..."
    pkg install gradle -y
    print_success "Gradle installed"
else
    GRADLE_VERSION=$(gradle --version | grep "Gradle" | cut -d' ' -f2)
    print_success "Gradle found: $GRADLE_VERSION"
fi

# Check Git
if ! command -v git &> /dev/null; then
    print_error "Git not found! Installing..."
    pkg install git -y
    print_success "Git installed"
else
    GIT_VERSION=$(git --version | cut -d' ' -f3)
    print_success "Git found: $GIT_VERSION"
fi

echo ""

# Step 2: Set environment variables
print_step "Setting up environment..."

export ANDROID_HOME="$HOME/android-sdk"
export JAVA_HOME="$PREFIX"
export GRADLE_USER_HOME="$HOME/.gradle"

# Create .gradle directory if it doesn't exist
mkdir -p "$GRADLE_USER_HOME"

# Set Gradle daemon options for Termux (limited memory)
cat > "$GRADLE_USER_HOME/gradle.properties" <<EOF
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx2048m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError
org.gradle.parallel=false
android.useAndroidX=true
android.enableJetifier=true
EOF

print_success "Environment configured"
echo ""

# Step 3: Clean previous builds
print_step "Cleaning previous builds..."

if [ -d "app/build" ]; then
    rm -rf app/build
    print_success "Cleaned app/build"
fi

if [ -d "build" ]; then
    rm -rf build
    print_success "Cleaned build"
fi

echo ""

# Step 4: Build APK
print_step "Building production APK..."
print_info "This may take 10-20 minutes on first build..."
echo ""

# Build release APK
if ./gradlew assembleRelease --no-daemon --stacktrace; then
    print_success "Build successful!"
    echo ""

    # Step 5: Locate APK
    print_step "Locating APK..."

    APK_PATH=$(find app/build/outputs/apk/release -name "*.apk" | head -n 1)

    if [ -n "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        print_success "APK built successfully!"
        echo ""
        print_header "Build Complete"
        echo ""
        echo -e "  📦 APK Location: ${GREEN}$APK_PATH${NC}"
        echo -e "  📊 APK Size: ${GREEN}$APK_SIZE${NC}"
        echo ""
        echo -e "  ${YELLOW}Installation:${NC}"
        echo -e "     1. Transfer APK to device storage"
        echo -e "     2. Open file manager and tap APK to install"
        echo -e "     3. Allow installation from unknown sources if prompted"
        echo ""

        # Optional: Copy to Downloads
        if [ -d "/sdcard/Download" ]; then
            DOWNLOAD_PATH="/sdcard/Download/ai-ish-production-$(date +%Y%m%d-%H%M%S).apk"
            cp "$APK_PATH" "$DOWNLOAD_PATH"
            print_success "APK copied to: $DOWNLOAD_PATH"
        fi

        echo ""
        print_success "✨ Build complete! Ready to install on S24 Ultra."
        echo ""

    else
        print_error "APK not found in expected location!"
        print_info "Check app/build/outputs/apk/release/"
        exit 1
    fi

else
    print_error "Build failed!"
    echo ""
    print_info "Common fixes:"
    echo "  1. Ensure you have enough storage space (8GB+ free)"
    echo "  2. Try running: ./gradlew clean"
    echo "  3. Check build logs above for specific errors"
    echo "  4. Restart Termux and try again"
    exit 1
fi

echo ""
print_header "Build Script Complete"
