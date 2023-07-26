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
import io.github.gelassen.wordinmemory.backgroundjobs.BaseWorker.Consts.KEY_ERROR_MSG
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.model.convertToJson
import io.github.gelassen.wordinmemory.repository.StorageRepository
import io.github.gelassen.wordinmemory.utils.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileWriter
import java.io.Writer
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
            val destinationFile = File(destinationPath, context.getString(R.string.backup_file_json))
            /*writeAsCsvFile(dataset, destinationFile)*/
            writeAsJsonArray(dataset, destinationFile)
        } catch (ex: Exception) {
            val errorMsg = "Failed to backup database into external storage file"
            Log.e(App.TAG, errorMsg, ex)
            val outputData = workDataOf(KEY_ERROR_MSG to errorMsg)
            result = Result.failure(outputData)
        }
        return result
    }

    private fun writeAsJsonArray(dataset: List<SubjectToStudy>, destinationFile: File) {
        val jsonArray = JSONArray()
        dataset.forEach { it -> jsonArray.put(it.convertToJson()) }
        val fileWriter = FileWriter(destinationFile, true)
        fileWriter.write(jsonArray.toString())
        fileWriter.flush()
        fileWriter.close()
    }

    /**
     * There is an unresolved issue with write over FastCSV library, that's why it is left
     * for unused:
     * https://github.com/osiegmar/FastCSV/issues/81
     * */
    private fun writeAsCsvFile(dataset: List<SubjectToStudy>, destinationFile: File) {
        val fileWriter = FileWriter(destinationFile, true)
        val csvWriter = prepareCsvWriter(fileWriter)
        csvWriter.writeRow("uid", "toTranslate", "translation", "isCompleted")
        dataset.forEach { it -> csvWriter.writeRow(it.uid.toString(), it.toTranslate, it.translation, it.isCompleted.toString()) }
    }

    private fun prepareCsvWriter(fileWriter: Writer): CsvWriter {
        return CsvWriter.builder()
            .fieldSeparator(';')
            .quoteCharacter('\'')
            .quoteStrategy(QuoteStrategy.ALWAYS)
            .lineDelimiter(LineDelimiter.LF)
            .build(fileWriter)
            .writeComment("File created by WordsInMemory app on ${Date(System.currentTimeMillis())}")
    }

}