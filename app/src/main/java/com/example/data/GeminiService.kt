package com.example.data

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
) {
    @JsonClass(generateAdapter = true)
    data class Content(
        val parts: List<Part>
    )

    @JsonClass(generateAdapter = true)
    data class Part(
        val text: String
    )

    @JsonClass(generateAdapter = true)
    data class GenerationConfig(
        val temperature: Float? = null,
        val maxOutputTokens: Int? = null
    )
}

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>? = null
) {
    @JsonClass(generateAdapter = true)
    data class Candidate(
        val content: Content? = null
    )

    @JsonClass(generateAdapter = true)
    data class Content(
        val parts: List<Part>? = null
    )

    @JsonClass(generateAdapter = true)
    data class Part(
        val text: String? = null
    )
}

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)
}
