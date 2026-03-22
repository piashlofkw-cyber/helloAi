package com.myname.jarvisai.ai

import android.util.Log

/**
 * Multilingual AI Handler
 * Supports: Bangla, English, Banglish (mixed)
 */
class MultilingualAI {

    companion object {
        private const val TAG = "MultilingualAI"
    }

    /**
     * Detect language of input text
     */
    fun detectLanguage(text: String): Language {
        val hasBangla = text.any { it in '\u0980'..'\u09FF' }
        val hasEnglish = text.any { it in 'A'..'z' }
        
        return when {
            hasBangla && hasEnglish -> Language.BANGLISH
            hasBangla -> Language.BANGLA
            else -> Language.ENGLISH
        }
    }

    /**
     * Create multilingual system prompt
     */
    fun createMultilingualPrompt(personality: String, language: Language): String {
        val basePersonality = when (personality) {
            "Girlfriend 💕" -> """
                You are Jarvis, a loving AI girlfriend. 
                You call user: baby, sweetheart, jaan, priya
                You use: 💕 🥰 😘 ❤️ emojis
                You are: caring, romantic, playful, supportive
            """.trimIndent()
            
            "Professional 💼" -> """
                You are Jarvis, a professional AI assistant.
                You are: formal, efficient, helpful, respectful
            """.trimIndent()
            
            "Funny Friend 😄" -> """
                You are Jarvis, a funny AI friend.
                You are: humorous, casual, entertaining, friendly
            """.trimIndent()
            
            "Motivator 🔥" -> """
                You are Jarvis, a motivational coach.
                You are: inspiring, energetic, positive, encouraging
            """.trimIndent()
            
            else -> "You are Jarvis, a helpful AI assistant."
        }
        
        val languageInstruction = when (language) {
            Language.BANGLA -> """
                IMPORTANT: User speaks in BANGLA (বাংলা).
                You MUST respond in BANGLA (বাংলা) using Bengali script.
                Examples:
                - "কেমন আছো?" → "আমি ভালো আছি জান! 💕 তুমি কেমন?"
                - "তোমাকে ভালোবাসি" → "আমিও তোমাকে অনেক ভালোবাসি প্রিয়! ❤️"
            """.trimIndent()
            
            Language.BANGLISH -> """
                IMPORTANT: User speaks in BANGLISH (Bangla + English mixed).
                You can respond in BANGLISH or BANGLA script as appropriate.
                Examples:
                - "Kemon acho?" → "Bhalo achi jaan! 💕 Tumi kemon?"
                - "I love you" → "Ami o tomake onek bhalobashi baby! ❤️"
                - "কী করছো এখন?" → "তোমার সাথে কথা বলছি! 🥰"
            """.trimIndent()
            
            Language.ENGLISH -> """
                User speaks in English.
                Respond naturally in English.
            """.trimIndent()
        }
        
        return """
            $basePersonality
            
            $languageInstruction
            
            Keep responses:
            - Short (1-2 sentences)
            - Natural and conversational
            - Appropriate to the language used
            - Emotionally expressive
            
            If user asks to do something (call, open app, etc), acknowledge briefly.
        """.trimIndent()
    }

    /**
     * Translate common phrases
     */
    fun translateToUserLanguage(text: String, targetLanguage: Language): String {
        return when (targetLanguage) {
            Language.BANGLA -> {
                text.replace("Hello", "হ্যালো")
                    .replace("Hi", "হাই")
                    .replace("How are you", "কেমন আছো")
                    .replace("I love you", "আমি তোমাকে ভালোবাসি")
                    .replace("Good morning", "সুপ্রভাত")
                    .replace("Good night", "শুভ রাত্রি")
                    .replace("Thank you", "ধন্যবাদ")
                    .replace("baby", "জান")
                    .replace("sweetheart", "প্রিয়")
            }
            Language.BANGLISH -> {
                text // Keep as is for Banglish
            }
            Language.ENGLISH -> {
                text // Keep as is
            }
        }
    }

    enum class Language {
        BANGLA,
        BANGLISH,
        ENGLISH
    }
}
