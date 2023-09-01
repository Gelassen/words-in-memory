package io.github.gelassen.wordinmemory.ml

import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.lang.Exception

class PlainTranslation(listener: ITranslationListener) {

    interface ITranslationListener {
        fun onTranslationSuccess(translatedText: String)
        fun onTranslationFailed(exception: Exception)
        fun onModelDownloaded()
        fun onModelDownloadFail(exception: Exception)
    }

    var chineseToEnglishTranslator: Translator

    init {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.CHINESE)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        chineseToEnglishTranslator = Translation.getClient(options)
        prepare(listener)
    }

    fun prepare(listener: ITranslationListener) {
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        chineseToEnglishTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener { listener.onModelDownloaded() }
            .addOnFailureListener { exception -> listener.onModelDownloadFail(exception)}
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
}