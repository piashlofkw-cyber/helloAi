package com.myname.jarvisai.models

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * AI Model Configuration
 */
data class ModelConfig(
    val id: String,                    // Unique ID
    val name: String,                  // Display name
    val modelId: String,               // API model ID
    val provider: String,              // "groq", "openrouter", "custom"
    val apiKey: String = "",           // API key (optional for some providers)
    val baseUrl: String = "",          // Custom base URL (optional)
    val priority: Int = 0,             // Lower number = higher priority (for fallback)
    val enabled: Boolean = true,       // Is this model active?
    val isFree: Boolean = false,       // Is this a free model?
    val maxTokens: Int = 1024,         // Max response tokens
    val temperature: Float = 0.7f,     // Response randomness
    val description: String = ""       // Model description
) {
    companion object {
        // Serialize list to JSON
        fun listToJson(models: List<ModelConfig>): String {
            return Gson().toJson(models)
        }

        // Deserialize JSON to list
        fun jsonToList(json: String): List<ModelConfig> {
            val type = object : TypeToken<List<ModelConfig>>() {}.type
            return try {
                Gson().fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Get default free models
        fun getDefaultFreeModels(): List<ModelConfig> {
            return listOf(
                // Groq Models (100% FREE)
                ModelConfig(
                    id = "groq_mixtral",
                    name = "Mixtral 8x7B",
                    modelId = "mixtral-8x7b-32768",
                    provider = "groq",
                    priority = 1,
                    isFree = true,
                    description = "Fast, FREE, 32K context - Best for general tasks"
                ),
                ModelConfig(
                    id = "groq_llama3_70b",
                    name = "Llama 3 70B",
                    modelId = "llama3-70b-8192",
                    provider = "groq",
                    priority = 2,
                    isFree = true,
                    description = "FREE, Powerful reasoning, 8K context"
                ),
                ModelConfig(
                    id = "groq_llama3_8b",
                    name = "Llama 3 8B",
                    modelId = "llama3-8b-8192",
                    provider = "groq",
                    priority = 3,
                    isFree = true,
                    description = "FREE, Very fast, 8K context"
                ),
                ModelConfig(
                    id = "groq_gemma_7b",
                    name = "Gemma 7B",
                    modelId = "gemma-7b-it",
                    provider = "groq",
                    priority = 4,
                    isFree = true,
                    description = "FREE, Google model, balanced"
                ),
                
                // OpenRouter Free Models
                ModelConfig(
                    id = "or_llama3_8b_free",
                    name = "Llama 3 8B (OR Free)",
                    modelId = "meta-llama/llama-3-8b-instruct:free",
                    provider = "openrouter",
                    priority = 5,
                    isFree = true,
                    description = "FREE on OpenRouter"
                ),
                ModelConfig(
                    id = "or_mistral_7b_free",
                    name = "Mistral 7B (OR Free)",
                    modelId = "mistralai/mistral-7b-instruct:free",
                    provider = "openrouter",
                    priority = 6,
                    isFree = true,
                    description = "FREE on OpenRouter"
                ),
                ModelConfig(
                    id = "or_phi3_free",
                    name = "Phi-3 Medium (OR Free)",
                    modelId = "microsoft/phi-3-medium-128k-instruct:free",
                    provider = "openrouter",
                    priority = 7,
                    isFree = true,
                    description = "FREE, Microsoft, 128K context"
                ),
                ModelConfig(
                    id = "or_gemma_7b_free",
                    name = "Gemma 7B (OR Free)",
                    modelId = "google/gemma-7b-it:free",
                    provider = "openrouter",
                    priority = 8,
                    isFree = true,
                    description = "FREE, Google model"
                ),
                
                // Premium OpenRouter Models
                ModelConfig(
                    id = "or_gpt35",
                    name = "GPT-3.5 Turbo",
                    modelId = "openai/gpt-3.5-turbo",
                    provider = "openrouter",
                    priority = 10,
                    isFree = false,
                    description = "Fast, cheap, good quality"
                ),
                ModelConfig(
                    id = "or_gpt4",
                    name = "GPT-4 Turbo",
                    modelId = "openai/gpt-4-turbo",
                    provider = "openrouter",
                    priority = 20,
                    isFree = false,
                    description = "Best quality, expensive"
                ),
                ModelConfig(
                    id = "or_claude_haiku",
                    name = "Claude 3 Haiku",
                    modelId = "anthropic/claude-3-haiku",
                    provider = "openrouter",
                    priority = 11,
                    isFree = false,
                    description = "Fast, cheap, smart"
                ),
                ModelConfig(
                    id = "or_claude_sonnet",
                    name = "Claude 3.5 Sonnet",
                    modelId = "anthropic/claude-3.5-sonnet",
                    provider = "openrouter",
                    priority = 21,
                    isFree = false,
                    description = "Best reasoning, balanced cost"
                ),
                ModelConfig(
                    id = "or_gemini_pro",
                    name = "Gemini Pro",
                    modelId = "google/gemini-pro",
                    provider = "openrouter",
                    priority = 12,
                    isFree = false,
                    description = "Google's best, good price"
                )
            )
        }
    }
}
