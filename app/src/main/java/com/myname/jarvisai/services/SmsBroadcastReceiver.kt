package com.myname.jarvisai.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast

class SmsBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "JarvisSmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                return
            }

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (smsMessage in messages) {
                val sender = smsMessage.displayOriginatingAddress
                val messageBody = smsMessage.messageBody
                val timestamp = smsMessage.timestampMillis

                Log.d(TAG, "SMS from $sender: $messageBody")

                // Process the SMS with AI
                processSms(context, sender, messageBody, timestamp)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error receiving SMS", e)
        }
    }

    private fun processSms(
        context: Context,
        sender: String,
        message: String,
        timestamp: Long
    ) {
        // Detect spam SMS
        if (isSpamSms(message)) {
            Log.w(TAG, "Spam SMS detected from $sender")
            // You can optionally block or move to spam folder
            return
        }

        // Detect important keywords (OTP, verification, etc.)
        if (isOtpMessage(message)) {
            val otp = extractOtp(message)
            Log.i(TAG, "OTP detected: $otp")
            // You can auto-fill OTP here or notify the user
        }

        // Check if message requires urgent attention
        if (isUrgentMessage(message)) {
            Log.w(TAG, "Urgent message from $sender")
            // Send notification or alert
            Toast.makeText(
                context,
                "Urgent message from $sender",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun isSpamSms(message: String): Boolean {
        val spamKeywords = listOf(
            "congratulations you won",
            "click here to claim",
            "free prize",
            "act now",
            "limited time offer",
            "verify your account immediately"
        )

        return spamKeywords.any { keyword ->
            message.lowercase().contains(keyword)
        }
    }

    private fun isOtpMessage(message: String): Boolean {
        val otpPatterns = listOf(
            "\\b\\d{4,6}\\b",  // 4-6 digit code
            "OTP",
            "verification code",
            "one time password"
        )

        return otpPatterns.any { pattern ->
            message.contains(Regex(pattern, RegexOption.IGNORE_CASE))
        }
    }

    private fun extractOtp(message: String): String? {
        val otpRegex = Regex("\\b(\\d{4,6})\\b")
        return otpRegex.find(message)?.value
    }

    private fun isUrgentMessage(message: String): Boolean {
        val urgentKeywords = listOf(
            "urgent",
            "emergency",
            "important",
            "asap",
            "immediately"
        )

        return urgentKeywords.any { keyword ->
            message.lowercase().contains(keyword)
        }
    }
}
