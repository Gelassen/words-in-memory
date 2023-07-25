package io.github.gelassen.wordinmemory.backgroundjobs

import android.content.Context
import android.icu.number.NumberRangeFormatter.RangeIdentityResult
import android.os.Environment
import android.util.Log
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.siegmar.fastcsv.writer.CsvWriter
import de.siegmar.fastcsv.writer.LineDelimiter
import de.siegmar.fastcsv.writer.QuoteStrategy
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.repository.StorageRepository
import io.github.gelassen.wordinmemory.utils.FileUtils
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
        var result = Result.failure()
        withContext(backgroundDispatcher) {
            if (FileUtils().isExternalStorageAvailable()) {
                val dataset = storageRepository.getSubjectsNonFlow()
                result = writeDatasetToExternalFile(dataset)
            }
        }
        return result
    }

    private fun writeDatasetToExternalFile(dataset: List<SubjectToStudy>): Result {
        var result: Result = Result.success()
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destinationPath = File(downloadsDir, context.getString(R.string.backup_folder))
            val destinationFile = File(destinationPath, context.getString(R.string.backup_file))
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