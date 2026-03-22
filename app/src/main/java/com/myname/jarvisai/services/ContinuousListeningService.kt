package com.myname.jarvisai.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.myname.jarvisai.R
import com.myname.jarvisai.ui.MainActivity
import kotlinx.coroutines.*

/**
 * Background service for continuous voice listening
 * Like having a girlfriend always ready to chat!
 */
class ContinuousListeningService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    companion object {
        private const val CHANNEL_ID = "jarvis_listening"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "START_LISTENING"
        const val ACTION_STOP = "STOP_LISTENING"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupRecognitionListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startContinuousListening()
            ACTION_STOP -> stopContinuousListening()
        }
        return START_STICKY
    }

    private fun startContinuousListening() {
        if (!isListening) {
            isListening = true
            startForeground(NOTIFICATION_ID, createNotification("🎤 Jarvis is listening..."))
            startListening()
        }
    }

    private fun stopContinuousListening() {
        isListening = false
        speechRecognizer?.cancel()
        stopForeground(true)
        stopSelf()
    }

    private fun startListening() {
        if (!isListening) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            // Retry after delay
            scope.launch {
                delay(1000)
                startListening()
            }
        }
    }

    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                updateNotification("🎤 Listening...")
            }

            override fun onBeginningOfSpeech() {
                updateNotification("👂 Hearing you...")
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                // Auto-restart listening
                scope.launch {
                    delay(500)
                    startListening()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val userInput = matches[0]
                    
                    // Broadcast to MainActivity
                    val intent = Intent("com.myname.jarvisai.VOICE_INPUT")
                    intent.putExtra("text", userInput)
                    sendBroadcast(intent)
                }
                
                // Continue listening
                scope.launch {
                    delay(300)
                    startListening()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jarvis Listening Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous voice listening"
                setShowBadge(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis AI")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(text))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isListening = false
        speechRecognizer?.destroy()
        scope.cancel()
    }
}
