#!/bin/bash
# Build script for EleMonitor Android App

set -e

echo "========================================"
echo "  EleMonitor Android App Builder"
echo "========================================"

# Check if gradlew exists
if [ ! -f "gradlew" ]; then
    echo "Downloading Gradle wrapper..."
    # Create minimal gradlew
    echo '#!/bin/bash' > gradlew
    echo 'exec gradle "$@"' >> gradlew
    chmod +x gradlew
fi

echo "Building debug APK..."
./gradlew assembleDebug

echo ""
echo "========================================"
echo "  Build Complete!"
echo "========================================"
echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "Install to phone:"
echo "  adb install app/build/outputs/apk/debug/app-debug.apk"
