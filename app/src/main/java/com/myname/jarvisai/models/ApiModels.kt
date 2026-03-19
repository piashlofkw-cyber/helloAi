package com.myname.jarvisai.models

import com.google.gson.annotations.SerializedName

// Groq API Models
data class GroqRequest(
    val model: String = "mixtral-8x7b-32768",
    val messages: List<Message>,
    val temperature: Float = 0.7f,
    @SerializedName("max_tokens") val maxTokens: Int = 1024
)

data class GroqResponse(
    val choices: List<Choice>,
    val usage: Usage?
)

// OpenRouter API Models
data class OpenRouterRequest(
    val model: String = "openai/gpt-4-turbo",
    val messages: List<Message>,
    val temperature: Float = 0.7f,
    @SerializedName("max_tokens") val maxTokens: Int = 1024
)

data class OpenRouterResponse(
    val choices: List<Choice>,
    val usage: Usage?
)

// Shared Models
data class Message(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class Choice(
    val message: Message,
    @SerializedName("finish_reason") val finishReason: String?
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)

// ElevenLabs TTS Models
data class ElevenLabsTTSRequest(
    val text: String,
    @SerializedName("model_id") val modelId: String = "eleven_monolingual_v1",
    @SerializedName("voice_settings") val voiceSettings: VoiceSettings = VoiceSettings()
)

data class VoiceSettings(
    val stability: Float = 0.5f,
    @SerializedName("similarity_boost") val similarityBoost: Float = 0.75f
)

// Avatar Models
data class AvatarContext(
    val mood: String, // "neutral", "happy", "thinking", "speaking"
    val lighting: String, // "soft", "dramatic", "natural"
    val angle: String, // "front", "slight_left", "slight_right"
    val background: String // "dark", "office", "futuristic"
)

// Voice Assistant State
enum class AssistantState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

// Call Info
data class CallInfo(
    val phoneNumber: String,
    val contactName: String?,
    val timestamp: Long
)
