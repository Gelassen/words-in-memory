package io.github.gelassen.wordinmemory.network

import com.google.gson.annotations.SerializedName

/**
 * Chat-completions request/response models (OpenAI-compatible wire format).
 * Provider-agnostic: any backend that exposes the same schema can be used.
 */

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatCompletionMessage>,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens")
    val maxTokens: Int = 800,
    /** Hint for backends that support disabling extended reasoning */
    @SerializedName("reasoning_effort")
    val reasoningEffort: String? = "none",
    val stream: Boolean = false
)

data class ChatCompletionMessage(
    val role: String, // "system" | "user" | "assistant"
    val content: String
)

data class ChatCompletionResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChatCompletionChoice>? = null,
    val usage: ChatCompletionUsage? = null,
    val error: ChatCompletionError? = null
)

data class ChatCompletionChoice(
    val index: Int? = null,
    val message: ChatCompletionMessage? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class ChatCompletionUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int? = null,
    @SerializedName("completion_tokens")
    val completionTokens: Int? = null,
    @SerializedName("total_tokens")
    val totalTokens: Int? = null
)

data class ChatCompletionError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)
