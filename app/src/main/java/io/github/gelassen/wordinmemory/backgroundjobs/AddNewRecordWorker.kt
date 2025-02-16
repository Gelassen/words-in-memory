package io.github.gelassen.wordinmemory.backgroundjobs

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.BuildConfig
import io.github.gelassen.wordinmemory.backgroundjobs.AddNewRecordWorker.Companion.NON_INITIALISED
import io.github.gelassen.wordinmemory.backgroundjobs.pipline.IPipelineTask
import io.github.gelassen.wordinmemory.ml.PlainTranslator
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.network.Response
import io.github.gelassen.wordinmemory.repository.NetworkRepository
import io.github.gelassen.wordinmemory.repository.StorageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.pilgr.pipinyin.PiPinyin
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class Model(
    var dataByWords: Queue<String> = ConcurrentLinkedQueue(),
    var dataWithTranslation: Queue<Pair<String, String>> = ConcurrentLinkedQueue(),
    var dataset: MutableList<Pair<String, String>> = mutableListOf(),
    val counter: AtomicInteger = AtomicInteger(NON_INITIALISED)
)
class AddNewRecordWorker(
    val context: Context,
    val params: WorkerParameters,
    val translator: PlainTranslator,
    val networkRepository: NetworkRepository,
    val storageRepository: StorageRepository,
    val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseWorker(context, params) {

    object Builder {

        const val EXTRA_TO_TRANSLATE_RECORD = "EXTRA_TO_TRANSLATE_RECORD"

        fun build(record: String): Data {
            return workDataOf(EXTRA_TO_TRANSLATE_RECORD to record)
        }
    }

    companion object {
        const val EXTRA_OPERATIONS_COUNT = 2
        const val NON_INITIALISED = -1
    }

    private val piPinyin = PiPinyin(context)
    private val model = Model()
    private var result = Result.failure()

    private fun disposeResources() {
        piPinyin.recycle()
        // we shouldn't close translator instance till the end of app's lifecycle
    }

    override suspend fun doWork(): Result {
        val record = inputData.getString(Builder.EXTRA_TO_TRANSLATE_RECORD)!!
        val pipeline = mutableListOf( /* the order of tasks in the pipeline is matter */
            WordSegmentationTask(record),
            TranslateTask(),
            AddPinyinTask(),
            PostProcessDataTask(record),
            StorageTask()
        )
        withContext(backgroundDispatcher) {
            for (task in pipeline) {
                task.process()
            }
        }
        disposeResources()
        return result
    }

    private fun initiateCounter() {
        model.counter.set(model.dataByWords.size + EXTRA_OPERATIONS_COUNT)
        Log.d(App.TAG, "Counter is initiated ${model.counter}")
    }

    private fun thereIsStillWork(): Boolean {
        return model.counter.get() != 0
    }

    /**
     * First task does not require extra checks, so there is no similar method. However it is
     * good to preserve order in names to avoid cognitive mess with terms.
     * */
    private fun isTaskTwoFinished(): Boolean {
        return model.counter.get() != EXTRA_OPERATIONS_COUNT
    }

    private fun isTaskThreeFinished(): Boolean {
        return model.counter.get() != EXTRA_OPERATIONS_COUNT.minus(1)
    }

    private fun debugCounterPrintln() {
        Log.d(App.TAG, "Counter number ${model.counter.get()}")
    }

    private inner class WordSegmentationTask(val record: String) : IPipelineTask {

        override suspend fun process(): IPipelineTask {
            Log.d(App.TAG, "[part 1] addNewRecord::splitSentenceIntoWords")
            val isFinished = AtomicBoolean(false)
            while (thereIsStillWork() && !isFinished.get()) {
                Log.d(App.TAG, "splitSentenceIntoWords inner loop. translator.isTranslationModelReady() ${translator.isTranslationModelReady()}")
                Thread.sleep(1000)
                // translation model will be required on the next step, but it would be better to wait it readiness here
                if (translator.isTranslationModelReady()) {
                    isFinished.set(true)
                    val response = networkRepository.splitChineseSentenceIntoWords(record)
                    when (response) {
                        is Response.Data -> { processResponse(response) }
                        is Response.Error -> { processErrorResponse(response) }
                    }
                } else {
                    continue
                }
            }
            return this
        }

        private fun processResponse(response: Response.Data<List<List<String>>>) {
            if (isNotValidResponse(response)) {
                val errorMsg = "Received data from backend either empty or has more than one record"
                val outputData = workDataOf(Consts.KEY_ERROR_MSG to errorMsg)
                result = Result.failure(outputData)
            } else {
                val data = ConcurrentLinkedQueue<String>()//mutableListOf<Pair<String, String>>()
                for (item in response.data.get(0)) {
                    data.add(item) // sentence is split to words, but not translated yet; live translation as empty string ""
                }
                model.dataByWords = data
                addWholeRecordAsExtraWord()
                initiateCounter()
            }
        }

        /**
         * In case of sentence in record, it would be good to have in vocabulary to. The best way so
         * far is to add it into dataset at the beginning of the pipeline
         * */
        private fun addWholeRecordAsExtraWord() {
            model.dataByWords.add(record)
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
            val outputData = workDataOf(Consts.KEY_ERROR_MSG to errorMsg)
            result = Result.failure(outputData)
        }

    }

    private inner class TranslateTask: IPipelineTask {

        override suspend fun process(): IPipelineTask {
            while (isTaskTwoFinished()) {
                Log.d(App.TAG, "processWordsInQueue inner loop")
                Thread.sleep(1000)
                if (model.dataByWords.isEmpty()) {
                    continue
                } else {
                    translate(model.dataByWords.poll()!!)
                }
            }
            return this
        }

        private fun translate(word: String) {
            Log.d(App.TAG, "[part 2] addNewRecord::translate")
            debugCounterPrintln()
            translator.translateChineseText(word, object: PlainTranslator.ITranslationListener {

                override fun onTranslationSuccess(translatedText: String) {
                    Log.d(App.TAG, "onTranslationSuccess $word and $translatedText")
                    model.dataWithTranslation.add(Pair(word, translatedText))
                    model.counter.decrementAndGet()
                    debugCounterPrintln()
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

            })
        }

    }

    private inner class AddPinyinTask: IPipelineTask {

        override suspend fun process(): IPipelineTask {
            Log.d(App.TAG, "[part 3] addNewRecord::extendWithPinyin")
            debugCounterPrintln()
            while (isTaskThreeFinished()) {
                Thread.sleep(1000)
                if (isTaskTwoFinished()) {
                    continue
                } else {
                    debugCounterPrintln()
                    Log.d(App.TAG, "model.dataWithTranslation ${model.dataWithTranslation}")
                    model.dataset = model.dataWithTranslation.map { it ->
                        val pinyin = piPinyin.toPinyin(it.first, " ")
                        Pair("%s / %s".format(it.first, pinyin), it.second)
                    }.toMutableList()
                    Log.d(App.TAG, "Extend translation with pinyin ${model.dataset}")
                    model.counter.decrementAndGet()
                }
            }
            return this
        }

    }

    private inner class PostProcessDataTask(val record: String): IPipelineTask {

        private var forbiddenSymbols: List<String> = mutableListOf<String>("", " ", ",", ", ")

        override suspend fun process(): IPipelineTask {
            cleanup()
            return this
        }

        private fun cleanup() {
            model.dataset = model.dataset.filter { it -> !forbiddenSymbols.contains(it.second) }.toMutableList()
        }

        @Deprecated("An origin record has been added into dataset at the beginning of the " +
                "pipeline. This method has been obsolete.")
        private fun addSentenceAsWhole() {
            val originSentence = record
            var originSentencePinyin = ""
            var originSentenceTranslation = ""
            for (item in model.dataset) {
                val originAndPinyin = item.first.trim().split("/")
                if (originAndPinyin.size != 2) {
                    val errorMsg = "PostProcessDataTask() find not expected origin hanzi&pinyin pair: ${item.first} / ${item.second}"
                    if (BuildConfig.DEBUG) {
                        throw IllegalStateException(errorMsg)
                    } else {
                        Log.e(App.TAG, errorMsg)
                    }
                } else {
                    originSentencePinyin = originSentencePinyin.plus(originAndPinyin.last())
                }
                originSentenceTranslation = originSentenceTranslation.plus(item.second).plus(" ")
            }
            model.dataset.add(
                Pair(
                    "%s / %s".format(originSentence, originSentencePinyin),
                    originSentenceTranslation)
            )
        }

    }

    private inner class StorageTask: IPipelineTask {
        override suspend fun process(): IPipelineTask {
            Log.d(App.TAG, "[part 4] addNewRecord::save")
            debugCounterPrintln()
            while (thereIsStillWork()) {
                Thread.sleep(1000)
                if (isTaskThreeFinished()) {
                    continue
                } else {
                    val toDomainObjects = model.dataset.map { it -> SubjectToStudy(toTranslate = it.first, translation = it.second) }
                    Log.d(App.TAG, "Data to save ${toDomainObjects}")
                    withContext(backgroundDispatcher) {
                        storageRepository.saveSubject(*toDomainObjects.map { it }.toTypedArray())
                    }
                    result = Result.success()
                    model.counter.decrementAndGet()
                }

            }
            return this
        }

    }
}