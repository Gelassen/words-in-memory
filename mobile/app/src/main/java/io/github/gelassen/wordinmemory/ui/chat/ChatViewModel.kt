package io.github.gelassen.wordinmemory.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.model.ChatMessage
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.network.ChatCompletionMessage
import io.github.gelassen.wordinmemory.repository.ChatRepository
import io.github.gelassen.wordinmemory.repository.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    val vocabularyLoaded: Boolean = false,
    val lastSessionNewWords: Int = 0,
    val sessionClosing: Boolean = false
)

/**
 * Practice chat ViewModel.
 *
 * Call [startSession] every time the chat screen is shown.
 * Call [endSession] when leaving (toolbar Back or system Back).
 */
class ChatViewModel @Inject constructor(
    application: Application,
    private val storageRepository: StorageRepository,
    private val chatRepository: ChatRepository
) : AndroidViewModel(application) {

    companion object {
        private const val MAX_VOCAB_ITEMS = 40
        private const val MIN_NEW_WORDS = 10
        private const val MAX_NEW_WORDS = 20
        private const val TAG = "ChatViewModel"
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var vocabularySnapshot: List<SubjectToStudy> = emptyList()
    private var allKnownToTranslate: Set<String> = emptySet()
    private val conversationHistory = mutableListOf<ChatCompletionMessage>()
    private var systemPrompt: String = ""
    private var sessionEnded: Boolean = false
    private var sessionStarted: Boolean = false
    private var endSessionJob: Job? = null

    private val gson = Gson()

    /**
     * Must be called from ChatFragment.onViewCreated so a repeated visit
     * resets state (activity-scoped ViewModel otherwise keeps sessionEnded=true).
     */
    fun startSession() {
        if (sessionStarted && !sessionEnded && conversationHistory.isNotEmpty()) {
            return
        }
        sessionStarted = true
        sessionEnded = false
        endSessionJob?.cancel()
        endSessionJob = null
        conversationHistory.clear()
        systemPrompt = ""
        _uiState.value = ChatUiState()
        loadVocabularyAndStart()
    }

    private fun loadVocabularyAndStart() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val all = withContext(Dispatchers.IO) {
                    storageRepository.getSubjectsNonFlow()
                }
                allKnownToTranslate = all
                    .map { normalizeKey(it.toTranslate) }
                    .toSet()

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
                    sendToModel(
                        kickoffUserText = "Начни короткую дружелюбную практику: поздоровайся и " +
                                "задай первый вопрос, используя слова из словаря пользователя. " +
                                "Каждую китайскую фразу пиши в формате: 汉字 (pīnyīn) — русский."
                    )
                }
            } catch (e: Exception) {
                Log.e(App.TAG, "$TAG Failed to load vocabulary", e)
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
        if (trimmed.isEmpty() || _uiState.value.isLoading || sessionEnded) return

        addUserMessage(trimmed)
        conversationHistory.add(ChatCompletionMessage(role = "user", content = trimmed))
        sendToModel()
    }

    /**
     * Extract 10–20 new vocabulary items and save them, then invoke [onFinished].
     * Idempotent: a second call is ignored.
     */
    fun endSession(onFinished: (() -> Unit)? = null) {
        if (sessionEnded) {
            Log.d(App.TAG, "$TAG endSession: already ended, saved=${_uiState.value.lastSessionNewWords}")
            onFinished?.invoke()
            return
        }

        val hasAssistantTurn = conversationHistory.any { it.role == "assistant" }
        if (!hasAssistantTurn) {
            Log.d(App.TAG, "$TAG endSession: no assistant turns, skip extraction")
            sessionEnded = true
            sessionStarted = false
            onFinished?.invoke()
            return
        }

        sessionEnded = true
        endSessionJob?.cancel()
        endSessionJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, sessionClosing = true, error = null) }
            var savedCount = 0
            try {
                val extractionPrompt = buildExtractionPrompt()
                val historyForRequest = conversationHistory.toList() +
                        ChatCompletionMessage(role = "user", content = extractionPrompt)

                Log.d(App.TAG, "$TAG endSession: requesting vocabulary extraction")

                when (val result = withContext(Dispatchers.IO) {
                    chatRepository.completeChat(
                        systemPrompt = systemPrompt,
                        history = historyForRequest
                    )
                }) {
                    is ChatRepository.Result.Success -> {
                        Log.d(App.TAG, "$TAG extraction raw (first 500): ${result.content.take(500)}")
                        var items = parseNewVocabulary(result.content)
                        if (items.isEmpty()) {
                            Log.w(App.TAG, "$TAG JSON parse empty — fallback from dialogue")
                            items = extractFromDialogueFallback()
                        }
                        savedCount = withContext(Dispatchers.IO) {
                            saveNewVocabulary(items)
                        }
                        _uiState.update { it.copy(lastSessionNewWords = savedCount) }
                        Log.d(App.TAG, "$TAG endSession: saved $savedCount items")
                        if (savedCount > 0) {
                            addAssistantMessage(
                                "Сессия завершена. В словарь добавлено новых записей: $savedCount."
                            )
                        } else {
                            addAssistantMessage(
                                "Сессия завершена. Новых слов для словаря не найдено."
                            )
                        }
                    }
                    is ChatRepository.Result.Error -> {
                        Log.e(App.TAG, "$TAG extraction failed: ${result.message}")
                        val items = extractFromDialogueFallback()
                        savedCount = withContext(Dispatchers.IO) {
                            saveNewVocabulary(items)
                        }
                        _uiState.update { it.copy(lastSessionNewWords = savedCount) }
                        if (savedCount > 0) {
                            addAssistantMessage(
                                "Сессия завершена. Из диалога сохранено записей: $savedCount."
                            )
                        } else {
                            addErrorMessage(
                                "Не удалось извлечь новые слова: ${result.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(App.TAG, "$TAG endSession exception", e)
                addErrorMessage("Ошибка при сохранении новых слов: ${e.message}")
            } finally {
                try {
                    withContext(Dispatchers.IO) {
                        bumpTutorCountersForPracticedWords()
                    }
                } catch (e: Exception) {
                    Log.e(App.TAG, "$TAG failed to bump tutorCounter", e)
                }
                sessionStarted = false
                _uiState.update { it.copy(isLoading = false, sessionClosing = false) }
                onFinished?.invoke()
            }
        }
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
                Log.e(App.TAG, "$TAG chat request failed", e)
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
Ты — дружелюбный репетитор китайского языка для русскоязычных студентов.
Reply ONLY with the dialogue or short feedback. Never show your thinking process, reasoning, or planning.
Keep answers short and natural.

ОБЯЗАТЕЛЬНЫЙ ФОРМАТ КИТАЙСКОГО В КАЖДОМ СООБЩЕНИИ:
• Каждое китайское слово или предложение пиши так:
  汉字 (pīnyīn) — русский перевод
• Пример: 你好 (nǐ hǎo) — привет
• Пример предложения: 我喜欢喝茶 (wǒ xǐhuān hē chá) — я люблю пить чай
• Не опускай ни иероглифы, ни пиньинь, ни русский перевод.

Правила практики:
1. Строй короткие естественные предложения и мини-диалоги, опираясь на словарь пользователя ниже.
2. Задавай вопросы, чтобы студент отвечал, используя эти слова.
3. Если студент ошибается — мягко поправь и дай правильный пример в формате 汉字 (pīnyīn) — русский.
4. Иногда вводи 1 новое полезное слово (тоже в тройном формате), опираясь на уже известные.
5. Можно кратко пояснять на русском; китайские фрагменты всегда в тройном формате.
6. Не устраивай длинных лекций — держи диалог живым и коротким.
7. Если словарь пуст — скажи об этом.

Словарь пользователя (используй его):
$vocabBlock
""".trimIndent()
    }

    private fun buildExtractionPrompt(): String {
        return """
Сессия практики закончена. По всему диалогу выше (И ответы ученика, И реплики учителя) составь список слов и предложений.

ОБЯЗАТЕЛЬНО учитывай сообщения ученика (role=user): если ученик писал по-китайски или смешанно — извлеки оттуда слова/фразы.
Также включи полезные слова и предложения из ответов учителя.

Для КАЖДОЙ единицы укажи:
- word_hanzi: слово иероглифами
- word_pinyin: пиньинь слова
- word_translation: перевод слова (русский или английский)
- sentence_hanzi: полное предложение/фраза из диалога, где слово встречалось (можно из реплики ученика)
- sentence_pinyin: пиньинь предложения
- sentence_translation: перевод предложения (русский или английский)
- source: "user" если взято из реплики ученика, "assistant" если из ответа учителя

Правила:
- Не выдумывай слова, которых не было в диалоге.
- Если ученик написал только по-русски без китайского — китайских единиц из этой реплики нет.
- 10–30 пунктов достаточно.

Ответь ТОЛЬКО JSON-массивом, без markdown и без пояснений:
[
  {
    "word_hanzi": "茶",
    "word_pinyin": "chá",
    "word_translation": "чай",
    "sentence_hanzi": "我喜欢喝茶。",
    "sentence_pinyin": "Wǒ xǐhuān hē chá.",
    "sentence_translation": "Я люблю пить чай.",
    "source": "user"
  }
]
""".trimIndent()
    }

    private data class NewVocabItem(
        @SerializedName(value = "word_hanzi", alternate = ["word", "hanzi", "词"])
        val wordHanzi: String? = null,
        @SerializedName(value = "word_pinyin", alternate = ["pinyin", "词拼音"])
        val wordPinyin: String? = null,
        @SerializedName(value = "word_translation", alternate = ["translation", "russian", "english", "ru", "en", "перевод"])
        val wordTranslation: String? = null,
        @SerializedName(value = "sentence_hanzi", alternate = ["sentence", "句子"])
        val sentenceHanzi: String? = null,
        @SerializedName(value = "sentence_pinyin", alternate = ["sentence_py", "拼音"])
        val sentencePinyin: String? = null,
        @SerializedName(value = "sentence_translation", alternate = ["sentence_russian", "sentence_english", "sentence_ru"])
        val sentenceTranslation: String? = null,
        @SerializedName(value = "source", alternate = ["from", "role"])
        val source: String? = null
    )

    private fun parseNewVocabulary(raw: String): List<NewVocabItem> {
        val json = extractJsonArray(raw) ?: run {
            Log.w(App.TAG, "$TAG no JSON array in model response")
            return emptyList()
        }
        return try {
            val type = object : TypeToken<List<NewVocabItem>>() {}.type
            val list: List<NewVocabItem> = gson.fromJson(json, type) ?: emptyList()
            list.mapNotNull { item ->
                val wordH = item.wordHanzi?.trim().orEmpty()
                val sentH = item.sentenceHanzi?.trim().orEmpty()
                val hanzi = sentH.ifEmpty { wordH }
                val pinyin = item.sentencePinyin?.trim().orEmpty()
                    .ifEmpty { item.wordPinyin?.trim().orEmpty() }
                val translation = item.sentenceTranslation?.trim().orEmpty()
                    .ifEmpty { item.wordTranslation?.trim().orEmpty() }
                if (hanzi.isEmpty() || translation.isEmpty()) {
                    null
                } else {
                    NewVocabItem(
                        wordHanzi = wordH.ifEmpty { null },
                        wordPinyin = item.wordPinyin,
                        wordTranslation = item.wordTranslation,
                        sentenceHanzi = hanzi,
                        sentencePinyin = pinyin.ifEmpty { "—" },
                        sentenceTranslation = translation,
                        source = item.source
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(App.TAG, "$TAG JSON parse error: ${e.message}\nraw=$json", e)
            emptyList()
        }
    }

    private fun extractJsonArray(raw: String): String? {
        var text = raw
            .replace(Regex("```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("```"), "")
            .trim()
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start < 0 || end <= start) return null
        return text.substring(start, end + 1)
    }

    /**
     * Fallback: pull Chinese / triples from BOTH user and assistant messages.
     */
    private fun extractFromDialogueFallback(): List<NewVocabItem> {
        val triplePattern = Regex(
            "([\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF，。！？、\\s]{1,40}?)" +
                    "\\s*[（(]\\s*([^）)]+?)\\s*[）)]\\s*[—–\\-－]\\s*([^\\n。；;]{2,80})"
        )
        val bareHanPattern = Regex("[\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF]{2,}")
        val found = linkedMapOf<String, NewVocabItem>()

        fun addItem(hanzi: String, pinyin: String, translation: String, source: String) {
            val h = hanzi.trim()
            if (h.isEmpty()) return
            val key = normalizeKey("$h / $pinyin")
            if (key in found) return
            found[key] = NewVocabItem(
                wordHanzi = h.filter { ch -> isHanChar(ch) }.take(8),
                sentenceHanzi = h,
                sentencePinyin = pinyin.ifEmpty { "—" },
                sentenceTranslation = translation.ifEmpty { "(from $source message)" },
                source = source
            )
        }

        for (msg in conversationHistory) {
            val source = msg.role
            for (m in triplePattern.findAll(msg.content)) {
                addItem(
                    hanzi = m.groupValues[1],
                    pinyin = m.groupValues[2].trim(),
                    translation = m.groupValues[3].trim(),
                    source = source
                )
                if (found.size >= MAX_NEW_WORDS * 2) break
            }
            if (msg.role == "user") {
                for (m in bareHanPattern.findAll(msg.content)) {
                    addItem(
                        hanzi = m.value,
                        pinyin = "—",
                        translation = "",
                        source = "user"
                    )
                    if (found.size >= MAX_NEW_WORDS * 2) break
                }
            }
            if (found.size >= MAX_NEW_WORDS * 2) break
        }
        Log.d(
            App.TAG,
            "$TAG fallback extracted ${found.size} items " +
                    "(user=${found.values.count { it.source == "user" }}, " +
                    "assistant=${found.values.count { it.source == "assistant" }})"
        )
        return found.values.toList()
    }

    /**
     * Increment tutorCounter for words offered in this session (vocabularySnapshot).
     */
    private suspend fun bumpTutorCountersForPracticedWords() {
        if (vocabularySnapshot.isEmpty()) {
            Log.d(App.TAG, "$TAG bumpTutorCounters: empty snapshot, skip")
            return
        }
        vocabularySnapshot.forEach { it.tutorCounter++ }
        storageRepository.saveSubject(*vocabularySnapshot.toTypedArray())
        Log.i(
            App.TAG,
            "$TAG bumpTutorCounters: +1 for ${vocabularySnapshot.size} items " +
                    "(examples: ${vocabularySnapshot.take(3).map { "${it.toTranslate}->${it.tutorCounter}" }})"
        )
    }

    /**
     * Filter against full DB, keep at most [MAX_NEW_WORDS], single batch insert.
     */
    private suspend fun saveNewVocabulary(items: List<NewVocabItem>): Int {
        if (items.isEmpty()) {
            Log.d(App.TAG, "$TAG saveNewVocabulary: empty list")
            return 0
        }

        val known = try {
            storageRepository.getSubjectsNonFlow()
        } catch (e: Exception) {
            Log.e(App.TAG, "$TAG refresh known failed", e)
            emptyList()
        }
        val knownKeys = known.map { normalizeKey(it.toTranslate) }.toSet()
        val knownHanziChunks = known.map { it.toTranslate.substringBefore(" /").trim() }.toSet()

        val candidates = linkedMapOf<String, SubjectToStudy>()
        for (item in items) {
            val wordH = item.wordHanzi?.trim().orEmpty()
            val hanzi = item.sentenceHanzi?.trim().orEmpty()
            val pinyin = item.sentencePinyin?.trim().orEmpty()
            val translation = item.sentenceTranslation?.trim().orEmpty()
            if (hanzi.isEmpty() || translation.isEmpty()) continue

            val toTranslate = when {
                wordH.isNotEmpty() && pinyin.isNotEmpty() && pinyin != "—" ->
                    "$wordH / ${item.wordPinyin?.trim().orEmpty().ifEmpty { pinyin }} ｜ $hanzi / $pinyin"
                pinyin.isNotEmpty() && pinyin != "—" ->
                    "$hanzi / $pinyin"
                else -> hanzi
            }
            val key = normalizeKey(toTranslate)
            if (key in knownKeys) continue
            if (hanzi in knownHanziChunks) continue
            if (wordH.isNotEmpty() && wordH in knownHanziChunks && hanzi == wordH) continue
            if (key in candidates) continue

            candidates[key] = SubjectToStudy(
                uid = 0,
                toTranslate = toTranslate,
                translation = translation,
                isCompleted = false,
                isRedundant = false,
                tutorCounter = 0
            )
        }

        val toSave = candidates.values.take(MAX_NEW_WORDS)
        if (toSave.isEmpty()) {
            Log.d(App.TAG, "$TAG nothing new after DB filter (from ${items.size} model items)")
            return 0
        }

        storageRepository.saveSubject(*toSave.toTypedArray())
        allKnownToTranslate = allKnownToTranslate + toSave.map { normalizeKey(it.toTranslate) }.toSet()
        Log.i(App.TAG, "$TAG SAVED ${toSave.size} subjects (batch): ${toSave.map { it.toTranslate }}")
        return toSave.size
    }

    /**
     * Han / CJK ideograph check without API 24+ UnicodeScript (minSdk 21).
     */
    private fun isHanChar(ch: Char): Boolean {
        val code = ch.toInt()
        return code in 0x4E00..0x9FFF ||
                code in 0x3400..0x4DBF ||
                code in 0xF900..0xFAFF
    }

    private fun normalizeKey(s: String): String =
        s.trim().lowercase().replace(Regex("\\s+"), " ")

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
