package io.github.gelassen.wordinmemory.model

import com.google.gson.annotations.SerializedName

data class SplitOnWordsApiResponse(
    @SerializedName("status") var status: Int? = null,
    @SerializedName("data") var data: ArrayList<ArrayList<String>> = arrayListOf()
)