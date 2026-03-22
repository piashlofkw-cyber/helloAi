package com.myname.jarvisai.ai

import android.content.Context
import android.util.Log
import com.myname.jarvisai.models.Message
import com.myname.jarvisai.models.ModelConfig
import com.myname.jarvisai.models.PersonalityMode
import com.myname.jarvisai.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Streaming AI Manager - Live typing effect like chatting with girlfriend
 */
class StreamingAIManager(private val context: Context) {

    private val prefsManager = PreferencesManager(context)
    private val aiManager = AIManager(context)
    private val conversationHistory = mutableListOf<Message>()
    
    companion object {
        private const val TAG = "StreamingAI"
        private const val MAX_HISTORY = 10 // Keep last 10 messages for context
    }

    /**
     * Send message with streaming response (live typing effect)
     */
    fun sendMessageStreaming(userMessage: String): Flow<StreamResult> = flow {
        try {
            // Add user message to history
            conversationHistory.add(Message(role = "user", content = userMessage))
            
            // Trim history
            if (conversationHistory.size > MAX_HISTORY * 2) {
                conversationHistory.removeAt(0)
                conversationHistory.removeAt(0)
            }
            
            emit(StreamResult.Thinking)
            
            // Get personality mode
            val personality = getPersonalityMode()
            
            // Add personality to conversation
            val messages = mutableListOf(
                Message(role = "system", content = personality.systemPrompt)
            )
            messages.addAll(conversationHistory)
            
            // Get response from AI
            val result = withContext(Dispatchers.IO) {
                aiManager.sendMessage(userMessage, conversationHistory)
            }
            
            when (result) {
                is AIManager.Result.Success -> {
                    val response = result.response
                    val model = result.model
                    
                    // Add to history
                    conversationHistory.add(Message(role = "assistant", content = response))
                    
                    // Emit model info
                    emit(StreamResult.ModelInfo(model.name))
                    
                    // Stream response character by character (live typing effect)
                    val words = response.split(" ")
                    val streamedText = StringBuilder()
                    
                    for (word in words) {
                        streamedText.append(word).append(" ")
                        emit(StreamResult.Chunk(streamedText.toString().trim()))
                        delay(100) // Typing speed effect
                    }
                    
                    emit(StreamResult.Complete(response, model))
                }
                is AIManager.Result.Error -> {
                    emit(StreamResult.Error(result.message))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Streaming error: ${e.message}")
            emit(StreamResult.Error(e.message ?: "Unknown error"))
        }
    }

    /**
     * Get current personality mode
     */
    private fun getPersonalityMode(): PersonalityMode {
        val modeName = prefsManager.getPersonalityMode()
        return PersonalityMode.values().find { it.displayName == modeName } 
            ?: PersonalityMode.GIRLFRIEND
    }

    /**
     * Clear conversation history
     */
    fun clearHistory() {
        conversationHistory.clear()
    }

    /**
     * Get conversation history
     */
    fun getHistory(): List<Message> = conversationHistory.toList()

    /**
     * Streaming result states
     */
    sealed class StreamResult {
        object Thinking : StreamResult()
        data class ModelInfo(val modelName: String) : StreamResult()
        data class Chunk(val text: String) : StreamResult()
        data class Complete(val fullText: String, val model: ModelConfig) : StreamResult()
        data class Error(val message: String) : StreamResult()
    }
}
