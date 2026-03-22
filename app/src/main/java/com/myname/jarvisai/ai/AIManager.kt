package com.myname.jarvisai.ai

import android.content.Context
import android.util.Log
import com.myname.jarvisai.models.Message
import com.myname.jarvisai.models.ModelConfig
import com.myname.jarvisai.utils.PreferencesManager

/**
 * AI Manager with automatic fallback
 * Tries models in priority order until one succeeds
 */
class AIManager(private val context: Context) {

    private val prefsManager = PreferencesManager(context)
    private val groqClients = mutableMapOf<String, GroqClient>()
    private val openRouterClients = mutableMapOf<String, OpenRouterClient>()

    companion object {
        private const val TAG = "AIManager"
    }

    /**
     * Send message with automatic fallback
     */
    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<Message> = emptyList()
    ): Result {
        val enabledModels = prefsManager.getEnabledModelsByPriority()
        val autoFallback = prefsManager.isAutoFallbackEnabled()

        if (enabledModels.isEmpty()) {
            return Result.Error("No models configured. Please add models in Model Manager.")
        }

        // Detect language and create multilingual prompt
        val multilingualAI = MultilingualAI()
        val detectedLanguage = multilingualAI.detectLanguage(userMessage)
        Log.d(TAG, "🌐 Detected language: $detectedLanguage for message: ${userMessage.take(30)}...")

        // Try each model in priority order
        for ((index, model) in enabledModels.withIndex()) {
            try {
                Log.d(TAG, "Trying model: ${model.name} (priority: ${model.priority})")
                
                val response = when (model.provider.lowercase()) {
                    "groq" -> sendViaGroq(model, userMessage, conversationHistory)
                    "openrouter" -> sendViaOpenRouter(model, userMessage, conversationHistory)
                    else -> {
                        Log.w(TAG, "Unknown provider: ${model.provider}")
                        null
                    }
                }

                if (response != null) {
                    Log.i(TAG, "✅ Success with ${model.name}")
                    return Result.Success(response, model)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed with ${model.name}: ${e.message}")
                
                // If auto fallback disabled, return error immediately
                if (!autoFallback) {
                    return Result.Error("Error with ${model.name}: ${e.message}")
                }
                
                // If last model, return error
                if (index == enabledModels.size - 1) {
                    return Result.Error("All models failed. Last error: ${e.message}")
                }
                
                // Otherwise continue to next model
                Log.i(TAG, "🔄 Falling back to next model...")
            }
        }

        return Result.Error("No response from any model")
    }

    private suspend fun sendViaGroq(
        model: ModelConfig,
        userMessage: String,
        conversationHistory: List<Message>
    ): String? {
        val apiKey = model.apiKey.ifEmpty { prefsManager.getGroqApiKey() }
        
        if (apiKey.isEmpty()) {
            Log.w(TAG, "❌ No Groq API key configured for ${model.name}")
            throw Exception("No Groq API key configured")
        }

        Log.d(TAG, "📤 Sending to Groq: ${model.name} (${model.modelId})")

        val client = groqClients.getOrPut(model.id) {
            GroqClient(apiKey)
        }

        val response = client.sendMessage(userMessage, conversationHistory, model.modelId)
        Log.i(TAG, "✅ Success with Groq: ${model.name}")
        return response
    }

    private suspend fun sendViaOpenRouter(
        model: ModelConfig,
        userMessage: String,
        conversationHistory: List<Message>
    ): String? {
        val apiKey = model.apiKey.ifEmpty { prefsManager.getOpenRouterApiKey() }
        
        if (apiKey.isEmpty()) {
            Log.w(TAG, "❌ No OpenRouter API key configured for ${model.name}")
            throw Exception("No OpenRouter API key configured")
        }

        Log.d(TAG, "📤 Sending to OpenRouter: ${model.name} (${model.modelId})")

        val client = openRouterClients.getOrPut(model.id) {
            OpenRouterClient(apiKey)
        }

        val response = client.sendMessage(userMessage, conversationHistory, model.modelId)
        Log.i(TAG, "✅ Success with OpenRouter: ${model.name}")
        return response
    }

    /**
     * Result sealed class
     */
    sealed class Result {
        data class Success(val response: String, val model: ModelConfig) : Result()
        data class Error(val message: String) : Result()
    }
}
