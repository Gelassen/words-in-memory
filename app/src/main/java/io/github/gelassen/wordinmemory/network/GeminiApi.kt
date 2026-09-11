package io.github.gelassen.wordinmemory.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Google Gemini generateContent.
 * Full URL is built by the caller and must include ?key=...
 * Example:
 * https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=AIza...
 */
interface GeminiApi {

    @Headers("Content-Type: application/json")
    @POST
    suspend fun generateContent(
        @Url url: String,
        @Body body: GeminiRequest
    ): Response<GeminiResponse>
}
