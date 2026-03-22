package com.myname.jarvisai.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.speech.tts.TextToSpeech
import android.telephony.SmsMessage
import android.util.Log
import java.util.Locale

/**
 * SMS Reader - Reads incoming SMS aloud
 */
class SmsReaderService : BroadcastReceiver() {

    private var tts: TextToSpeech? = null
    
    companion object {
        private const val TAG = "SmsReaderService"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val bundle = intent.extras
            if (bundle != null) {
                try {
                    val pdus = bundle.get("pdus") as Array<*>
                    val messages = pdus.map { pdu ->
                        SmsMessage.createFromPdu(pdu as ByteArray, bundle.getString("format"))
                    }
                    
                    messages.forEach { sms ->
                        val sender = sms.displayOriginatingAddress
                        val messageBody = sms.messageBody
                        
                        Log.d(TAG, "SMS from $sender: $messageBody")
                        
                        // Check if SMS reading is enabled in preferences
                        val prefs = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
                        val readSmsEnabled = prefs.getBoolean("read_sms_enabled", false)
                        
                        if (readSmsEnabled) {
                            readSmsAloud(context, sender, messageBody)
                        }
                        
                        // Broadcast to app for AI processing
                        val aiIntent = Intent("com.myname.jarvisai.NEW_SMS")
                        aiIntent.putExtra("sender", sender)
                        aiIntent.putExtra("message", messageBody)
                        context.sendBroadcast(aiIntent)
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading SMS: ${e.message}")
                }
            }
        }
    }

    private fun readSmsAloud(context: Context, sender: String, message: String) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                
                val announcement = "New message from $sender. $message"
                tts?.speak(announcement, TextToSpeech.QUEUE_ADD, null, null)
                
                // Stop after speaking
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    tts?.shutdown()
                }, 10000)
            }
        }
    }
}
