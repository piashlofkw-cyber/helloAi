package com.myname.jarvisai.utils

import android.content.Context
import android.content.SharedPreferences
import com.myname.jarvisai.models.ModelConfig

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "jarvis_prefs"
        
        private const val KEY_GROQ_API_KEY = "groq_api_key"
        private const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"
        private const val KEY_ELEVENLABS_API_KEY = "elevenlabs_api_key"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_WAKE_WORD = "wake_word"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_AI_MODEL = "ai_model"
        private const val KEY_MODEL_LIST = "model_list"
        private const val KEY_AUTO_FALLBACK = "auto_fallback_enabled"
        
        private const val DEFAULT_WAKE_WORD = "Hey Jarvis"
        const val PROVIDER_GROQ = "Groq"
        const val PROVIDER_OPENROUTER = "OpenRouter"
        const val DEFAULT_GROQ_MODEL = "mixtral-8x7b-32768"
        const val DEFAULT_OPENROUTER_MODEL = "openai/gpt-3.5-turbo"
    }

    // Groq API Key
    fun setGroqApiKey(key: String) {
        prefs.edit().putString(KEY_GROQ_API_KEY, key).apply()
    }

    fun getGroqApiKey(): String {
        return prefs.getString(KEY_GROQ_API_KEY, "") ?: ""
    }

    // OpenRouter API Key
    fun setOpenRouterApiKey(key: String) {
        prefs.edit().putString(KEY_OPENROUTER_API_KEY, key).apply()
    }

    fun getOpenRouterApiKey(): String {
        return prefs.getString(KEY_OPENROUTER_API_KEY, "") ?: ""
    }

    // ElevenLabs API Key
    fun setElevenLabsApiKey(key: String) {
        prefs.edit().putString(KEY_ELEVENLABS_API_KEY, key).apply()
    }

    fun getElevenLabsApiKey(): String {
        return prefs.getString(KEY_ELEVENLABS_API_KEY, "") ?: ""
    }

    // Wake Word Settings
    fun setWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
    }

    fun isWakeWordEnabled(): Boolean {
        return prefs.getBoolean(KEY_WAKE_WORD_ENABLED, false)
    }

    fun setWakeWord(wakeWord: String) {
        prefs.edit().putString(KEY_WAKE_WORD, wakeWord).apply()
    }

    fun getWakeWord(): String {
        return prefs.getString(KEY_WAKE_WORD, DEFAULT_WAKE_WORD) ?: DEFAULT_WAKE_WORD
    }

    // AI Provider
    fun setAiProvider(provider: String) {
        prefs.edit().putString(KEY_AI_PROVIDER, provider).apply()
    }

    fun getAiProvider(): String {
        return prefs.getString(KEY_AI_PROVIDER, PROVIDER_GROQ) ?: PROVIDER_GROQ
    }

    // AI Model
    fun setAiModel(model: String) {
        prefs.edit().putString(KEY_AI_MODEL, model).apply()
    }

    fun getAiModel(): String {
        return prefs.getString(KEY_AI_MODEL, DEFAULT_GROQ_MODEL) ?: DEFAULT_GROQ_MODEL
    }

    // Model List Management
    fun saveModelList(models: List<ModelConfig>) {
        val json = ModelConfig.listToJson(models)
        prefs.edit().putString(KEY_MODEL_LIST, json).apply()
    }

    fun getModelList(): List<ModelConfig> {
        val json = prefs.getString(KEY_MODEL_LIST, null)
        return if (json != null) {
            ModelConfig.jsonToList(json)
        } else {
            // First time - return default free models
            val defaults = ModelConfig.getDefaultFreeModels()
            saveModelList(defaults)
            defaults
        }
    }

    fun addModel(model: ModelConfig) {
        val currentList = getModelList().toMutableList()
        currentList.add(model)
        saveModelList(currentList)
    }

    fun updateModel(modelId: String, updatedModel: ModelConfig) {
        val currentList = getModelList().toMutableList()
        val index = currentList.indexOfFirst { it.id == modelId }
        if (index >= 0) {
            currentList[index] = updatedModel
            saveModelList(currentList)
        }
    }

    fun deleteModel(modelId: String) {
        val currentList = getModelList().toMutableList()
        currentList.removeAll { it.id == modelId }
        saveModelList(currentList)
    }

    // Auto Fallback
    fun setAutoFallbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_FALLBACK, enabled).apply()
    }

    fun isAutoFallbackEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_FALLBACK, true)
    }

    // Get enabled models sorted by priority
    fun getEnabledModelsByPriority(): List<ModelConfig> {
        return getModelList()
            .filter { it.enabled }
            .sortedBy { it.priority }
    }

    // Clear all preferences
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
