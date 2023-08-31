package io.github.gelassen.wordinmemory.ui.addnewrecord

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gelassen.wordinmemory.network.Response
import io.github.gelassen.wordinmemory.repository.NetworkRepository
import io.github.gelassen.wordinmemory.ui.dashboard.StateFlag
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Model(
    val toTranslate: String = "",
    val sentenceInWordsWithTranslation: List<Pair<String, String>> = emptyList(),
    val isLoading: Boolean = false,
    val errors: List<String> = mutableListOf(),
    val messages: List<String> = mutableListOf(),
    val status: StateFlag = StateFlag.NONE
)
class NewRecordViewModel
    @Inject constructor(
        val app: Application,
        val networkRepository: NetworkRepository
    )
        : AndroidViewModel(app) {

    private val state: MutableStateFlow<Model> = MutableStateFlow(Model())
    val uiState: StateFlow<Model> = state
        .asStateFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, state.value)

    @OptIn(FlowPreview::class)
    fun start() {
        viewModelScope.launch {
            // TODO split sentence on separate words
            // TODO get translation for each word
        }
    }

    private fun splitSentenceIntoWords() {
        viewModelScope.launch {
            val response = networkRepository.splitChineseSentenceIntoWords(state.value.toTranslate)
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
            val data = mutableListOf<Pair<String, String>>()
            for (item in response.data.get(0)) {
                data.add(Pair(item, "")) // sentence is split to words, but not translated yet; live translation as empty string ""
            }
            state.update { state -> state.copy(isLoading = false, sentenceInWordsWithTranslation = data) }
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

}