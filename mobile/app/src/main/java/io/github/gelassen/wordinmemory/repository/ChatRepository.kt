package io.github.gelassen.wordinmemory.repository

import android.content.SharedPreferences
import android.util.Log
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.network.ChatCompletionMessage
import io.github.gelassen.wordinmemory.network.ChatCompletionRequest
import io.github.gelassen.wordinmemory.network.ChatCompletionResponse
import io.github.gelassen.wordinmemory.network.LlmApi
import io.github.gelassen.wordinmemory.network.LlmBaseUrlInterceptor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All LLM network traffic for the practice chat lives here.
 * ViewModels must not call Retrofit / OkHttp directly.
 *
 * Backend is configured via SharedPreferences (host, port, model name).
 * Swapping provider or model does not require changes in UI code.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val llmApi: LlmApi,
    private val sharedPreferences: SharedPreferences
) {

    sealed class Result {
        data class Success(val content: String, val model: String?) : Result()
        data class Error(val message: String, val httpCode: Int? = null) : Result()
    }

    fun getModelName(): String {
        return sharedPreferences
            .getString(LlmBaseUrlInterceptor.PREF_LLM_MODEL, LlmBaseUrlInterceptor.DEFAULT_MODEL)
            ?.trim()
            ?.ifEmpty { LlmBaseUrlInterceptor.DEFAULT_MODEL }
            ?: LlmBaseUrlInterceptor.DEFAULT_MODEL
    }

    fun getEndpointDescription(): String {
        val host = sharedPreferences
            .getString(LlmBaseUrlInterceptor.PREF_LLM_HOST, LlmBaseUrlInterceptor.DEFAULT_HOST)
            ?: LlmBaseUrlInterceptor.DEFAULT_HOST
        val port = sharedPreferences
            .getString(LlmBaseUrlInterceptor.PREF_LLM_PORT, LlmBaseUrlInterceptor.DEFAULT_PORT)
            ?: LlmBaseUrlInterceptor.DEFAULT_PORT
        return "http://$host:$port (model=${getModelName()})"
    }

    /**
     * Sends a full multi-turn conversation to the configured LLM backend.
     * @param systemPrompt tutor instructions (may be blank)
     * @param history ordered list of user/assistant turns (roles: "user" | "assistant")
     */
    suspend fun completeChat(
        systemPrompt: String,
        history: List<ChatCompletionMessage>
    ): Result {
        val model = getModelName()
        val messages = buildList {
            if (systemPrompt.isNotBlank()) {
                add(ChatCompletionMessage(role = "system", content = systemPrompt))
            }
            addAll(history)
        }

        val request = ChatCompletionRequest(
            model = model,
            messages = messages,
            temperature = 0.7,
            maxTokens = 800,
            reasoningEffort = "none",
            stream = false
        )

        return try {
            Log.d(App.TAG, "LLM request → ${getEndpointDescription()}, msgs=${messages.size}")
            val response = llmApi.chatCompletions(request)
            if (response.isSuccessful) {
                parseSuccess(response.body(), model)
            } else {
                val errBody = response.errorBody()?.string()
                Log.e(App.TAG, "LLM HTTP ${response.code()}: $errBody")
                Result.Error(
                    message = mapHttpError(response.code(), errBody),
                    httpCode = response.code()
                )
            }
        } catch (e: Exception) {
            Log.e(App.TAG, "LLM network failure", e)
            Result.Error(
                message = "Не удалось связаться с AI-сервером (${getEndpointDescription()}): ${e.message}"
            )
        }
    }

    private fun parseSuccess(body: ChatCompletionResponse?, model: String): Result {
        if (body == null) {
            return Result.Error("Пустой ответ от AI-сервера")
        }
        body.error?.message?.let { err ->
            return Result.Error(err)
        }

        val choice = body.choices?.firstOrNull()
        val content = choice?.message?.content?.trim().orEmpty()

        if (content.isEmpty()) {
            Log.w(App.TAG, "LLM returned empty content (finish=${choice?.finishReason})")
            return Result.Error(
                "Модель вернула пустой ответ. Проверьте настройки AI-сервера и лимит токенов."
            )
        }

        return Result.Success(content = content, model = body.model ?: model)
    }

    private fun mapHttpError(code: Int, body: String?): String {
        return when (code) {
            404 -> "Модель «${getModelName()}» не найдена на AI-сервере (404). " +
                "Проверьте имя модели в Настройках."
            400 -> "Некорректный запрос к AI-серверу (400). ${body ?: ""}"
            500, 502, 503 -> "AI-сервер временно недоступен ($code). Проверьте, что сервис запущен."
            else -> "Ошибка AI-сервера ($code). ${body ?: ""}"
        }
    }
}
