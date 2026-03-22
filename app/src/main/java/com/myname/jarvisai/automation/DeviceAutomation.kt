package com.myname.jarvisai.automation

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import java.net.URLEncoder

/**
 * Device Automation Handler
 * Controls apps, SMS, calls, settings via voice
 */
class DeviceAutomation(private val context: Context) {

    companion object {
        private const val TAG = "DeviceAutomation"
    }

    /**
     * Process voice command and execute action
     */
    fun processCommand(command: String): CommandResult {
        val lowerCommand = command.lowercase()
        Log.d(TAG, "📱 Processing command: $command")
        
        return when {
            // App launching
            lowerCommand.contains("open") || lowerCommand.contains("launch") -> {
                openApp(command)
            }
            
            // SMS
            lowerCommand.contains("send sms") || lowerCommand.contains("send message") -> {
                sendSMS(command)
            }
            
            // Call
            lowerCommand.contains("call") || lowerCommand.contains("dial") -> {
                makeCall(command)
            }
            
            // Facebook/Social Media
            lowerCommand.contains("facebook status") || lowerCommand.contains("fb status") -> {
                postFacebookStatus(command)
            }
            
            lowerCommand.contains("whatsapp") && lowerCommand.contains("send") -> {
                sendWhatsAppMessage(command)
            }
            
            // Settings
            lowerCommand.contains("wifi") -> toggleWifi(lowerCommand.contains("on"))
            lowerCommand.contains("bluetooth") -> toggleBluetooth(lowerCommand.contains("on"))
            lowerCommand.contains("flashlight") || lowerCommand.contains("torch") -> {
                CommandResult.NeedAccessibility("Flashlight control requires accessibility service")
            }
            
            // System
            lowerCommand.contains("screenshot") -> {
                CommandResult.NeedAccessibility("Screenshot requires accessibility service")
            }
            
            lowerCommand.contains("brightness") -> {
                adjustBrightness(command)
            }
            
            else -> CommandResult.NotAutomation
        }
    }

    private fun openApp(command: String): CommandResult {
        val appKeywords = mapOf(
            "whatsapp" to "com.whatsapp",
            "facebook" to "com.facebook.katana",
            "instagram" to "com.instagram.android",
            "twitter" to "com.twitter.android",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "maps" to "com.google.android.apps.maps",
            "camera" to "com.android.camera",
            "gallery" to "com.google.android.apps.photos",
            "settings" to "com.android.settings",
            "phone" to "com.android.dialer",
            "messages" to "com.google.android.apps.messaging"
        )
        
        val lowerCommand = command.lowercase()
        for ((keyword, packageName) in appKeywords) {
            if (lowerCommand.contains(keyword)) {
                return try {
                    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        CommandResult.Success("Opening $keyword")
                    } else {
                        CommandResult.Error("$keyword app not installed")
                    }
                } catch (e: Exception) {
                    CommandResult.Error("Failed to open $keyword: ${e.message}")
                }
            }
        }
        
        return CommandResult.Error("App not recognized")
    }

    private fun sendSMS(command: String): CommandResult {
        // Parse: "send sms to John saying Hello"
        // or "message John Hello how are you"
        
        return try {
            // For now, open SMS app with pre-filled message
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            CommandResult.Success("Opening SMS app. You can send message manually or enable accessibility for auto-send.")
        } catch (e: Exception) {
            CommandResult.Error("Failed to open SMS: ${e.message}")
        }
    }

    private fun makeCall(command: String): CommandResult {
        return try {
            // Open dialer
            val intent = Intent(Intent.ACTION_DIAL)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            CommandResult.Success("Opening dialer")
        } catch (e: Exception) {
            CommandResult.Error("Failed to open dialer: ${e.message}")
        }
    }

    private fun postFacebookStatus(command: String): CommandResult {
        // Extract status text from command
        val statusText = command.substringAfter("status", "")
            .replace("saying", "")
            .replace("post", "")
            .trim()
        
        return try {
            // Try Facebook app
            val fbIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, statusText)
                setPackage("com.facebook.katana")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            context.startActivity(fbIntent)
            CommandResult.Success("Opening Facebook to post status")
        } catch (e: Exception) {
            // Fallback to browser
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://www.facebook.com/")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
                CommandResult.Success("Opening Facebook in browser")
            } catch (e2: Exception) {
                CommandResult.Error("Failed to open Facebook")
            }
        }
    }

    private fun sendWhatsAppMessage(command: String): CommandResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            CommandResult.Success("Opening WhatsApp")
        } catch (e: Exception) {
            CommandResult.Error("WhatsApp not installed")
        }
    }

    private fun toggleWifi(enable: Boolean): CommandResult {
        return try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            CommandResult.Success("Opening WiFi settings")
        } catch (e: Exception) {
            CommandResult.Error("Failed to open WiFi settings")
        }
    }

    private fun toggleBluetooth(enable: Boolean): CommandResult {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            CommandResult.Success("Opening Bluetooth settings")
        } catch (e: Exception) {
            CommandResult.Error("Failed to open Bluetooth settings")
        }
    }

    private fun adjustBrightness(command: String): CommandResult {
        return try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            CommandResult.Success("Opening display settings")
        } catch (e: Exception) {
            CommandResult.Error("Failed to open settings")
        }
    }

    /**
     * Command execution result
     */
    sealed class CommandResult {
        data class Success(val message: String) : CommandResult()
        data class Error(val message: String) : CommandResult()
        data class NeedAccessibility(val message: String) : CommandResult()
        object NotAutomation : CommandResult()
    }
}
