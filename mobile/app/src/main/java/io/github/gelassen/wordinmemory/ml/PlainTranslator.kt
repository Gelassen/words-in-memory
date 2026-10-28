package io.github.gelassen.wordinmemory.ml

import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import io.github.gelassen.wordinmemory.BuildConfig
import okhttp3.internal.closeQuietly
import java.lang.Exception

open class PlainTranslator(listener: ITranslationListener?) {

    interface ITranslationListener {
        fun onTranslationSuccess(translatedText: String)
        fun onTranslationFailed(exception: Exception)
        fun onModelDownloaded()
        fun onModelDownloadFail(exception: Exception)
    }

    private var chineseToEnglishTranslator: Translator
    private var isTranslationModelReady: Boolean = false

    init {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.CHINESE)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        chineseToEnglishTranslator = Translation.getClient(options)
        prepare(listener)
    }

    fun isTranslationModelReady(): Boolean {
        return isTranslationModelReady
    }

    fun prepare(listener: ITranslationListener?) {
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        chineseToEnglishTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                isTranslationModelReady = true
                listener?.onModelDownloaded()
            }
            .addOnFailureListener { exception -> listener?.onModelDownloadFail(exception)}
    }

    fun translateChineseText(text: String, listener: ITranslationListener) {
        chineseToEnglishTranslator.translate(text)
            .addOnSuccessListener { translatedText -> listener.onTranslationSuccess(translatedText) }
            .addOnFailureListener { exception -> listener.onTranslationFailed(exception) }
    }

    /**
     * This is MUST be called to prevent memory leaks
     * */
    fun manageAutoClose(lifecycle: LifecycleOwner) {
        lifecycle.lifecycle.addObserver(chineseToEnglishTranslator)
    }

    fun close() {
        if (BuildConfig.DEBUG) {
            chineseToEnglishTranslator.close()
        } else {
            chineseToEnglishTranslator.closeQuietly()
        }
    }
}