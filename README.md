# My Jarvis AI - Advanced AI Voice Assistant for Android

An intelligent voice assistant powered by cutting-edge AI technologies including Groq, OpenRouter, and ElevenLabs Text-to-Speech.

## 🚀 Features

- **AI-Powered Conversations**: Integrates with Groq and OpenRouter for intelligent responses
- **Natural Voice Synthesis**: ElevenLabs TTS for human-like speech
- **Smart Call Screening**: Automatically detects and filters spam/scam calls
- **SMS Intelligence**: AI-powered SMS analysis and OTP detection
- **Device Automation**: Accessibility service for hands-free device control
- **Dynamic AI Avatar**: Context-aware visual avatar with locked facial identity
- **Futuristic UI**: Modern, animated interface with glassmorphism effects

## 📋 Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 26 (Oreo) or higher
- API Keys:
  - Groq API Key
  - OpenRouter API Key (optional)
  - ElevenLabs API Key

## 🔧 Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/piashlofkw-cyber/helloAi.git
cd helloAi
```

### 2. Configure API Keys

1. Launch the app
2. Navigate to Settings
3. Enter your API keys:
   - Groq API Key
   - OpenRouter API Key (optional)
   - ElevenLabs API Key

### 3. Grant Permissions

The app requires the following permissions:
- **Microphone**: For voice commands
- **Phone**: For call screening
- **SMS**: For message intelligence
- **Accessibility**: For device automation
- **Notifications**: For alerts

### 4. Build the APK

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

The APK will be generated in `app/build/outputs/apk/`

## 📱 Usage

1. **Voice Commands**: Tap the microphone button and speak
2. **Settings**: Configure API keys and preferences
3. **Call Screening**: Automatic - spam calls are filtered
4. **SMS Intelligence**: Automatic - OTP and urgent messages are detected

## 🎨 Avatar System

The AI avatar uses a locked facial identity system:
- **Face Structure**: Locked and consistent
- **Variations**: Pose, lighting, expression, background
- **Reference Image**: Place your reference avatar in `app/src/main/res/drawable/`

To replace the default avatar:
1. Add `reference_avatar.jpg` to `app/src/main/res/drawable/`
2. The avatar system will use this as the base face

## 🔒 Security & Privacy

- API keys are stored locally using Android SharedPreferences
- No data is sent to third parties except API providers
- Call screening runs on-device
- Accessibility service only acts on explicit user commands

## 🛠️ Technologies Used

- **Kotlin**: Primary language
- **Retrofit**: API communication
- **Coroutines**: Asynchronous operations
- **Material Design 3**: Modern UI components
- **Coil**: Image loading
- **Lottie**: Animations

## 📄 License

This project is licensed under the MIT License.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📧 Support

For issues or questions, please open an issue on GitHub.

---

**Built with ❤️ using Letta Code AI Assistant**
