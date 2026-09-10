package io.github.gelassen.wordinmemory.network

import com.google.gson.annotations.SerializedName

/**
 * Minimal OpenAI Chat Completions models (compatible with gpt-4o-mini / gpt-3.5-turbo).
 * Free trial credits are available for new OpenAI accounts.
 * For long-term free usage consider switching the base URL + model to Gemini / Groq.
 */

data class OpenAiChatRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens")
    val maxTokens: Int = 512
)

data class OpenAiMessage(
    val role: String,
    val content: String
)

data class OpenAiChatResponse(
    val id: String? = null,
    val choices: List<OpenAiChoice>? = null,
    val error: OpenAiError? = null
)

data class OpenAiChoice(
    val index: Int = 0,
    val message: OpenAiMessage? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class OpenAiError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)
