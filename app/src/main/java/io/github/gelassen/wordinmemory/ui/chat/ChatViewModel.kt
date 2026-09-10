package io.github.gelassen.wordinmemory.ui.chat

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.model.ChatMessage
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.network.OpenAiApi
import io.github.gelassen.wordinmemory.network.OpenAiChatRequest
import io.github.gelassen.wordinmemory.network.OpenAiMessage
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

class ChatViewModel @Inject constructor(
    application: Application,
    private val storageRepository: StorageRepository,
    private val openAiApi: OpenAiApi,
    private val sharedPreferences: SharedPreferences
) : AndroidViewModel(application) {

    companion object {
        /** Same style as server_ip / server_port in preferences.xml */
        const val PREF_OPENAI_API_KEY = "openai_api_key"

        private const val MAX_VOCAB_ITEMS = 40
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var vocabularySnapshot: List<SubjectToStudy> = emptyList()
    private val conversationHistory = mutableListOf<OpenAiMessage>()

    init {
        loadVocabularyAndStart()
    }

    /** Reads key from Settings (SharedPreferences), same mechanism as backend server IP/port. */
    private fun getApiKey(): String {
        return sharedPreferences.getString(PREF_OPENAI_API_KEY, "")?.trim().orEmpty()
    }

    private fun loadVocabularyAndStart() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val all = withContext(Dispatchers.IO) {
                    storageRepository.getSubjectsNonFlow()
                }
                // Prefer non-completed + low tutorCounter, then fill with completed
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
                    val systemPrompt = buildSystemPrompt(prioritized)
                    conversationHistory.clear()
                    conversationHistory.add(OpenAiMessage(role = "system", content = systemPrompt))

                    // First proactive message from the tutor
                    sendToModel(
                        userVisibleText = null,
                        forceAssistantOnly = true
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
        conversationHistory.add(OpenAiMessage(role = "user", content = trimmed))
        sendToModel(userVisibleText = trimmed, forceAssistantOnly = false)
    }

    private fun sendToModel(userVisibleText: String?, forceAssistantOnly: Boolean) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            addAssistantMessage(
                "⚠ API-ключ OpenAI не задан.\n\n" +
                        "Откройте Настройки (Settings) → поле «OpenAI API Key» и вставьте ключ.\n" +
                        "Ключ: https://platform.openai.com/api-keys\n" +
                        "(у новых аккаунтов обычно есть бесплатные кредиты).\n\n" +
                        "После сохранения ключа вернитесь в чат и отправьте сообщение снова."
            )
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val request = OpenAiChatRequest(
                    model = "gpt-4o-mini",
                    messages = conversationHistory.toList(),
                    temperature = 0.75,
                    maxTokens = 600
                )

                val response = withContext(Dispatchers.IO) {
                    openAiApi.createChatCompletion(
                        authorization = "Bearer $apiKey",
                        body = request
                    )
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    val content = body?.choices?.firstOrNull()?.message?.content?.trim()
                    if (!content.isNullOrEmpty()) {
                        conversationHistory.add(OpenAiMessage(role = "assistant", content = content))
                        addAssistantMessage(content)
                    } else {
                        val err = body?.error?.message ?: "Пустой ответ от модели"
                        addErrorMessage(err)
                    }
                } else {
                    val errBody = response.errorBody()?.string()
                    Log.e(App.TAG, "OpenAI error ${response.code()}: $errBody")
                    addErrorMessage("Ошибка API (${response.code()}). Проверьте ключ в Настройках и лимиты.")
                }
            } catch (e: Exception) {
                Log.e(App.TAG, "Chat request failed", e)
                addErrorMessage("Сеть или сервер недоступны: ${e.message}")
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
Ты — дружелюбный репетитор иностранного языка (в основном китайского).
Твоя главная задача: помочь пользователю практиковать слова из его личного словаря.

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
