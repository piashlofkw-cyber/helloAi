# 🔨 Build Instructions - My Jarvis AI

## ✅ সুপারিশকৃত পদ্ধতি: Android IDE ব্যবহার করুন

### **Android IDE তে Build:**

1. **Android IDE খুলুন**
2. **File → Open** → Select `/sdcard/AndroidIDEProjects/helloAi`
3. **অপেক্ষা করুন** Gradle sync শেষ হওয়ার জন্য (২-৩ মিনিট)
4. **Build → Clean Project**
5. **Build → Rebuild Project**
6. **Build → Build APK(s)**

✅ APK পাবেন: `app/build/outputs/apk/debug/app-debug.apk`

---

## 🖥️ Android IDE Terminal দিয়ে:

Android IDE এর নিচে **Terminal** tab খুলুন:

```bash
# Clean previous build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build and install (if device connected)
./gradlew installDebug
```

---

## ⚠️ সমস্যা সমাধান:

### **"SDK location not found" Error:**

**সমাধান 1:** Android IDE তে build করুন (UI থেকে)

**সমাধান 2:** `local.properties` file এ SDK path ঠিক করুন:

```properties
# সম্ভাব্য paths (আপনার Android IDE installation অনুযায়ী):
sdk.dir=/data/data/com.itsaky.androidide/files/framework/android-sdk
# অথবা
sdk.dir=/data/user/0/com.itsaky.androidide/files/framework/android-sdk
# অথবা
sdk.dir=/storage/emulated/0/AndroidIDE/android-sdk
```

Android IDE তে check করুন:
- Settings → Build Tools → Android SDK Location

---

### **Kotlin Compilation Error:**

```bash
# Full clean and rebuild
./gradlew clean build --stacktrace
```

---

### **Build খুব ধীর হলে:**

`gradle.properties` তে যোগ করুন:

```properties
org.gradle.jvmargs=-Xmx4096m
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
```

---

## 📱 APK Install করুন:

### **Method 1: File Manager থেকে**
```
1. File manager খুলুন
2. যান: /sdcard/AndroidIDEProjects/helloAi/app/build/outputs/apk/debug/
3. app-debug.apk ট্যাপ করুন
4. Install করুন
```

### **Method 2: ADB দিয়ে**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🎯 প্রথমবার Setup:

1. ✅ Build করুন
2. ✅ Install করুন
3. ✅ App খুলুন
4. ✅ Settings → API Keys configure করুন
5. ✅ Permissions দিন
6. ✅ Test করুন!

---

## 🔑 API Keys কোথায় পাবেন:

### **Groq (FREE - সুপারিশকৃত):**
- 🔗 https://console.groq.com
- ✅ Free tier: 14,400 requests/day
- ⚡ Super fast responses

### **OpenRouter (Premium Models):**
- 🔗 https://openrouter.ai
- 💰 Pay as you go
- 🧠 Access to GPT-4, Claude, Gemini

### **ElevenLabs (Voice):**
- 🔗 https://elevenlabs.io
- ✅ Free: 10,000 chars/month
- 🎤 High quality TTS

---

## 📊 Build Time:

- **প্রথমবার:** 5-10 মিনিট (dependencies download)
- **পরবর্তীতে:** 1-3 মিনিট
- **Incremental build:** 30 সেকেন্ড - 1 মিনিট

---

## 💡 Tips:

✅ সবসময় Android IDE ব্যবহার করুন build এর জন্য
✅ Gradle daemon on রাখুন (দ্রুত build)
✅ Clean build শুধু সমস্যা হলেই করুন
✅ Internet connection লাগবে প্রথমবার

---

Happy Building! 🚀
