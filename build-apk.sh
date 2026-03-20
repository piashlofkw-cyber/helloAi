#!/bin/bash

echo "🚀 Building My Jarvis AI APK..."
echo ""

cd /sdcard/AndroidIDEProjects/helloAi

# Clean previous builds
echo "🧹 Cleaning previous builds..."
rm -rf app/build/outputs/apk/

# Build debug APK
echo "📦 Building debug APK..."
bash gradlew clean assembleDebug --warning-mode all

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ BUILD SUCCESSFUL!"
    echo ""
    echo "📱 Your APK is ready:"
    echo "   Location: app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    
    # Check if APK exists
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        APK_SIZE=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
        echo "   Size: $APK_SIZE"
        echo ""
        echo "🎉 Install করুন এবং enjoy করুন!"
    fi
else
    echo ""
    echo "❌ BUILD FAILED!"
    echo ""
    echo "📝 Error log check করুন উপরে"
fi
