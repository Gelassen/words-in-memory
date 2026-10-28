package io.github.gelassen.wordinmemory.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.model.ChatMessage
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.network.ChatCompletionMessage
import io.github.gelassen.wordinmemory.repository.ChatRepository
import io.github.gelassen.wordinmemory.repository.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val vocabularyLoaded: Boolean = false
)

/**
 * UI-only ViewModel. No Retrofit / OkHttp / URL construction here —
 * all network goes through [ChatRepository].
 */
class ChatViewModel @Inject constructor(
    application: Application,
    private val storageRepository: StorageRepository,
    private val chatRepository: ChatRepository
) : AndroidViewModel(application) {

    companion object {
        private const val MAX_VOCAB_ITEMS = 40
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var vocabularySnapshot: List<SubjectToStudy> = emptyList()
    /** Roles: "user" | "assistant" (system is passed separately) */
    private val conversationHistory = mutableListOf<ChatCompletionMessage>()
    private var systemPrompt: String = ""

    init {
        loadVocabularyAndStart()
    }

    private fun loadVocabularyAndStart() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val all = withContext(Dispatchers.IO) {
                    storageRepository.getSubjectsNonFlow()
                }
                val prioritized = all
                    .filter { !it.isRedundant }
                    .sortedWith(
                        compareBy<SubjectToStudy> { it.isCompleted }
                            .thenBy { it.tutorCounter }
                    )
                    .take(MAX_VOCAB_ITEMS)

                vocabularySnapshot = prioritized
                _uiState.update { it.copy(vocabularyLoaded = true, isLoading = false) }

                if (prioritized.isEmpty()) {
                    addAssistantMessage(
                        "Словарь пока пуст. Добавьте несколько слов на главном экране, " +
                            "затем возвращайтесь — я буду строить предложения и диалоги на их основе."
                    )
                } else {
                    systemPrompt = buildSystemPrompt(prioritized)
                    conversationHistory.clear()
                    sendToModel(
                        kickoffUserText = "Начни короткую дружелюбную практику: поздоровайся и " +
                            "задай первый вопрос, используя слова из словаря пользователя."
                    )
                }
            } catch (e: Exception) {
                Log.e(App.TAG, "Failed to load vocabulary for chat", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Не удалось загрузить словарь: ${e.message}"
                    )
                }
            }
        }
    }

    fun sendUserMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _uiState.value.isLoading) return

        addUserMessage(trimmed)
        conversationHistory.add(ChatCompletionMessage(role = "user", content = trimmed))
        sendToModel()
    }

    private fun sendToModel(kickoffUserText: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val historyForRequest = if (kickoffUserText != null && conversationHistory.isEmpty()) {
                    listOf(ChatCompletionMessage(role = "user", content = kickoffUserText))
                } else {
                    conversationHistory.toList()
                }

                when (val result = withContext(Dispatchers.IO) {
                    chatRepository.completeChat(
                        systemPrompt = systemPrompt,
                        history = historyForRequest
                    )
                }) {
                    is ChatRepository.Result.Success -> {
                        if (kickoffUserText != null && conversationHistory.isEmpty()) {
                            conversationHistory.add(
                                ChatCompletionMessage(role = "user", content = kickoffUserText)
                            )
                        }
                        conversationHistory.add(
                            ChatCompletionMessage(role = "assistant", content = result.content)
                        )
                        addAssistantMessage(result.content)
                    }
                    is ChatRepository.Result.Error -> {
                        addErrorMessage(result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(App.TAG, "Chat request failed", e)
                addErrorMessage("Сеть или AI-сервер недоступны: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun buildSystemPrompt(vocab: List<SubjectToStudy>): String {
        val vocabBlock = vocab.joinToString(separator = "\n") { item ->
            val status = when {
                item.isCompleted -> "выучено"
                else -> "изучается (практик: ${item.tutorCounter})"
            }
            "- ${item.toTranslate} → ${item.translation} [$status]"
        }

        return """
Ты — дружелюбный репетитор иностранного языка (в основном китайского) для русскоязычных студентов.
Reply ONLY with the dialogue or short feedback. Never show your thinking process, reasoning, or planning.
Keep answers short and natural.

Правила:
1. Строй короткие естественные предложения и мини-диалоги, используя преимущественно слова из списка ниже.
2. Задавай вопросы, чтобы пользователь отвечал, используя эти слова.
3. Если пользователь ошибается — мягко поправь и дай правильный пример.
4. Иногда предлагай 1 новое слово (помечай его как новое), но опирайся на уже известные.
5. Отвечай на языке, удобном пользователю (русский / английский / смешанно с китайским).
6. Не устраивай длинных лекций — держи диалог живым и коротким.
7. Если словарь пуст — скажи об этом.

Словарь пользователя (используй его):
$vocabBlock
""".trimIndent()
    }

    private fun addUserMessage(text: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(
                    role = ChatMessage.ROLE_USER,
                    content = text
                )
            )
        }
    }

    private fun addAssistantMessage(text: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(
                    role = ChatMessage.ROLE_ASSISTANT,
                    content = text
                )
            )
        }
    }

    private fun addErrorMessage(text: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(
                    role = ChatMessage.ROLE_ASSISTANT,
                    content = text,
                    isError = true
                ),
                error = text
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
