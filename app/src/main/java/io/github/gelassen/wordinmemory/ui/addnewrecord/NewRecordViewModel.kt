package io.github.gelassen.wordinmemory.ui.addnewrecord

import android.app.Application
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.ml.PlainTranslator
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.network.Response
import io.github.gelassen.wordinmemory.repository.NetworkRepository
import io.github.gelassen.wordinmemory.repository.StorageRepository
import io.github.gelassen.wordinmemory.ui.dashboard.StateFlag
import io.github.gelassen.wordinmemory.utils.Validator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject

data class Model(
    val toTranslate: String = "",
    val sentenceInWordsForTranslation: Queue<String> = ConcurrentLinkedQueue(),
    val sentenceInWordsWithTranslation: List<Pair<String, String>> = mutableListOf(),
    val isLoading: Boolean = false,
    val errors: List<String> = mutableListOf(),
    val messages: List<String> = mutableListOf(),
    val status: StateFlag = StateFlag.NONE
)
class NewRecordViewModel
    @Inject constructor(
        val app: Application,
        val networkRepository: NetworkRepository,
        val storageRepository: StorageRepository,
        val translator: PlainTranslator
    )
        : AndroidViewModel(app) {

    private val state: MutableStateFlow<Model> = MutableStateFlow(Model())
    val uiState: StateFlow<Model> = state
        .asStateFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, state.value)

    private val validator = Validator()

    val wordToTranslate: ObservableField<String> = ObservableField<String>("")

    val translation: ObservableField<String> = ObservableField<String>("")

    fun start() {
        viewModelScope.launch {
//            async { splitSentenceIntoWords() }
            // FIXME async block doesn't spawn a thread and doesn't guarantee order of execution
            splitSentenceIntoWords()
            getTranslationForEachWord()
        }
    }

    fun addItem() {
        Log.d(App.TAG, "Add item has started")
        viewModelScope.launch {
            Log.d(App.TAG, "Add item coroutine has started")
            if (validator.isAllowedWordOrSentence(wordToTranslate.get()!!, translation.get()!!)) {
                val subject = SubjectToStudy(
                    0,
                    wordToTranslate.get()!!,
                    translation = translation.get()!!,
                    isCompleted = false,
                    tutorCounter = 0
                )
                storageRepository.saveSubject(subject)
            } else {
                addError("You can not add an empty word or word without translation")
            }
            wordToTranslate.set("")
            translation.set("")
        }
    }

    fun updateItem(subject: SubjectToStudy) {
        viewModelScope.launch {
            val subj = SubjectToStudy(
                uid = subject.uid,
                wordToTranslate.get()!!,
                translation.get()!!,
                subject.isCompleted,
                subject.tutorCounter
            )
            storageRepository.saveSubject(subj)
            wordToTranslate.set("")
            translation.set("")
        }
    }

    private fun getTranslationForEachWord() {
        Log.d(App.TAG, "[2]. getTranslationForEachWord()")
        processWordsInQueue()
    }

    private fun processWordsInQueue() {
        viewModelScope.launch {
            while(true) {
                delay(1000L)
                if (state.value.sentenceInWordsForTranslation.isEmpty()) {
                    continue
                } else {
                    async { translate(state.value.sentenceInWordsForTranslation.poll()!!) }
                }
            }
        }
    }

    private fun translate(word: String) {
        translator.translateChineseText(word, object: PlainTranslator.ITranslationListener {
            override fun onTranslationSuccess(translatedText: String) {
                state.update { state ->
                    val newList = state.sentenceInWordsWithTranslation.plus(Pair(word, translatedText))
                    state.copy(sentenceInWordsWithTranslation = newList)
                }
                Log.d(App.TAG, "Model state after translation feedback trigger ${state.value}")
            }

            override fun onTranslationFailed(exception: Exception) {
                // it should never happen, at this product version there is no right handler for it
                Log.e(App.TAG, "onTranslationFailed for word $word", exception)
            }

            override fun onModelDownloaded() {
                TODO("Not yet implemented")
            }

            override fun onModelDownloadFail(exception: Exception) {
                TODO("Not yet implemented")
            }

        } )
    }

    private fun splitSentenceIntoWords() {
        viewModelScope.launch {
            Log.d(App.TAG, "[1]. splitSentenceIntoWords()")
            Log.d(App.TAG, "wordToTranslate ${wordToTranslate.get()}")
            if (wordToTranslate.get()!!.isEmpty()) {
                state.update { state -> state.copy(errors = state.errors.plus("Translation field should not be empty. Please enter text on Chinese"))}
                return@launch
            }
            val response = networkRepository.splitChineseSentenceIntoWords(wordToTranslate.get()!!)
            when (response) {
                is Response.Data -> { processResponse(response) }
                is Response.Error -> { processErrorResponse(response) }
            }
        }
    }

    private fun processResponse(response: Response.Data<List<List<String>>>) {
        if (isNotValidResponse(response)) {
            val errorMsg = "Received data from backend either empty or has more than one record"
            state.update { state -> state.copy(isLoading = false, errors = state.errors.plus(errorMsg)) }
        } else {
            val data = ConcurrentLinkedQueue<String>()//mutableListOf<Pair<String, String>>()
            for (item in response.data.get(0)) {
                data.add(item) // sentence is split to words, but not translated yet; live translation as empty string ""
            }
            state.update { state -> state.copy(isLoading = false, sentenceInWordsForTranslation = data) }
        }
    }

    private fun isNotValidResponse(response: Response.Data<List<List<String>>>): Boolean {
        return response.data.isEmpty()
                || response.data.get(0).isEmpty()
                || response.data.size > 1
    }

    private fun processErrorResponse(response: Response.Error) {
        val errorMsg = when (response) {
            is Response.Error.Message -> { response.msg }
            is Response.Error.Exception -> { "Failed to classify text with error" }
        }
        state.update { state -> state.copy(isLoading = false, errors = state.errors.plus(errorMsg)) }
    }

    private fun addError(msg: String) {
        state.update { state ->
            state.copy(errors = state.errors.plus(msg))
        }
    }

}