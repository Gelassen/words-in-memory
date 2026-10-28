package io.github.gelassen.wordinmemory.network

import com.google.gson.annotations.SerializedName

data class ApiResponse<T> (
    @SerializedName("payload") var payload : T,
    @SerializedName("msg") var message : String
)