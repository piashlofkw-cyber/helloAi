package com.myname.jarvisai.ai

import android.content.Context
import android.util.Log
import com.myname.jarvisai.automation.ContactManager
import com.myname.jarvisai.automation.DeviceAutomation
import com.myname.jarvisai.models.Message

/**
 * Smart Command Processor - Determines if command or conversation
 */
class CommandProcessor(private val context: Context) {

    private val deviceAutomation = DeviceAutomation(context)
    private val contactManager = ContactManager(context)
    
    companion object {
        private const val TAG = "CommandProcessor"
    }

    /**
     * Process user input and determine action type
     */
    suspend fun process(userInput: String): ProcessResult {
        val lowerInput = userInput.lowercase()
        
        // Check for automation commands first
        return when {
            // Contact operations
            isCallCommand(lowerInput) -> handleCallCommand(userInput)
            isSmsCommand(lowerInput) -> handleSmsCommand(userInput)
            isWhatsAppCommand(lowerInput) -> handleWhatsAppCommand(userInput)
            
            // Device automation
            isAppCommand(lowerInput) -> handleAppCommand(userInput)
            isSettingsCommand(lowerInput) -> handleSettingsCommand(userInput)
            
            // Camera/Vision
            isVisionCommand(lowerInput) -> ProcessResult.VisionRequest
            
            // Otherwise, it's a conversation
            else -> ProcessResult.Conversation(userInput)
        }
    }

    private fun isCallCommand(input: String): Boolean {
        return input.contains("call ") || 
               input.contains("dial ") ||
               input.contains("phone ")
    }

    private fun handleCallCommand(input: String): ProcessResult {
        // Extract contact name
        val name = extractContactName(input, listOf("call", "dial", "phone"))
        
        return if (name != null) {
            val result = contactManager.callContact(name)
            when (result) {
                is ContactManager.Result.Success -> ProcessResult.ActionCompleted(result.message)
                is ContactManager.Result.Error -> ProcessResult.ActionFailed(result.message)
            }
        } else {
            ProcessResult.ActionFailed("Who do you want to call?")
        }
    }

    private fun isSmsCommand(input: String): Boolean {
        return (input.contains("send sms") || 
                input.contains("send message") ||
                input.contains("text ")) &&
               !input.contains("whatsapp")
    }

    private fun handleSmsCommand(input: String): ProcessResult {
        val name = extractContactName(input, listOf("send sms to", "send message to", "text"))
        val message = extractMessage(input)
        
        return if (name != null) {
            val result = contactManager.sendSmsToContact(name, message)
            when (result) {
                is ContactManager.Result.Success -> ProcessResult.ActionCompleted(result.message)
                is ContactManager.Result.Error -> ProcessResult.ActionFailed(result.message)
            }
        } else {
            ProcessResult.ActionFailed("Who should I message?")
        }
    }

    private fun isWhatsAppCommand(input: String): Boolean {
        return input.contains("whatsapp") && 
               (input.contains("send") || input.contains("message"))
    }

    private fun handleWhatsAppCommand(input: String): ProcessResult {
        val name = extractContactName(input, listOf("whatsapp", "send to", "message"))
        val message = extractMessage(input)
        
        return if (name != null) {
            val result = contactManager.whatsappContact(name, message)
            when (result) {
                is ContactManager.Result.Success -> ProcessResult.ActionCompleted(result.message)
                is ContactManager.Result.Error -> ProcessResult.ActionFailed(result.message)
            }
        } else {
            ProcessResult.ActionFailed("Who should I WhatsApp?")
        }
    }

    private fun isAppCommand(input: String): Boolean {
        return input.contains("open ") || 
               input.contains("launch ") ||
               input.contains("start ")
    }

    private fun handleAppCommand(input: String): ProcessResult {
        val result = deviceAutomation.processCommand(input)
        return when (result) {
            is DeviceAutomation.CommandResult.Success -> ProcessResult.ActionCompleted(result.message)
            is DeviceAutomation.CommandResult.Error -> ProcessResult.ActionFailed(result.message)
            is DeviceAutomation.CommandResult.NeedAccessibility -> ProcessResult.ActionFailed(result.message)
            DeviceAutomation.CommandResult.NotAutomation -> ProcessResult.Conversation(input)
        }
    }

    private fun isSettingsCommand(input: String): Boolean {
        return input.contains("wifi") || 
               input.contains("bluetooth") ||
               input.contains("brightness") ||
               input.contains("volume")
    }

    private fun handleSettingsCommand(input: String): ProcessResult {
        val result = deviceAutomation.processCommand(input)
        return when (result) {
            is DeviceAutomation.CommandResult.Success -> ProcessResult.ActionCompleted(result.message)
            is DeviceAutomation.CommandResult.Error -> ProcessResult.ActionFailed(result.message)
            else -> ProcessResult.Conversation(input)
        }
    }

    private fun isVisionCommand(input: String): Boolean {
        return input.contains("what do you see") ||
               input.contains("analyze image") ||
               input.contains("look at") ||
               input.contains("describe this") ||
               input.contains("how do i look")
    }

    private fun extractContactName(input: String, keywords: List<String>): String? {
        var text = input.lowercase()
        
        // Remove keywords
        keywords.forEach { keyword ->
            text = text.replace(keyword, "")
        }
        
        // Remove common words
        text = text.replace("saying", "")
            .replace("that", "")
            .replace("please", "")
            .trim()
        
        // Get first word(s) as name
        val words = text.split(" ").filter { it.isNotBlank() }
        
        return if (words.isNotEmpty()) {
            words.take(2).joinToString(" ").capitalize()
        } else {
            null
        }
    }

    private fun extractMessage(input: String): String {
        val patterns = listOf("saying ", "that ", "message ")
        
        for (pattern in patterns) {
            if (input.lowercase().contains(pattern)) {
                return input.substringAfter(pattern, "").trim()
            }
        }
        
        return ""
    }

    private fun String.capitalize(): String {
        return split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    /**
     * Processing result types
     */
    sealed class ProcessResult {
        data class Conversation(val message: String) : ProcessResult()
        data class ActionCompleted(val message: String) : ProcessResult()
        data class ActionFailed(val message: String) : ProcessResult()
        object VisionRequest : ProcessResult()
    }
}
