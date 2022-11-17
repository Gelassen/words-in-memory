package io.github.gelassen.wordinmemory.ui.dashboard

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.repository.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Model(
    val data: List<SubjectToStudy> = mutableListOf(),
    val isLoading: Boolean = false,
    val errors: List<String> = emptyList(),
    val status: StateFlag = StateFlag.NONE
)

enum class StateFlag {
    NONE,
    DATA
}

class DashboardViewModel
    @Inject constructor(
        val storageRepository: StorageRepository
    )
    : ViewModel() {

    private val state: MutableStateFlow<Model> = MutableStateFlow(Model())
    val uiState: StateFlow<Model> = state
        .asStateFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, state.value)

    val wordToTranslate: ObservableField<String> = ObservableField<String>("")

    val translation: ObservableField<String> = ObservableField<String>("")

    private var filterRequestJob: Job? = null

    fun addItem() {
        viewModelScope.launch {
            val subject = SubjectToStudy(
                uid = 0,
                wordToTranslate.get()!!,
                translation.get()!!,
                false
            )
            storageRepository.saveSubject(subject)
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
}