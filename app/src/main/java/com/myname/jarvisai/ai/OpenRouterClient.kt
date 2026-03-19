package com.myname.jarvisai.ai

import com.myname.jarvisai.models.Message
import com.myname.jarvisai.models.OpenRouterRequest
import com.myname.jarvisai.models.OpenRouterResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface OpenRouterApiService {
    @POST("api/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}

class OpenRouterClient(private val apiKey: String) {
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val service = retrofit.create(OpenRouterApiService::class.java)
    
    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<Message> = emptyList(),
        model: String = "openai/gpt-4-turbo"
    ): String {
        try {
            val messages = mutableListOf(
                Message(
                    role = "system",
                    content = "You are Jarvis, an advanced AI assistant. Be helpful, concise, and friendly."
                )
            )
            messages.addAll(conversationHistory)
            messages.add(Message(role = "user", content = userMessage))
            
            val request = OpenRouterRequest(
                model = model,
                messages = messages,
                temperature = 0.7f,
                maxTokens = 1024
            )
            
            val response = service.chatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )
            
            return response.choices.firstOrNull()?.message?.content 
                ?: "I apologize, but I couldn't generate a response."
                
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error communicating with AI: ${e.message}"
        }
    }
}
