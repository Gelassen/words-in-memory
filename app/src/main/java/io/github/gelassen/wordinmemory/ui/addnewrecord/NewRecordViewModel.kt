package io.github.gelassen.wordinmemory.ui.addnewrecord

import android.app.Application
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.backgroundjobs.AddNewRecordWorker
import io.github.gelassen.wordinmemory.backgroundjobs.BaseWorker
import io.github.gelassen.wordinmemory.backgroundjobs.getWorkRequest
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.repository.StorageRepository
import io.github.gelassen.wordinmemory.ui.dashboard.StateFlag
import io.github.gelassen.wordinmemory.utils.Validator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject

data class Model(
    val toTranslate: String = "",
    val sentenceInWordsForTranslation: Queue<String> = ConcurrentLinkedQueue(),
    val sentenceInWordsWithTranslation: Queue<Pair<String, String>> = ConcurrentLinkedQueue(),
    val sentenceInWordsWithTranslationAndPinyin: Queue<Pair<String, String>> = ConcurrentLinkedQueue(),
    val isLoading: Boolean = false,
    val errors: List<String> = mutableListOf(),
    val messages: List<String> = mutableListOf(),
    val status: StateFlag = StateFlag.NONE
)
class NewRecordViewModel
    @Inject constructor(
        val app: Application,
        val storageRepository: StorageRepository
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
            if (!validator.isAllowedWordOrSentence(wordToTranslate.get()!!)) {
                addError(app.getString(R.string.msg_error_not_valid_new_record))
                return@launch
            }
            val text = wordToTranslate.get()!!
            val workManager = WorkManager.getInstance(app)
            val workRequest = workManager.getWorkRequest<AddNewRecordWorker>(AddNewRecordWorker.Builder.build(text))
            workManager.enqueue(workRequest)
            workManager
                .getWorkInfoByIdLiveData(workRequest.id)
                .asFlow()
                .onStart { state.update { state -> state.copy(isLoading = false) } }
                .onCompletion { state.update { state -> state.copy(isLoading = false) } }
                .collect {
                    when(it.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            Log.d(App.TAG, "AddNewRecordWorker is succeed")
                            val msg = app.getString(R.string.msg_database_backup_ok)
                            state.update { state -> state.copy(messages = state.messages.plus(msg)) }
                        }
                        WorkInfo.State.FAILED -> {
                            Log.d(App.TAG, "AddNewRecordWorker is failed")
                            val errorMsg = it.outputData.keyValueMap.get(BaseWorker.Consts.KEY_ERROR_MSG) as String
                            state.update { state -> state.copy(messages = state.errors.plus(errorMsg) ) }
                        }
                        else -> { Log.d(App.TAG, "[${workRequest.javaClass.simpleName}] unexpected state on collect with state $it") }
                    }
                }
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

    private fun addError(msg: String) {
        state.update { state ->
            state.copy(errors = state.errors.plus(msg))
        }
    }

    fun removeError(errorMsg: String) {
        state.update { state ->
            state.copy(errors = state.errors.filter { it != errorMsg })
        }
    }

}