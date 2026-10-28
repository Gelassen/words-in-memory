package io.github.gelassen.wordinmemory.utils

import android.text.TextUtils

class Validator {

    fun isAllowedWordOrSentence(msg: String, translatedMsg: String): Boolean {
        return !TextUtils.isEmpty(msg) && !TextUtils.isEmpty(translatedMsg)
    }

    fun isAllowedWordOrSentence(msg: String): Boolean {
        return !TextUtils.isEmpty(msg)
    }

}