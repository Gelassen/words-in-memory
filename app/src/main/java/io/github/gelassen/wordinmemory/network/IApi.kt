package io.github.gelassen.wordinmemory.network

import io.github.gelassen.wordinmemory.model.SplitOnWordsPayload
import io.github.gelassen.wordinmemory.model.SplitOnWordsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface IApi {

    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("/classify")
    suspend fun splitChineseTextIntoWords(
        @Body subj: SplitOnWordsPayload
    ): Response<ApiResponse<SplitOnWordsResponse>>
}