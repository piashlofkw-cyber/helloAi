package com.myname.jarvisai.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.myname.jarvisai.R
import com.myname.jarvisai.databinding.ActivitySettingsBinding
import com.myname.jarvisai.utils.PreferencesManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefsManager: PreferencesManager

    private val groqModels = listOf(
        "mixtral-8x7b-32768" to "Mixtral 8x7B (Fast, Free)",
        "llama3-8b-8192" to "Llama 3 8B (Fast)",
        "llama3-70b-8192" to "Llama 3 70B (Powerful)",
        "gemma-7b-it" to "Gemma 7B (Google)"
    )

    private val openRouterModels = listOf(
        "openai/gpt-3.5-turbo" to "GPT-3.5 Turbo (Fast, Cheap)",
        "openai/gpt-4-turbo" to "GPT-4 Turbo (Best Quality)",
        "anthropic/claude-3-haiku" to "Claude 3 Haiku (Fast)",
        "anthropic/claude-3-sonnet" to "Claude 3 Sonnet (Balanced)",
        "meta-llama/llama-3-70b" to "Llama 3 70B",
        "google/gemini-pro" to "Gemini Pro"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)

        setupDropdowns()
        loadSettings()
        setupListeners()
    }

    private fun setupDropdowns() {
        // Personality Mode dropdown
        val personalities = PersonalityMode.values().map { it.displayName }
        val personalityAdapter = ArrayAdapter(this, R.layout.dropdown_item, personalities)
        binding.personalityDropdown.setAdapter(personalityAdapter)
        binding.personalityDropdown.setText(prefsManager.getPersonalityMode(), false)
        
        // AI Provider dropdown
        val providers = listOf(
            PreferencesManager.PROVIDER_GROQ,
            PreferencesManager.PROVIDER_OPENROUTER
        )
        val providerAdapter = ArrayAdapter(this, R.layout.dropdown_item, providers)
        binding.aiProviderDropdown.setAdapter(providerAdapter)

        // Provider selection listener
        binding.aiProviderDropdown.setOnItemClickListener { _, _, position, _ ->
            val provider = providers[position]
            updateModelDropdown(provider)
        }

        // Initial model dropdown setup
        updateModelDropdown(prefsManager.getAiProvider())
    }

    private fun updateModelDropdown(provider: String) {
        val models = when (provider) {
            PreferencesManager.PROVIDER_GROQ -> groqModels
            PreferencesManager.PROVIDER_OPENROUTER -> openRouterModels
            else -> groqModels
        }

        val modelNames = models.map { it.second }
        val modelAdapter = ArrayAdapter(this, R.layout.dropdown_item, modelNames)
        binding.modelDropdown.setAdapter(modelAdapter)

        // Set current selection
        val currentModel = prefsManager.getAiModel()
        val currentIndex = models.indexOfFirst { it.first == currentModel }
        if (currentIndex >= 0) {
            binding.modelDropdown.setText(modelNames[currentIndex], false)
        }
    }

    private fun loadSettings() {
        binding.apply {
            groqApiKeyInput.setText(prefsManager.getGroqApiKey())
            openrouterApiKeyInput.setText(prefsManager.getOpenRouterApiKey())
            elevenlabsApiKeyInput.setText(prefsManager.getElevenLabsApiKey())
            wakeWordSwitch.isChecked = prefsManager.isWakeWordEnabled()
            wakeWordInput.setText(prefsManager.getWakeWord())
            
            // Set AI provider
            aiProviderDropdown.setText(prefsManager.getAiProvider(), false)
        }
    }

    private fun setupListeners() {
        binding.saveButton.setOnClickListener {
            saveSettings()
        }

        binding.modelManagerButton.setOnClickListener {
            startActivity(Intent(this, ModelManagerActivity::class.java))
        }

        binding.wakeWordSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.wakeWordInput.isEnabled = isChecked
        }
    }

    private fun saveSettings() {
        binding.apply {
            val groqKey = groqApiKeyInput.text.toString().trim()
            val openRouterKey = openrouterApiKeyInput.text.toString().trim()
            val elevenLabsKey = elevenlabsApiKeyInput.text.toString().trim()
            val wakeWord = wakeWordInput.text.toString().trim()
            val wakeWordEnabled = wakeWordSwitch.isChecked
            val provider = aiProviderDropdown.text.toString()

            // Validate inputs
            if (groqKey.isEmpty() && openRouterKey.isEmpty()) {
                Toast.makeText(
                    this@SettingsActivity,
                    "Please enter at least one AI API key",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            // Get selected model
            val selectedModelName = modelDropdown.text.toString()
            val models = when (provider) {
                PreferencesManager.PROVIDER_GROQ -> groqModels
                PreferencesManager.PROVIDER_OPENROUTER -> openRouterModels
                else -> groqModels
            }
            val selectedModel = models.find { it.second == selectedModelName }?.first
                ?: PreferencesManager.DEFAULT_GROQ_MODEL

            // Save to preferences
            prefsManager.setGroqApiKey(groqKey)
            prefsManager.setOpenRouterApiKey(openRouterKey)
            prefsManager.setElevenLabsApiKey(elevenLabsKey)
            prefsManager.setWakeWordEnabled(wakeWordEnabled)
            prefsManager.setWakeWord(wakeWord)
            prefsManager.setAiProvider(provider)
            prefsManager.setAiModel(selectedModel)

            Toast.makeText(this@SettingsActivity, "Settings saved! Using $provider - $selectedModelName", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
   if (continuousListeningSwitch.isChecked) {
                val serviceIntent = Intent(this@SettingsActivity, ContinuousListeningService::class.java)
                serviceIntent.action = ContinuousListeningService.ACTION_START
                startService(serviceIntent)
            } else {
                val serviceIntent = Intent(this@SettingsActivity, ContinuousListeningService::class.java)
                serviceIntent.action = ContinuousListeningService.ACTION_STOP
                startService(serviceIntent)
            }

            Toast.makeText(this@SettingsActivity, "Settings saved! Personality: $personalityMode", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
