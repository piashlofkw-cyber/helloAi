package com.myname.jarvisai.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.myname.jarvisai.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Vision AI - Image analysis using Gemini Vision or GPT-4 Vision
 */
class VisionAI(context: Context) {

    private val prefsManager = PreferencesManager(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "VisionAI"
        private const val GEMINI_VISION_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-pro-vision:generateContent"
        private const val GPT4_VISION_URL = "https://api.openai.com/v1/chat/completions"
    }

    /**
     * Analyze image with AI
     */
    suspend fun analyzeImage(
        bitmap: Bitmap,
        prompt: String = "Describe what you see in this image in detail."
    ): String = withContext(Dispatchers.IO) {
        try {
            // Try Gemini Vision first (if available)
            val geminiKey = prefsManager.getGeminiApiKey()
            
            if (geminiKey.isNotEmpty()) {
                analyzeWithGemini(bitmap, prompt, geminiKey)
            } else {
                // Fallback to GPT-4 Vision via OpenRouter
                val openRouterKey = prefsManager.getOpenRouterApiKey()
                if (openRouterKey.isNotEmpty()) {
                    analyzeWithGPT4Vision(bitmap, prompt, openRouterKey)
                } else {
                    "Please configure Gemini API key for vision features in Settings"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vision analysis failed: ${e.message}")
            "Error analyzing image: ${e.message}"
        }
    }

    private fun analyzeWithGemini(bitmap: Bitmap, prompt: String, apiKey: String): String {
        val base64Image = bitmapToBase64(bitmap)
        
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url("$GEMINI_VISION_URL?key=$apiKey")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Gemini API error: ${response.code}")
            }

            val responseBody = response.body?.string()
            val jsonResponse = JSONObject(responseBody ?: "{}")
            
            return jsonResponse
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }

    private fun analyzeWithGPT4Vision(bitmap: Bitmap, prompt: String, apiKey: String): String {
        val base64Image = bitmapToBase64(bitmap)
        
        val requestJson = JSONObject().apply {
            put("model", "openai/gpt-4-vision-preview")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().put("type", "text").put("text", prompt))
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")
                            })
                        })
                    })
                })
            })
            put("max_tokens", 500)
        }

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("GPT-4 Vision error: ${response.code}")
            }

            val responseBody = response.body?.string()
            val jsonResponse = JSONObject(responseBody ?: "{}")
            
            return jsonResponse
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
