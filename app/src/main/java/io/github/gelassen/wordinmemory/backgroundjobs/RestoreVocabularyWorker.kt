package io.github.gelassen.wordinmemory.backgroundjobs

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.siegmar.fastcsv.reader.CsvReader
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.backgroundjobs.BaseWorker.Consts.KEY_ERROR_MSG
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.model.fromCsvRow
import io.github.gelassen.wordinmemory.model.fromJson
import io.github.gelassen.wordinmemory.repository.StorageRepository
import io.github.gelassen.wordinmemory.utils.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import java.io.File
import java.io.FileReader

class RestoreVocabularyWorker(
    val context: Context,
    val params: WorkerParameters,
    val storageRepository: StorageRepository,
    val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseWorker(context, params) {

    object Builder {
        fun build(): Data {
            return workDataOf()
        }
    }
    override suspend fun doWork(): Result {
        var result = Result.failure()
        try {
            if (!FileUtils().isExternalStorageAvailable()) {
                val errorMsg = "External storage is not available"
                val outputData = workDataOf(KEY_ERROR_MSG to errorMsg)
                result = Result.failure(outputData)
            } else if (!isThereBackupFile()) {
                val errorMsg = "There is no backup file ${context.getString(R.string.backup_file_csv)}"
                val outputData = workDataOf(KEY_ERROR_MSG to errorMsg)
                result = Result.failure(outputData)
            } else {
                val dataset = getDataFromBackup()
                // TODO reset uid to zero to create new ones - it will allow to integrated existing backup with recently added rows
                storageRepository.saveSubject(*dataset.map { it }.toTypedArray())
            }
        } catch (ex: Exception) {
            val errorMsg = "Failed to restore a vocabulary"
            Log.e(App.TAG, errorMsg, ex)
            val outputData = workDataOf(KEY_ERROR_MSG to errorMsg)
            result = Result.failure(outputData)
        }
        return result
    }

    private fun isThereBackupFile(): Boolean {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val destinationPath = File(downloadsDir, context.getString(R.string.backup_folder))
        val destinationFile = File(destinationPath, context.getString(R.string.backup_file_json))
        return destinationFile.exists()
    }

    private fun getDataFromBackup(): MutableList<SubjectToStudy> {
        val result = mutableListOf<SubjectToStudy>()
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val destinationPath = File(downloadsDir, context.getString(R.string.backup_folder))
        val destinationFile = File(destinationPath, context.getString(R.string.backup_file_json/*backup_file_csv*/))
        /*readCsvFile(destinationFile)*/
        result.addAll(readJsonFile(destinationFile))
        return result
    }

    private fun readJsonFile(destinationFile: File): MutableList<SubjectToStudy> {
        val result = mutableListOf<SubjectToStudy>()
        val fileReader = FileReader(destinationFile)
        val jsonArrayAsString = fileReader.readText()
        fileReader.close()
        val jsonArray = JSONArray(jsonArrayAsString)
        for (idx in 0 until jsonArray.length()) {
            result.add(
                SubjectToStudy().fromJson(jsonArray.optJSONObject(idx).toString())
            )
        }
        return result
    }

    /**
     * There is an unresolved issue with write over FastCSV library, that's why it is left unused:
     * https://github.com/osiegmar/FastCSV/issues/81
     * */
    private fun readCsvFile(destinationFile: File): MutableList<SubjectToStudy> {
        val result = mutableListOf<SubjectToStudy>()
        val text = destinationFile.readText(java.nio.charset.StandardCharsets.UTF_8)
        val csvReader: CsvReader = CsvReader.builder().build(text)
        val iterator = csvReader.iterator()
        while (iterator.hasNext()) {
            val row = iterator.next()
            if (row.fields.contains("toTranslate")) {
                continue // skip the first as a header
            }
            result.add(SubjectToStudy().fromCsvRow(row))
        }
        return result
    }

}