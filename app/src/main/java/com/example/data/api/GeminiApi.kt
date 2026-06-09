package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<GeminiCandidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiRepository {
    suspend fun getGeminiResponse(query: String, currentMarketsState: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "API Key is missing. Please add your GEMINI_API_KEY in the AI Studio Secrets panel."
        }

        val systemInstruction = "You are an intelligent marketplace guide named 'Gemini Market Bridge Assistant'. " +
                "You help users navigate through secondary decentralized marketplaces, answer security questions, " +
                "and explain bridge routing protocols. Address the user. If they mention Subhajit, greet them kindly. " +
                "Always recommend using the Direct Secure Card Link buttons on the screen since they route through our secure verification gateway code. " +
                "Keep responses professional, brief, clear, and highlight cryptographic best practices. " +
                "Here is the current state of tracked marketplaces:\n$currentMarketsState"

        val request = GenerateContentRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = query)))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstruction)))
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No suggestions retrieved. Please verify your internet connection."
        } catch (e: Exception) {
            "Connection Error: Unable to query Gemini API. Reason: ${e.localizedMessage ?: "Timeout"}. Make sure your API key in AI Studio Secrets is valid."
        }
    }
}
