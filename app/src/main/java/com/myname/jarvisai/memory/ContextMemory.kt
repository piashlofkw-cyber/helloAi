package com.myname.jarvisai.memory

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.myname.jarvisai.models.ChatMessage
import java.io.File

/**
 * Context Memory System - Remembers everything like a real girlfriend!
 */
class ContextMemory(private val context: Context) {

    private val conversationFile: File = File(context.filesDir, "conversations.json")
    private val userProfileFile: File = File(context.filesDir, "user_profile.json")
    private val gson = Gson()
    
    companion object {
        private const val MAX_MESSAGES = 500
    }

    /**
     * Save conversation message
     */
    fun saveMessage(message: ChatMessage) {
        val messages = loadAllMessages().toMutableList()
        messages.add(message)
        
        // Keep only last MAX_MESSAGES
        if (messages.size > MAX_MESSAGES) {
            messages.removeAt(0)
        }
        
        conversationFile.writeText(gson.toJson(messages))
    }

    /**
     * Load all conversation messages
     */
    fun loadAllMessages(): List<ChatMessage> {
        return if (conversationFile.exists()) {
            try {
                val json = conversationFile.readText()
                val type = object : TypeToken<List<ChatMessage>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * Get recent messages for context
     */
    fun getRecentMessages(count: Int = 20): List<ChatMessage> {
        return loadAllMessages().takeLast(count)
    }

    /**
     * Clear all conversations
     */
    fun clearConversations() {
        conversationFile.delete()
    }

    /**
     * Save user profile information
     */
    fun saveUserProfile(profile: UserProfile) {
        userProfileFile.writeText(gson.toJson(profile))
    }

    /**
     * Load user profile
     */
    fun loadUserProfile(): UserProfile {
        return if (userProfileFile.exists()) {
            try {
                val json = userProfileFile.readText()
                gson.fromJson(json, UserProfile::class.java) ?: UserProfile()
            } catch (e: Exception) {
                UserProfile()
            }
        } else {
            UserProfile()
        }
    }

    /**
     * Learn from conversation
     */
    fun learnFromMessage(message: String, isUser: Boolean) {
        if (!isUser) return
        
        val profile = loadUserProfile()
        
        // Extract information
        val lowerMessage = message.lowercase()
        
        // Learn name
        if (lowerMessage.contains("my name is") || lowerMessage.contains("i am")) {
            val name = extractName(lowerMessage)
            if (name != null) {
                profile.name = name
            }
        }
        
        // Learn preferences
        if (lowerMessage.contains("i like") || lowerMessage.contains("i love")) {
            val preference = lowerMessage.substringAfter("like").substringAfter("love").trim()
            if (!profile.likes.contains(preference)) {
                profile.likes.add(preference)
            }
        }
        
        if (lowerMessage.contains("i hate") || lowerMessage.contains("i don't like")) {
            val dislike = lowerMessage.substringAfter("hate").substringAfter("don't like").trim()
            if (!profile.dislikes.contains(dislike)) {
                profile.dislikes.add(dislike)
            }
        }
        
        // Learn routines
        if (lowerMessage.contains("every day") || lowerMessage.contains("usually")) {
            profile.routines.add(message)
        }
        
        // Update conversation count
        profile.totalConversations++
        
        saveUserProfile(profile)
    }

    private fun extractName(message: String): String? {
        val patterns = listOf("my name is ", "i am ", "i'm ", "call me ")
        
        for (pattern in patterns) {
            if (message.contains(pattern)) {
                return message.substringAfter(pattern)
                    .split(" ")[0]
                    .trim()
                    .capitalize()
            }
        }
        
        return null
    }

    /**
     * Get personalized greeting
     */
    fun getPersonalizedGreeting(): String {
        val profile = loadUserProfile()
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        
        val greeting = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Hey"
        }
        
        return if (profile.name.isNotEmpty()) {
            "$greeting ${profile.name}! 💕"
        } else {
            "$greeting baby! 💕"
        }
    }

    /**
     * User profile data
     */
    data class UserProfile(
        var name: String = "",
        val likes: MutableList<String> = mutableListOf(),
        val dislikes: MutableList<String> = mutableListOf(),
        val routines: MutableList<String> = mutableListOf(),
        var favoriteColor: String = "",
        var birthday: String = "",
        var totalConversations: Int = 0,
        var firstMetDate: Long = System.currentTimeMillis()
    )
}
