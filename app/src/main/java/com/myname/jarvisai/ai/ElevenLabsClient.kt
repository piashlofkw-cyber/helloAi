package com.myname.jarvisai.ai

import com.myname.jarvisai.models.ElevenLabsTTSRequest
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface ElevenLabsApiService {
    @POST("v1/text-to-speech/{voice_id}")
    suspend fun textToSpeech(
        @Path("voice_id") voiceId: String,
        @Header("xi-api-key") apiKey: String,
        @Body request: ElevenLabsTTSRequest
    ): ResponseBody
}

class ElevenLabsClient(private val apiKey: String) {
    
    companion object {
        // Default voice ID (you can change this to your preferred voice)
        const val DEFAULT_VOICE_ID = "21m00Tcm4TlvDq8ikWAM" // Rachel voice
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.elevenlabs.io/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val service = retrofit.create(ElevenLabsApiService::class.java)
    
    suspend fun synthesizeSpeech(
        text: String,
        voiceId: String = DEFAULT_VOICE_ID
    ): ByteArray? {
        return try {
            val request = ElevenLabsTTSRequest(text = text)
            
            val response = service.textToSpeech(
                voiceId = voiceId,
                apiKey = apiKey,
                request = request
            )
            
            response.bytes()
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
