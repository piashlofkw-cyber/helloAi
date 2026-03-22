package com.myname.jarvisai.ai

import com.myname.jarvisai.models.GroqRequest
import com.myname.jarvisai.models.GroqResponse
import com.myname.jarvisai.models.Message
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface GroqApiService {
    @POST("openai/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: GroqRequest
    ): GroqResponse
}

class GroqClient(private val apiKey: String) {
    
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
        .baseUrl("https://api.groq.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val service = retrofit.create(GroqApiService::class.java)
    
    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<Message> = emptyList(),
        model: String = "mixtral-8x7b-32768"
    ): String {
        val messages = mutableListOf(
            Message(
                role = "system",
                content = "You are Jarvis, an advanced AI assistant. Be helpful, concise, and friendly."
            )
        )
        messages.addAll(conversationHistory)
        messages.add(Message(role = "user", content = userMessage))
        
        val request = GroqRequest(
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
            ?: throw Exception("Empty response from Groq")
    }
}
