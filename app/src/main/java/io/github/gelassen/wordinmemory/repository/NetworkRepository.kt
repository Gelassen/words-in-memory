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

class NetworkRepository(val api: IApi) {

    suspend fun splitChineseSentenceIntoWords(text: String): Response<List<List<String>>> {
        lateinit var result: Response<List<List<String>>>
        try {
            val response = api.splitChineseTextIntoWords(subj = SplitOnWordsPayload(text))
            Log.d(App.TAG, "response headers ${response.headers()}")
            Log.d(App.TAG, "Response from the backend as body ${response.body()} " +
                    "and as a raw ${response.raw()} ") /*(${response.raw().body!!.byteString()} and ${response.raw().message})*/
            if (isRequestOk(response)) {
                val payload = response.body()!! // check if it can be consumed a second time
                result = Response.Data(payload.payload.data)
            } else {
                Log.d(App.TAG, "Get an error from backend ${response.errorBody()} + ${response.message()}")
                result = Response.Error.Message("Something went wrong on backend")
            }
        } catch (ex: Exception) {
            Log.e(App.TAG, "Failed to classify text with error", ex)
            result = Response.Error.Exception(ex)
        }
        return result
    }

    private fun isRequestOk(response: retrofit2.Response<ApiResponse<SplitOnWordsResponse>>): Boolean {
        Log.d(App.TAG, "Response from the backend ${response}")
        return response.isSuccessful
                && response.body()!!
                    .payload.status == HttpURLConnection.HTTP_OK
    }

}