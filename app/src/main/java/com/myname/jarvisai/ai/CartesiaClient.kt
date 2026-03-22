package com.myname.jarvisai.ai

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cartesia AI Client - Ultra-fast TTS (40ms-90ms!)
 * Fastest and most realistic voice AI
 */
class CartesiaClient(
    private val apiKey: String,
    private val voiceId: String = "a0e99841-438c-4a64-b679-ae501e7d6091" // Default: Barbershop Man
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "CartesiaClient"
        private const val API_BASE_URL = "https://api.cartesia.ai"
        private const val API_VERSION = "2024-06-10"
        
        // Popular Voice IDs
        const val VOICE_BARBERSHOP_MAN = "a0e99841-438c-4a64-b679-ae501e7d6091"
        const val VOICE_CLASSY_BRITISH_MAN = "63ff761f-c1e8-414b-b969-d1833d1c870c"
        const val VOICE_CALM_LADY = "156fb8d2-335b-4950-9cb3-a2d33befec77"
        const val VOICE_SWEET_LADY = "79a125e8-cd45-4c13-8a67-188112f4dd22"
        const val VOICE_FRIENDLY_READING_MAN = "638efaaa-4d0c-442e-b701-3fae16aad012"
    }

    /**
     * Text to Speech - Returns PCM audio bytes
     */
    suspend fun textToSpeech(
        text: String,
        customVoiceId: String? = null,
        modelId: String = "sonic-english", // Options: sonic-english, sonic-multilingual, sonic-turbo
        language: String = "en",
        outputFormat: String = "pcm_16000" // Options: pcm_16000, pcm_22050, pcm_44100, mp3
    ): ByteArray? {
        return try {
            val selectedVoiceId = customVoiceId ?: voiceId
            
            Log.d(TAG, "🎤 Generating speech with Cartesia...")
            Log.d(TAG, "   API Key: ${apiKey.take(15)}...")
            Log.d(TAG, "   Model: $modelId")
            Log.d(TAG, "   Voice ID: $selectedVoiceId")
            Log.d(TAG, "   Text: ${text.take(50)}...")
            Log.d(TAG, "   URL: $API_BASE_URL/tts/bytes")
            
            val requestBody = JSONObject().apply {
                put("model_id", modelId)
                put("transcript", text)
                put("voice", JSONObject().apply {
                    put("mode", "id")
                    put("id", selectedVoiceId)
                })
                put("language", language)
                put("output_format", JSONObject().apply {
                    put("container", "raw")
                    put("encoding", "pcm_s16le")
                    put("sample_rate", 16000)
                })
            }

            val request = Request.Builder()
                .url("$API_BASE_URL/tts/bytes")
                .header("X-API-Key", apiKey)
                .header("Cartesia-Version", API_VERSION)
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val error = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "❌ Cartesia API error (${response.code}): $error")
                return null
            }

            val audioBytes = response.body?.bytes()
            Log.i(TAG, "✅ Generated ${audioBytes?.size ?: 0} bytes of audio")
            
            audioBytes

        } catch (e: Exception) {
            Log.e(TAG, "❌ Cartesia error: ${e.message}", e)
            null
        }
    }

    /**
     * Get available voices
     */
    suspend fun getVoices(): List<Voice>? {
        return try {
            val request = Request.Builder()
                .url("$API_BASE_URL/voices")
                .header("X-API-Key", apiKey)
                .header("Cartesia-Version", API_VERSION)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                // Parse voices list
                // Implementation depends on API response structure
                Log.i(TAG, "✅ Retrieved voices list")
                null // TODO: Parse JSON
            } else {
                Log.e(TAG, "❌ Failed to get voices: ${response.code}")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting voices: ${e.message}")
            null
        }
    }

    data class Voice(
        val id: String,
        val name: String,
        val language: String,
        val description: String
    )
}
