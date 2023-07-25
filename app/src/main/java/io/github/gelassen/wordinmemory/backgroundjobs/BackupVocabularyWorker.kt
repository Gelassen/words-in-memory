package io.github.gelassen.wordinmemory.backgroundjobs

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.siegmar.fastcsv.writer.CsvWriter
import de.siegmar.fastcsv.writer.LineDelimiter
import de.siegmar.fastcsv.writer.QuoteStrategy
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.repository.StorageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.StringWriter
import java.util.Date


class BackupVocabularyWorker(
    val context: Context,
    val params: WorkerParameters,
    val storageRepository: StorageRepository,
    val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO) : BaseWorker(context, params) {

    object Builder {
        fun build(): Data {
            return workDataOf()
        }
    }

    override suspend fun doWork(): Result {
        withContext(backgroundDispatcher) {
            if (isExternalStorageAvailable()) {
                val dataset = storageRepository.getSubjectsNonFlow()
                writeDatasetToExternalFile(dataset)
            }
        }
        return Result.success()
    }

    private fun isExternalStorageAvailable(): Boolean {
        var isExternalStorageAvailable = false
        var isExternalStorageWriteable = false
        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED == state) {
            isExternalStorageWriteable = true
            isExternalStorageAvailable = isExternalStorageWriteable
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY == state) {
            isExternalStorageAvailable = true
            isExternalStorageWriteable = false
        } else {
            isExternalStorageWriteable = false
            isExternalStorageAvailable = isExternalStorageWriteable
        }
        Log.d(App.TAG, "External storage state availability (${isExternalStorageAvailable}) " +
                "and writeability(${isExternalStorageWriteable})")
        return isExternalStorageAvailable && isExternalStorageWriteable
    }

    private fun writeDatasetToExternalFile(dataset: List<SubjectToStudy>): Result {
        var result: Result = Result.success()
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destinationPath = File(downloadsDir, "WordsInMemory")
            val destinationFile = File(destinationPath, "WordsInMemory-vocabulary.csv")
            destinationFile.mkdirs()
            val sw = StringWriter()
            val csvWriter = prepareCsvWriter(sw)
            csvWriter.writeRow("uid", "toTranslate", "translation", "isCompleted")
            dataset.forEach { it -> csvWriter.writeRow(it.uid.toString(), it.toTranslate, it.translation, it.isCompleted.toString()) }
        } catch (ex: Exception) {
            Log.e(App.TAG, "Failed to backup database into external storage file", ex)
            result = Result.failure()
        }
        return result
    }

    private fun prepareCsvWriter(sw: StringWriter): CsvWriter {
        return CsvWriter.builder()
            .fieldSeparator(';')
            .quoteCharacter('\'')
            .quoteStrategy(QuoteStrategy.ALWAYS)
            .lineDelimiter(LineDelimiter.LF)
            .build(sw)
            .writeComment("File created by WordsInMemory app on ${Date(System.currentTimeMillis())}")
    }
/*    fun createTestFileInSharedFolder() {
        Log.d(App.TAG, "[start] createTestFileInSharedFolder")
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val destinationPath = File(downloadsDir, "WordsInMemory")
        val destinationFile = File(destinationPath, "test.txt")
        var outputStream: FileOutputStream? = null
        try {
            destinationPath.mkdirs()

            outputStream = FileOutputStream(destinationFile)
            val testStr = "Hello from WordsInMemory app!"
            outputStream.write(testStr.toByteArray(Charset.defaultCharset()))
            outputStream.flush()
        } catch (ex: Exception) {
            Log.e(App.TAG, "Failed to write into shared folder", ex)
        } finally {
            outputStream?.close()
            Log.d(App.TAG, "[end] createTestFileInSharedFolder")
        }
    }*/
}