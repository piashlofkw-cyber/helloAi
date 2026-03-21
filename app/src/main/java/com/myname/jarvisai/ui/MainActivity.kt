package com.myname.jarvisai.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var avatarManager: AvatarManager
    private lateinit var aiManager: AIManager
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var elevenLabsClient: ElevenLabsClient? = null
    
    private var currentState = AssistantState.IDLE

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
                val elevenLabsKey = prefsManager.getElevenLabsApiKey()

                if (elevenLabsKey.isNotEmpty()) {
                    elevenLabsClient = ElevenLabsClient(elevenLabsKey)
                }

                // Load avatar
                avatarManager.loadAvatar(binding.avatarImage, "neutral")

                // Show configured models count
                val modelCount = prefsManager.getEnabledModelsByPriority().size
                if (modelCount > 0) {
                    Toast.makeText(
                        this@MainActivity,
                        "$modelCount AI model(s) ready",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error initializing AI", Toast.LENGTH_SHORT).show()
            }
        }
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
                    processUserInput(matches[0])
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
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

            } catch (e: Exception) {
                binding.responseText.text = "Error: ${e.message}"
                updateUI(AssistantState.ERROR)
            }
        }
    }

    private fun speakResponse(text: String) {
        lifecycleScope.launch {
            updateUI(AssistantState.SPEAKING)
            
            try {
                withContext(Dispatchers.IO) {
                    elevenLabsClient?.synthesizeSpeech(text)
                }
                // Audio playback would happen here
                
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "TTS Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                updateUI(AssistantState.IDLE)
            }
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
    }
}
)
    }
}
