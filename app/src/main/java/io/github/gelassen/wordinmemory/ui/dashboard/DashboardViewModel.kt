package io.github.gelassen.wordinmemory.ui.dashboard

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.backgroundjobs.BackupVocabularyWorker
import io.github.gelassen.wordinmemory.backgroundjobs.BaseWorker
import io.github.gelassen.wordinmemory.backgroundjobs.RestoreVocabularyWorker
import io.github.gelassen.wordinmemory.backgroundjobs.getWorkRequest
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.repository.StorageRepository
import io.github.gelassen.wordinmemory.storage.AppQuickStorage
import io.github.gelassen.wordinmemory.utils.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class Model(
    val data: List<SubjectToStudy> = mutableListOf(),
    val isLoading: Boolean = false,
    val counter: Int = 0,
    val errors: List<String> = mutableListOf(),
    val messages: List<String> = mutableListOf(),
    val status: StateFlag = StateFlag.NONE
)

enum class StateFlag {
    NONE,
    DATA,
    TUTORING_PART_ONE,
    TUTORING_PART_TWO
}

class DashboardViewModel
    @Inject constructor(
        val app: Application,
        val storageRepository: StorageRepository
    )
    : AndroidViewModel(app) {

    companion object {
        const val REQUIRED_AMOUNT_OF_ITEMS_FOR_TUTORING = 10
    }

    private val state: MutableStateFlow<Model> = MutableStateFlow(Model())
    val uiState: StateFlow<Model> = state
        .asStateFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, state.value)

    val wordToTranslate: ObservableField<String> = ObservableField<String>("")

    val translation: ObservableField<String> = ObservableField<String>("")

    private var filterRequestJob: Job? = null
    private val validator = Validator()

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

    override fun onCleared() {
        Log.d(App.TAG, "${this.javaClass.simpleName} onCleared() call")
        super.onCleared()
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
                    Log.d(App.TAG, "[showAll] show all result (count: ${it.size}) $it")
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
                    Log.d(App.TAG, "[showAll] show not completed only  result (count: ${it.size}) $it")
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
    fun backupVocabulary(uriToBackupDataFile: Uri) {
        viewModelScope.launch {
            val workManager = WorkManager.getInstance(app)
            val workRequest = workManager.getWorkRequest<BackupVocabularyWorker>(
                BackupVocabularyWorker.Builder.build(uriToBackupDataFile)
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

    fun restoreVocabulary(backupUri: Uri) {
        viewModelScope.launch {
            val workManager = WorkManager.getInstance(app)
            val workRequest = workManager.getWorkRequest<RestoreVocabularyWorker>(
                RestoreVocabularyWorker.Builder.build(backupUri)
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
//                            val msg = app.getString(R.string.msg_database_backup_ok)
//                            state.update { state -> state.copy(messages = state.messages.plus(msg)) }
                            // no op, data should appear in list
                            Log.d(App.TAG, "Command is successfully finished")
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

    suspend fun showPartOneDailyPractice() {
        /* when we show only subset based on sql query, observer doesn't respond on changes in database */
/*        withContext(Dispatchers.IO) {
            val dailyPractice = storageRepository.getDailyPractice()
            state.update { state ->
                state.copy(isLoading = false, data = dailyPractice)
            }
        }*/
        Log.d(App.TAG, "[start] showDailyPractice")
        val itemsForPracticeAmount = 10
        filterRequestJob?.cancel()
        filterRequestJob = viewModelScope.launch {
            Log.d(App.TAG, "${this.javaClass.simpleName} showDailyPractice()::viewModelScope.launch {}")
            storageRepository
                .getNonCompleteSubjectsOnly()
                .cancellable()
                .map { it -> it.sortedBy { item -> item.tutorCounter } }
                /*.take(itemsForPracticeAmount)*/
                .flowOn(Dispatchers.IO)
                .collect { it ->
                    Log.d(App.TAG, "[showAll] show not completed only  result (count: ${it.size}) $it")
                    state.update { state ->
                        state.copy(data = it.take(itemsForPracticeAmount), status = StateFlag.TUTORING_PART_ONE)
                    }
                }
        }
    }

    suspend fun showPartTwoDailyPractice() {
        /* when we show only subset based on sql query, observer doesn't respond on changes in database */
        /*        withContext(Dispatchers.IO) {
                    val dailyPractice = storageRepository.getDailyPractice()
                    state.update { state ->
                        state.copy(isLoading = false, data = dailyPractice)
                    }
                }*/
        Log.d(App.TAG, "[start] showPartTwoDailyPractice")
        val itemsForPracticeAmount = 10
        filterRequestJob?.cancel()
        filterRequestJob = viewModelScope.launch {
            storageRepository
                .getCompleteSubjectsOnly()
                .cancellable()
                .map { it -> it.sortedBy { item -> item.tutorCounter } }
                .map { it -> revertBackTranslationAndSubjectToTranslate(it) }
                /*.take(itemsForPracticeAmount)*/
                .flowOn(Dispatchers.IO)
                .collect { it ->
                    Log.d(App.TAG, "[showAll] show not completed only  result (count: ${it.size}) $it")
                    state.update { state ->
                        state.copy(data = it.take(itemsForPracticeAmount), status = StateFlag.TUTORING_PART_TWO)
                    }
                }
        }
    }

    suspend fun completePartOneDailyPractice(
        activity: Activity,
        dataset: MutableList<SubjectToStudy>
    ) {
        // race condition on dataset was there, deep copy solved an issue
        // https://stackoverflow.com/questions/34697828/parallel-operations-on-kotlin-collections
        // https://stackoverflow.com/questions/45575516/kotlin-process-collection-in-parallel
        withContext(Dispatchers.IO) {
            AppQuickStorage().saveLastTrainedTime(activity, System.currentTimeMillis())
            dataset.forEach { it.tutorCounter++ }
            storageRepository.saveSubject(*dataset.map { it }.toTypedArray())
        }
    }

    suspend fun completePartTwoDailyPractice(
        activity: Activity,
        dataset: MutableList<SubjectToStudy>
    ) {
        val list = revertBackTranslationAndSubjectToTranslate(dataset.toList())
        completePartOneDailyPractice(activity, list.toMutableList())
    }

    fun clearState() {
        state.update { state ->
            state.copy(data = emptyList(), counter = 0)
        }
    }

    private fun revertBackTranslationAndSubjectToTranslate(dataset: List<SubjectToStudy>): List<SubjectToStudy> {
        val data = dataset.map { it ->
            val tmp = it.translation
            it.translation = it.toTranslate
            it.toTranslate = tmp
            it
        }
        return data
    }

    fun shallSkipPartOneTutoringScreen(): Boolean {
        return state.value.status == StateFlag.TUTORING_PART_ONE
                && state.value.data.isEmpty()
    }

    fun shallSkipPartTwoTutoringScreen(): Boolean {
        return state.value.status == StateFlag.TUTORING_PART_TWO
                && state.value.data.isEmpty()
    }

    fun areNotEnoughWordsForPractice(): Boolean {
        return state.value.data.isNotEmpty()
                && state.value.data.size < REQUIRED_AMOUNT_OF_ITEMS_FOR_TUTORING
    }


}