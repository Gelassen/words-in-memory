package io.github.gelassen.wordinmemory.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Provider-agnostic chat completions API.
 * Base URL is injected at runtime (see [LlmBaseUrlInterceptor]).
 */
interface LlmApi {

    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun chatCompletions(
        @Body body: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}
