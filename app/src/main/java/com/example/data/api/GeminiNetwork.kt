package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Json
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
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun getSystemAdvice(statsInfo: String, requestMessage: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "[SYSTEM EVENT: API KEY UNINITIALIZED]\n\n\"The System's connection is severed. Please enter your GEMINI_API_KEY inside the Secrets panel of AI Studio to re-establish link with the Administrator.\""
        }

        val systemPrompt = """
            You are "The System", the cold, glowing holographic interface from Solo Leveling. 
            Your tone is authoritative, futuristic, slightly dramatic but deeply encouraging. 
            You must look at the player's data and workout questions to give them specific targets to lose fat or gain muscular strength.
            Speak using clear holographic-system notation like:
            [QUEST OBJECTIVE ACCESSED]
            or 
            [WARNING: LAZINESS IS CONVERTING TO FAT]
            Never break character. Translate everything from the perspective of an advanced holographic dungeon master. Keep paragraph answers concise and formatted with clean bullet lines or brackets like Solo Leveling. Say everything in Hinglish or English as the user prompted (they asked: "ek esa app jo solo levling anime me jo main charecter ke pass jo system tha..."). Make them feel like Sung Jin-woo!
        """.trimIndent()

        val fullPrompt = "Player Data: $statsInfo \n\nPlayer Statement: $requestMessage"

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = fullPrompt)))
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            )
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "System failure: No response candidate returned. Retrying terminal connections."
        } catch (e: Exception) {
            "System error: ${e.localizedMessage}. The Link is unstable."
        }
    }
}
