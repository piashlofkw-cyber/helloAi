package com.myname.jarvisai.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.myname.jarvisai.databinding.ActivitySettingsBinding
import com.myname.jarvisai.utils.PreferencesManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)

        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        binding.apply {
            groqApiKeyInput.setText(prefsManager.getGroqApiKey())
            openrouterApiKeyInput.setText(prefsManager.getOpenRouterApiKey())
            elevenlabsApiKeyInput.setText(prefsManager.getElevenLabsApiKey())
            wakeWordSwitch.isChecked = prefsManager.isWakeWordEnabled()
            wakeWordInput.setText(prefsManager.getWakeWord())
        }
    }

    private fun setupListeners() {
        binding.saveButton.setOnClickListener {
            saveSettings()
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

            // Validate inputs
            if (groqKey.isEmpty() && openRouterKey.isEmpty()) {
                Toast.makeText(
                    this@SettingsActivity,
                    "Please enter at least one AI API key",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            // Save to preferences
            prefsManager.setGroqApiKey(groqKey)
            prefsManager.setOpenRouterApiKey(openRouterKey)
            prefsManager.setElevenLabsApiKey(elevenLabsKey)
            prefsManager.setWakeWordEnabled(wakeWordEnabled)
            prefsManager.setWakeWord(wakeWord)

            Toast.makeText(this@SettingsActivity, "Settings saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
