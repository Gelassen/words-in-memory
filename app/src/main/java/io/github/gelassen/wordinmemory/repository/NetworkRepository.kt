package io.github.gelassen.wordinmemory.repository

import android.util.Log
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.model.SplitOnWordsPayload
import io.github.gelassen.wordinmemory.model.SplitOnWordsApiResponse
import io.github.gelassen.wordinmemory.network.ApiResponse
import io.github.gelassen.wordinmemory.network.IApi
import io.github.gelassen.wordinmemory.network.Response
import java.net.HttpURLConnection

class NetworkRepository(val api: IApi) {

    suspend fun splitChineseSentenceIntoWords(text: String): Response<ArrayList<ArrayList<String>>> {
        lateinit var result: Response<ArrayList<ArrayList<String>>>
        try {
            val response = api.splitChineseTextIntoWords(subj = SplitOnWordsPayload(text))
            Log.d(App.TAG, "response headers ${response.headers()}")
            Log.d(App.TAG, "Response from the backend as body ${response.body()} " +
                    "and as a raw ${response.raw()} ") /*(${response.raw().body!!.byteString()} and ${response.raw().message})*/
            if (isRequestOk(response)) {
                val payload = response.body()!! // check if it can be consumed a second time
                result = Response.Data(payload.data)
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

    private fun isRequestOk(response: retrofit2.Response<SplitOnWordsApiResponse>): Boolean {
        Log.d(App.TAG, "Response from the backend ${response}")
        return response.isSuccessful
                && response.body()!!.status == HttpURLConnection.HTTP_OK
    }

}