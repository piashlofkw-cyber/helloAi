package com.myname.jarvisai.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.myname.jarvisai.R
import com.myname.jarvisai.ai.AIManager
import com.myname.jarvisai.ai.AvatarManager
import com.myname.jarvisai.ai.ElevenLabsClient
import com.myname.jarvisai.databinding.ActivityMainBinding
import com.myname.jarvisai.models.AssistantState
import com.myname.jarvisai.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var avatarManager: AvatarManager
    private lateinit var aiManager: AIManager
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var elevenLabsClient: ElevenLabsClient? = null
    private var cartesiaClient: com.myname.jarvisai.ai.CartesiaClient? = null
    private var wakeWordDetector: com.myname.jarvisai.services.WakeWordDetector? = null
    private var commandProcessor: com.myname.jarvisai.ai.CommandProcessor? = null
    
    private var currentState = AssistantState.IDLE
    private var isContinuousMode = false

    companion object {
        private const val REQUEST_PERMISSIONS = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)
        avatarManager = AvatarManager(this)
        aiManager = AIManager(this)

        initializeUI()
        checkPermissions()
        initializeAI()
    }

    private fun initializeUI() {
        binding.apply {
            btnSettings.setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }

            micButton.setOnClickListener {
                when (currentState) {
                    AssistantState.IDLE -> startListening()
                    AssistantState.LISTENING -> stopListening()
                    else -> {}
                }
            }

            // Start avatar animation
            avatarAnimation.playAnimation()
            val pulseAnim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.pulse)
            glowEffect.startAnimation(pulseAnim)
        }

        updateUI(AssistantState.IDLE)
    }

    private fun checkPermissions() {
        val permissionsNeeded = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                REQUEST_PERMISSIONS
            )
        }
    }

    private fun initializeAI() {
        lifecycleScope.launch {
            try {
                val voiceProvider = prefsManager.getVoiceProvider()
                val cartesiaKey = prefsManager.getCartesiaApiKey()
                val cartesiaVoiceId = prefsManager.getCartesiaVoiceId()
                val elevenLabsKey = prefsManager.getElevenLabsApiKey()
                
                Log.d("MainActivity", "🎤 Initializing Voice System...")
                Log.d("MainActivity", "   Voice Provider: $voiceProvider")
                Log.d("MainActivity", "   Cartesia Key: ${if (cartesiaKey.isEmpty()) "EMPTY" else "Present (${cartesiaKey.length} chars)"}")
                Log.d("MainActivity", "   Cartesia Voice: ${if (cartesiaVoiceId.isEmpty()) "EMPTY" else cartesiaVoiceId}")

                // Initialize Cartesia if key present
                if (cartesiaKey.isNotEmpty() && cartesiaVoiceId.isNotEmpty()) {
                    try {
                        cartesiaClient = com.myname.jarvisai.ai.CartesiaClient(cartesiaKey, cartesiaVoiceId)
                        Log.i("MainActivity", "✅ Cartesia AI initialized!")
                        Log.i("MainActivity", "   Voice ID: $cartesiaVoiceId")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "❌ Cartesia init error: ${e.message}")
                    }
                } else {
                    Log.w("MainActivity", "⚠️ Cartesia not initialized (missing key or voice ID)")
                }

                // Initialize ElevenLabs if key present  
                if (elevenLabsKey.isNotEmpty()) {
                    try {
                        elevenLabsClient = ElevenLabsClient(elevenLabsKey)
                        Log.i("MainActivity", "✅ ElevenLabs initialized!")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "❌ ElevenLabs init error: ${e.message}")
                    }
                }
                
                Log.i("MainActivity", "🔊 Active Voice Provider: $voiceProvider")
                if (voiceProvider == "cartesia" && cartesiaClient == null) {
                    Log.e("MainActivity", "⚠️ WARNING: Cartesia selected but not initialized!")
                }

                // Load avatar
                avatarManager.loadAvatar(binding.avatarImage, "neutral")

                // Show configured models count
                val modelCount = prefsManager.getEnabledModelsByPriority().size
                val voiceStatus = when {
                    voiceProvider == "cartesia" && cartesiaClient != null -> "Cartesia Voice Ready! ⚡"
                    voiceProvider == "elevenlabs" && elevenLabsClient != null -> "ElevenLabs Voice Ready!"
                    else -> "Android TTS"
                }
                
                if (modelCount > 0) {
                    Toast.makeText(
                        this@MainActivity,
                        "$modelCount AI model(s) | $voiceStatus",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error initializing AI: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Error initializing AI", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startContinuousListening() {
        wakeWordDetector = com.myname.jarvisai.services.WakeWordDetector(this) {
            // Wake word detected - start full listening
            runOnUiThread {
                Toast.makeText(this, "🎤 Listening...", Toast.LENGTH_SHORT).show()
                startListening()
            }
        }
        wakeWordDetector?.start()
        
        Toast.makeText(this, "🎤 Always listening mode ON", Toast.LENGTH_LONG).show()
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                updateUI(AssistantState.LISTENING)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                updateUI(AssistantState.ERROR)
                Toast.makeText(this@MainActivity, "Error: $error", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val userInput = matches[0]
                    Log.d("MainActivity", "🎤 Heard: $userInput")
                    processUserInput(userInput)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            
            // Multilingual support - Bangla + English + Banglish
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD") // Bengali (Bangladesh)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            
            // Also support English fallback
            val languages = arrayListOf("bn-BD", "en-US", "en-IN")
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, languages)
            
            // Enable better recognition for mixed languages
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            
            Log.d("MainActivity", "🎤 Speech Recognition configured for Bangla/English/Banglish")
        }

        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        updateUI(AssistantState.IDLE)
    }

    private fun processUserInput(userMessage: String) {
        updateUI(AssistantState.PROCESSING)
        binding.responseText.text = "You: $userMessage"

        lifecycleScope.launch {
            try {
                // First, check if it's a command or conversation
                val commandResult = withContext(Dispatchers.IO) {
                    commandProcessor?.process(userMessage)
                }

                when (commandResult) {
                    is com.myname.jarvisai.ai.CommandProcessor.ProcessResult.ActionCompleted -> {
                        // Command executed successfully
                        val response = commandResult.message
                        binding.responseText.append("\n\nJarvis: $response ✅")
                        speakResponse(response)
                        updateUI(AssistantState.IDLE)
                    }
                    
                    is com.myname.jarvisai.ai.CommandProcessor.ProcessResult.ActionFailed -> {
                        // Command failed
                        val response = commandResult.message
                        binding.responseText.append("\n\nJarvis: $response ❌")
                        speakResponse(response)
                        updateUI(AssistantState.ERROR)
                    }
                    
                    is com.myname.jarvisai.ai.CommandProcessor.ProcessResult.VisionRequest -> {
                        // Vision request - would open camera
                        val response = "Please take a photo first baby, then I can see it 💕"
                        binding.responseText.append("\n\nJarvis: $response")
                        speakResponse(response)
                        updateUI(AssistantState.IDLE)
                    }
                    
                    is com.myname.jarvisai.ai.CommandProcessor.ProcessResult.Conversation -> {
                        // Regular conversation - send to AI
                        val result = withContext(Dispatchers.IO) {
                            aiManager.sendMessage(userMessage)
                        }

                        when (result) {
                            is AIManager.Result.Success -> {
                                val response = result.response
                                val modelUsed = result.model.name
                                
                                binding.responseText.append("\n\nJarvis ($modelUsed): $response")
                                
                                // Speak the response
                                speakResponse(response)
                                
                                // Update avatar mood
                                avatarManager.loadAvatar(binding.avatarImage, "speaking")
                            }
                            is AIManager.Result.Error -> {
                                binding.responseText.append("\n\nError: ${result.message}")
                                Toast.makeText(
                                    this@MainActivity,
                                    "AI Error: ${result.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                updateUI(AssistantState.ERROR)
                            }
                        }
                    }
                    
                    else -> {
                        // Fallback to conversation
                        val result = withContext(Dispatchers.IO) {
                            aiManager.sendMessage(userMessage)
                        }

                        when (result) {
                            is AIManager.Result.Success -> {
                                binding.responseText.append("\n\nJarvis: ${result.response}")
                                speakResponse(result.response)
                            }
                            is AIManager.Result.Error -> {
                                binding.responseText.append("\n\nError: ${result.message}")
                                updateUI(AssistantState.ERROR)
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                binding.responseText.text = "Error: ${e.message}"
                updateUI(AssistantState.ERROR)
            }
        }
    }

    private var tts: android.speech.tts.TextToSpeech? = null
    
    private fun speakResponse(text: String) {
        lifecycleScope.launch {
            updateUI(AssistantState.SPEAKING)
            
            try {
                val voiceProvider = prefsManager.getVoiceProvider()
                
                Log.d("MainActivity", "🔊 Speaking with: $voiceProvider")
                Log.d("MainActivity", "   Text: ${text.take(50)}...")
                Log.d("MainActivity", "   Cartesia client: ${if (cartesiaClient != null) "READY ✅" else "NULL ❌"}")
                
                when (voiceProvider) {
                    "cartesia" -> {
                        if (cartesiaClient == null) {
                            Log.e("MainActivity", "❌ Cartesia client is NULL! Using TTS fallback")
                            speakWithAndroidTTS(text)
                        } else {
                            Log.d("MainActivity", "📡 Calling Cartesia API...")
                            val audioBytes = withContext(Dispatchers.IO) {
                                try {
                                    cartesiaClient?.textToSpeech(text)
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "❌ Cartesia API error: ${e.message}", e)
                                    null
                                }
                            }
                            
                            if (audioBytes != null && audioBytes.isNotEmpty()) {
                                Log.i("MainActivity", "✅ Got ${audioBytes.size} bytes, playing PCM audio...")
                                playPCMAudio(audioBytes)
                            } else {
                                Log.w("MainActivity", "❌ Cartesia returned null/empty, using TTS")
                                speakWithAndroidTTS(text)
                            }
                        }
                    }
                    "elevenlabs" -> {
                        if (elevenLabsClient == null) {
                            Log.e("MainActivity", "❌ ElevenLabs client is NULL! Using TTS fallback")
                            speakWithAndroidTTS(text)
                        } else {
                            val audioBytes = withContext(Dispatchers.IO) {
                                elevenLabsClient?.synthesizeSpeech(text)
                            }
                            
                            if (audioBytes != null) {
                                playMP3Audio(audioBytes)
                            } else {
                                Log.w("MainActivity", "ElevenLabs failed, using TTS")
                                speakWithAndroidTTS(text)
                            }
                        }
                    }
                    else -> {
                        Log.d("MainActivity", "🔊 Using Android TTS")
                        speakWithAndroidTTS(text)
                    }
                }
                
                // Wait for speech to complete
                delay(text.length * 50L)
                
            } catch (e: Exception) {
                Log.e("MainActivity", "Voice Error: ${e.message}", e)
                speakWithAndroidTTS(text)
            } finally {
                updateUI(AssistantState.IDLE)
                
                // Auto restart listening in continuous mode
                if (isContinuousMode) {
                    delay(2000)
                    startListening()
                }
            }
        }
    }
    
    private fun playPCMAudio(audioBytes: ByteArray) {
        try {
            Log.d("MainActivity", "🔊 Initializing AudioTrack for PCM playback...")
            val audioTrack = android.media.AudioTrack(
                android.media.AudioManager.STREAM_MUSIC,
                16000, // Sample rate
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                audioBytes.size,
                android.media.AudioTrack.MODE_STATIC
            )
            
            audioTrack.write(audioBytes, 0, audioBytes.size)
            audioTrack.play()
            
            Log.i("MainActivity", "✅ Playing Cartesia audio (${audioBytes.size} bytes)")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ PCM playback error: ${e.message}", e)
            speakWithAndroidTTS("Audio playback error")
        }
    }
    
    private fun playMP3Audio(audioBytes: ByteArray) {
        try {
            val tempFile = java.io.File.createTempFile("jarvis_voice", ".mp3", cacheDir)
            tempFile.writeBytes(audioBytes)
            
            val mediaPlayer = android.media.MediaPlayer()
            mediaPlayer.setDataSource(tempFile.absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.start()
            
            mediaPlayer.setOnCompletionListener {
                it.release()
                tempFile.delete()
            }
            
            Log.i("MainActivity", "✅ Playing ElevenLabs audio")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ MP3 playback error: ${e.message}", e)
            speakWithAndroidTTS("Audio playback error")
        }
    }
    
    private fun speakWithAndroidTTS(text: String) {
        if (tts == null) {
            tts = android.speech.tts.TextToSpeech(this) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    // Auto-detect language and set appropriate voice
                    val hasBangla = text.any { it in '\u0980'..'\u09FF' }
                    
                    if (hasBangla) {
                        // Set Bangla language
                        val bengaliLocale = java.util.Locale("bn", "BD")
                        val result = tts?.setLanguage(bengaliLocale)
                        if (result == android.speech.tts.TextToSpeech.LANG_MISSING_DATA ||
                            result == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.w("MainActivity", "Bangla TTS not supported, using English")
                            tts?.language = java.util.Locale.US
                        } else {
                            Log.i("MainActivity", "🇧🇩 Using Bangla TTS voice")
                        }
                    } else {
                        // Use English
                        tts?.language = java.util.Locale.US
                    }
                    
                    tts?.setPitch(1.1f)
                    tts?.setSpeechRate(0.9f)
                    tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "jarvis")
                }
            }
        } else {
            // Auto-detect language for existing TTS
            val hasBangla = text.any { it in '\u0980'..'\u09FF' }
            if (hasBangla) {
                val bengaliLocale = java.util.Locale("bn", "BD")
                tts?.setLanguage(bengaliLocale)
            } else {
                tts?.language = java.util.Locale.US
            }
            tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "jarvis")
        }
    }

    private fun updateUI(state: AssistantState) {
        currentState = state
        binding.apply {
            when (state) {
                AssistantState.IDLE -> {
                    statusText.text = getString(R.string.tap_to_speak)
                    voiceVisualizer.visibility = View.INVISIBLE
                    micButton.isEnabled = true
                }
                AssistantState.LISTENING -> {
                    statusText.text = getString(R.string.listening)
                    voiceVisualizer.visibility = View.VISIBLE
                    micButton.isEnabled = true
                }
                AssistantState.PROCESSING -> {
                    statusText.text = getString(R.string.processing)
                    voiceVisualizer.visibility = View.INVISIBLE
                    micButton.isEnabled = false
                }
                AssistantState.SPEAKING -> {
                    statusText.text = getString(R.string.speaking)
                    voiceVisualizer.visibility = View.VISIBLE
                    micButton.isEnabled = false
                }
                AssistantState.ERROR -> {
                    statusText.text = "Error occurred"
                    voiceVisualizer.visibility = View.INVISIBLE
                    micButton.isEnabled = true
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        wakeWordDetector?.stop()
    }
}
