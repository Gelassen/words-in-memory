package io.github.gelassen.wordinmemory.network

import com.google.gson.annotations.SerializedName

/**
 * Google Gemini generateContent request/response models.
 * Free tier: https://aistudio.google.com/apikey
 */

data class GeminiRequest(
    @SerializedName("system_instruction")
    val systemInstruction: GeminiContent? = null,
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    /** "user" or "model" (Gemini does not use "assistant" / "system" in contents) */
    val role: String? = null,
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerationConfig(
    val temperature: Double = 0.75,
    @SerializedName("maxOutputTokens")
    val maxOutputTokens: Int = 600
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)

data class GeminiCandidate(
    val content: GeminiContent? = null,
    @SerializedName("finishReason")
    val finishReason: String? = null
)

data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)
