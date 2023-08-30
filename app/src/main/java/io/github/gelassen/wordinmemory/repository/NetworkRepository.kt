package io.github.gelassen.wordinmemory.repository

import android.util.Log
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.model.SplitOnWordsPayload
import io.github.gelassen.wordinmemory.model.SplitOnWordsResponse
import io.github.gelassen.wordinmemory.network.ApiResponse
import io.github.gelassen.wordinmemory.network.IApi
import io.github.gelassen.wordinmemory.network.Response
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection

class NetworkRepository(url: String) {

    private val api: IApi

    init {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val httpClient = OkHttpClient
            .Builder()
            .addInterceptor(logging)
            .build()
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .baseUrl(url)
            .build()

        api = retrofit.create(IApi::class.java)
    }

    suspend fun splitChineseSentenceIntoWords(text: String): Response<List<List<String>>> {
        lateinit var result: Response<List<List<String>>>
        try {
            val response = api.splitChineseTextIntoWords(subj = SplitOnWordsPayload(text))
            if (isRequestOk(response)) {
                val payload = response.body()!! // check if it can be consumed a second time
                result = Response.Data(payload.payload.data)
            } else {
                Log.d(App.TAG, "Get an error from backend ${response.errorBody()} + ${response.message()}")
                result = Response.Error.Message("Something went wrong on backend")
            }
        } catch (ex: Exception) {
            Log.e(App.TAG, "Failed to classify text with error", ex)
        }
        return result
    }

    private fun isRequestOk(response: retrofit2.Response<ApiResponse<SplitOnWordsResponse>>): Boolean {
        return response.isSuccessful
                && response.body()!!
                    .payload.status == HttpURLConnection.HTTP_OK
    }

}