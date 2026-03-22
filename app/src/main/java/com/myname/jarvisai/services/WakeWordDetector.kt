package com.myname.jarvisai.services

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.*

/**
 * Wake Word Detector - Listens for "Hey Jarvis" continuously
 */
class WakeWordDetector(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var wakeWords = listOf("hey jarvis", "jarvis", "ok jarvis")
    
    companion object {
        private const val TAG = "WakeWordDetector"
        private const val RESTART_DELAY = 500L
    }

    fun start() {
        if (isListening) return
        
        isListening = true
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        setupRecognitionListener()
        startListening()
        
        Log.d(TAG, "Wake word detection started")
    }

    fun stop() {
        isListening = false
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        scope.cancel()
        
        Log.d(TAG, "Wake word detection stopped")
    }

    fun updateWakeWords(words: List<String>) {
        wakeWords = words.map { it.lowercase().trim() }
    }

    private fun startListening() {
        if (!isListening) return
        
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000)
        }
        
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting listening: ${e.message}")
            restartListening()
        }
    }

    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                Log.d(TAG, "Recognition error: $error")
                restartListening()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0].lowercase()
                    Log.d(TAG, "Heard: $spokenText")
                    
                    // Check if wake word detected
                    if (wakeWords.any { spokenText.contains(it) }) {
                        Log.i(TAG, "🎯 Wake word detected!")
                        onWakeWordDetected()
                        
                        // Pause briefly after detection
                        scope.launch {
                            delay(2000)
                            startListening()
                        }
                        return
                    }
                }
                
                // Continue listening
                restartListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                
                if (!matches.isNullOrEmpty()) {
                    val partial = matches[0].lowercase()
                    
                    // Quick wake word check on partial results
                    if (wakeWords.any { partial.contains(it) }) {
                        Log.i(TAG, "🎯 Wake word detected (partial)!")
                        onWakeWordDetected()
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun restartListening() {
        if (!isListening) return
        
        scope.launch {
            delay(RESTART_DELAY)
            startListening()
        }
    }
}
