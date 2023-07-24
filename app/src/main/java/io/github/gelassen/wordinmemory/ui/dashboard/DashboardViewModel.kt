package io.github.gelassen.wordinmemory.ui.dashboard

import android.app.Application
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.Operation.State.SUCCESS
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.backgroundjobs.BackupVocabularyWorker
import io.github.gelassen.wordinmemory.backgroundjobs.BaseWorker
import io.github.gelassen.wordinmemory.backgroundjobs.getWorkRequest
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.repository.StorageRepository
import io.github.gelassen.wordinmemory.utils.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Model(
    val data: List<SubjectToStudy> = mutableListOf(),
    val isLoading: Boolean = false,
    val errors: List<String> = mutableListOf(),
    val messages: List<String> = mutableListOf(),
    val status: StateFlag = StateFlag.NONE
)

enum class StateFlag {
    NONE,
    DATA
}

class DashboardViewModel
    @Inject constructor(
        val app: Application,
        val storageRepository: StorageRepository
    )
    : AndroidViewModel(app) {

    private val state: MutableStateFlow<Model> = MutableStateFlow(Model())
    val uiState: StateFlow<Model> = state
        .asStateFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, state.value)

    val wordToTranslate: ObservableField<String> = ObservableField<String>("")

    val translation: ObservableField<String> = ObservableField<String>("")

    private var filterRequestJob: Job? = null
    private val validator = Validator()

    fun addItem() {
        viewModelScope.launch {
            if (validator.isAllowedWordOrSentence(wordToTranslate.get()!!, translation.get()!!)) {
                val subject = SubjectToStudy(
                    uid = 0,
                    wordToTranslate.get()!!,
                    translation.get()!!,
                    false
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
            val subject = SubjectToStudy(
                uid = subject.uid,
                wordToTranslate.get()!!,
                translation.get()!!,
                subject.isCompleted
            )
            storageRepository.saveSubject(subject)
            wordToTranslate.set("")
            translation.set("")
        }
    }

    fun updateItem(selectedSubject: SubjectToStudy, isComplete: Boolean) {
        viewModelScope.launch {
            selectedSubject.isCompleted = isComplete
            storageRepository
                .saveSubject(selectedSubject)
        }
    }

    fun showAll() {
        filterRequestJob?.cancel()
        filterRequestJob = viewModelScope.launch {
            storageRepository
                .getSubjects()
                .cancellable()
                .flowOn(Dispatchers.IO)
                .collect { it ->
                    state.update { state ->
                        state.copy(data = it, status = StateFlag.DATA)
                    }
                }
        }
    }

    fun showNonCompletedOnly() {
        filterRequestJob?.cancel()
        filterRequestJob = viewModelScope.launch {
            storageRepository
                .getNonCompleteSubjectsOnly()
                .cancellable()
                .flowOn(Dispatchers.IO)
                .collect { it ->
                    state.update { state ->
                        state.copy(data = it, status = StateFlag.DATA)
                    }
                }
        }
    }

    fun addError(msg: String) {
        state.update { state ->
            state.copy(errors = state.errors.plus(msg))
        }
    }

    fun removeError(error: String) {
        state.update { state ->
            state.copy(errors = state.errors.filter { it -> it != error })
        }
    }
    fun backupVocabulary() {
        viewModelScope.launch {
            val workManager = WorkManager.getInstance(app)
            val workRequest = workManager.getWorkRequest<BackupVocabularyWorker>(
                BackupVocabularyWorker.Builder.build()
            )
            workManager.enqueue(workRequest)
            workManager
                .getWorkInfoByIdLiveData(workRequest.id)
                .asFlow()
                .onStart { state.update { state -> state.copy(isLoading = false) } }
                .onCompletion { state.update { state -> state.copy(isLoading = false) } }
                .collect {
                    when(it.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val msg = app.getString(R.string.msg_database_backup_ok)
                            state.update { state -> state.copy(messages = state.messages.plus(msg)) }
                        }
                        WorkInfo.State.FAILED -> {
                            val errorMsg = it.outputData.keyValueMap.get(BaseWorker.Consts.KEY_ERROR_MSG) as String
                            state.update { state -> state.copy(messages = state.errors.plus(errorMsg) ) }
                        }
                        else -> { Log.d(App.TAG, "[${workRequest.javaClass.simpleName}] unexpected state on collect with state $it") }
                    }
                }

        }
    }
}