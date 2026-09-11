package io.github.gelassen.wordinmemory.ui.chat

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.model.ChatMessage
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.network.GeminiApi
import io.github.gelassen.wordinmemory.network.GeminiContent
import io.github.gelassen.wordinmemory.network.GeminiGenerationConfig
import io.github.gelassen.wordinmemory.network.GeminiPart
import io.github.gelassen.wordinmemory.network.GeminiRequest
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
    private val geminiApi: GeminiApi,
    private val sharedPreferences: SharedPreferences
) : AndroidViewModel(application) {

    companion object {
        /** Preference key — same style as server_ip / server_port */
        const val PREF_GEMINI_API_KEY = "gemini_api_key"
        /** Legacy key from OpenAI migration; still read as fallback */
        private const val PREF_OPENAI_API_KEY_LEGACY = "openai_api_key"

        /**
         * Models to try in order. First match that exists for the API key wins.
         * 404 = model not available for this key/region → try next.
         */
        private val GEMINI_MODELS = listOf(
            "gemini-2.5-flash-lite",
            "gemini-2.5-flash",
            "gemini-flash-lite-latest",
            "gemini-flash-latest",
            "gemini-3.5-flash-lite",
            "gemini-3.5-flash",
            "gemini-3.8-flash"
        )

        private const val MAX_VOCAB_ITEMS = 40
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var vocabularySnapshot: List<SubjectToStudy> = emptyList()
    /** Gemini roles: "user" | "model" */
    private val conversationHistory = mutableListOf<GeminiContent>()
    private var systemPrompt: String = ""

    init {
        loadVocabularyAndStart()
    }

    private fun getApiKey(): String {
        val gemini = sharedPreferences.getString(PREF_GEMINI_API_KEY, "")?.trim().orEmpty()
        if (gemini.isNotEmpty()) return gemini
        // fallback if user still has old preference name
        return sharedPreferences.getString(PREF_OPENAI_API_KEY_LEGACY, "")?.trim().orEmpty()
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
                    // Kick off the first tutor message
                    sendToModel()
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
        conversationHistory.add(
            GeminiContent(role = "user", parts = listOf(GeminiPart(trimmed)))
        )
        sendToModel()
    }

    private fun sendToModel() {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            addAssistantMessage(
                "⚠ API-ключ Google Gemini не задан.\n\n" +
                        "1. Откройте https://aistudio.google.com/apikey\n" +
                        "2. Создайте бесплатный API key\n" +
                        "3. Вставьте его в Настройки → «Gemini API Key»\n\n" +
                        "После сохранения вернитесь в чат и отправьте сообщение снова."
            )
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // If history is empty, ask the model to start the dialogue
                val startPrompt =
                    "Начни короткую дружелюбную практику: поздоровайся и " +
                            "задай первый вопрос, используя слова из словаря пользователя."

                val contents = if (conversationHistory.isEmpty()) {
                    // Fold system instructions into the first user message for max compatibility
                    val firstText = if (systemPrompt.isNotBlank()) {
                        systemPrompt + "\n\n---\n\n" + startPrompt
                    } else {
                        startPrompt
                    }
                    listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(firstText))
                        )
                    )
                } else {
                    conversationHistory.toList()
                }

                val request = GeminiRequest(
                    // Prefer contents-only; system_instruction optional (null = omitted if we pass null)
                    systemInstruction = null,
                    contents = contents,
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.75,
                        maxOutputTokens = 2048
                    )
                )

                data class Attempt(val response: retrofit2.Response<io.github.gelassen.wordinmemory.network.GeminiResponse>, val model: String, val errBody: String?)

                val attempt = withContext(Dispatchers.IO) {
                    var last: Attempt? = null
                    for (model in GEMINI_MODELS) {
                        val url =
                            "https://generativelanguage.googleapis.com/v1beta/models/" +
                                    model +
                                    ":generateContent?key=" +
                                    java.net.URLEncoder.encode(apiKey, "UTF-8")
                        Log.d(App.TAG, "Trying Gemini model: $model")
                        val resp = geminiApi.generateContent(url = url, body = request)
                        val errBody = if (!resp.isSuccessful) {
                            try { resp.errorBody()?.string() } catch (_: Exception) { null }
                        } else null
                        if (errBody != null) {
                            Log.e(App.TAG, "Gemini $model -> ${resp.code()}: $errBody")
                        }
                        last = Attempt(resp, model, errBody)
                        if (resp.code() != 404 && resp.code() != 503) break
                    }
                    last!!
                }

                val response = attempt.response
                val usedModel = attempt.model

                if (response.isSuccessful) {
                    val body = response.body()
                    val content = body?.candidates
                        ?.firstOrNull()
                        ?.content
                        ?.parts
                        ?.joinToString("") { it.text }
                        ?.trim()

                    if (!content.isNullOrEmpty()) {
                        conversationHistory.add(
                            GeminiContent(
                                role = "model",
                                parts = listOf(GeminiPart(content))
                            )
                        )
                        addAssistantMessage(content)
                    } else {
                        val err = body?.error?.message ?: "Пустой ответ от Gemini"
                        addErrorMessage(err)
                    }
                } else {
                    val errBody = attempt.errBody
                    Log.e(App.TAG, "Gemini error ${response.code()} model=$usedModel: $errBody")
                    val msg = when (response.code()) {
                        400 -> "Некорректный запрос (400). ${errBody ?: "Проверьте ключ."}"
                        401, 403 -> "Неверный или отозванный API-ключ Gemini. Проверьте Настройки."
                        404 -> "Модель недоступна (404, $usedModel). ${errBody ?: "См. Logcat."}"
                        429 -> "Лимит запросов Gemini (429). Подождите или проверьте квоту в AI Studio."
                        503 -> "Сервис Gemini временно недоступен (503). Попробуйте через минуту."
                        else -> "Ошибка API (${response.code()}, model=$usedModel). ${errBody ?: ""}"
                    }
                    addErrorMessage(msg)
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
