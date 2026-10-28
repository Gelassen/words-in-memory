package io.github.gelassen.wordinmemory.network

import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.Response

class DynamicBaseUrlInterceptor(private val sharedPreferences: SharedPreferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Fetch updated server URL from SharedPreferences
        val serverIp = sharedPreferences.getString("server_ip", "192.168.1.1")
        val serverPort = sharedPreferences.getString("server_port", "8080")
        val newBaseUrl = "http://$serverIp:$serverPort"

        // Update the request URL dynamically
        val newUrl = originalRequest.url.newBuilder()
            .scheme("http")
            .host(serverIp ?: "192.168.1.1")
            .port(serverPort?.toIntOrNull() ?: 8080)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }

}
