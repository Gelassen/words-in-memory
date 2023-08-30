package io.github.gelassen.wordinmemory.model

import com.google.gson.annotations.SerializedName

data class SplitOnWordsPayload (
    @SerializedName("text") var text: String = ""
)