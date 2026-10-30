package io.github.gelassen.wordinmemory.network

import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Rewrites request host/port from SharedPreferences so the client can reach
 * whatever LLM backend is configured (local or remote).
 *
 * Prefs:
 *   llm_host  — IP or hostname
 *   llm_port  — port
 */
class LlmBaseUrlInterceptor(
    private val sharedPreferences: SharedPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val host = sharedPreferences
            .getString(PREF_LLM_HOST, DEFAULT_HOST)
            ?.trim()
            ?.ifEmpty { DEFAULT_HOST }
            ?: DEFAULT_HOST

        val port = sharedPreferences
            .getString(PREF_LLM_PORT, DEFAULT_PORT)
            ?.toIntOrNull()
            ?: DEFAULT_PORT.toInt()

        val newUrl = original.url.newBuilder()
            .scheme("http")
            .host(host)
            .port(port)
            .build()

        val newRequest = original.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }

    companion object {
        const val PREF_LLM_HOST = "llm_host"
        const val PREF_LLM_PORT = "llm_port"
        const val PREF_LLM_MODEL = "llm_model"

        /** Android emulator → host machine loopback */
        const val DEFAULT_HOST = "10.0.2.2"
        const val DEFAULT_PORT = "11434"
        const val DEFAULT_MODEL = "gemma4:12b"

        fun resolveBaseUrl(prefs: SharedPreferences): String {
            val host = prefs.getString(PREF_LLM_HOST, DEFAULT_HOST)?.trim().orEmpty()
                .ifEmpty { DEFAULT_HOST }
            val port = prefs.getString(PREF_LLM_PORT, DEFAULT_PORT)?.trim().orEmpty()
                .ifEmpty { DEFAULT_PORT }
            return "http://$host:$port/v1/"
        }
    }
}
