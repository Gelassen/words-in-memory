package io.github.gelassen.wordinmemory.backgroundjobs

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.hasKeyWithValueOfType
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
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Objects


class RestoreVocabularyWorker(
    val context: Context,
    val params: WorkerParameters,
    val storageRepository: StorageRepository,
    val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseWorker(context, params) {

    object Builder {

        const val EXTRA_BACKUP_URI = "EXTRA_BACKUP_URI"

        fun build(backupUri: Uri): Data {
            return workDataOf(EXTRA_BACKUP_URI to backupUri.toString())
        }
    }
    override suspend fun doWork(): Result {
        var result = Result.failure()
        try {
            if (!FileUtils().isExternalStorageAvailable()) {
                val errorMsg = "External storage is not available"
                result = prepareFailureResult(errorMsg)
            } else if (!inputData.hasKeyWithValueOfType<String>(Builder.EXTRA_BACKUP_URI)) {
                val errorMsg = "There is no uri for backup data. Did you forget to pass it when had prepared Worker?"
                result = prepareFailureResult(errorMsg)
            } else if (!DocumentFile.fromSingleUri(
                    context,
                    Uri.parse(inputData.getString(Builder.EXTRA_BACKUP_URI))
                    )!!.exists()
                ) {
                val errorMsg = "Uri is valid, but file doesn't exist anymore. Did you or any your apps unexpectedly had removed it?"
                result = prepareFailureResult(errorMsg)
            } else {
                val backupUri = Uri.parse(inputData.getString(Builder.EXTRA_BACKUP_URI))
                val dataset = getDataFromBackup(backupUri)
                // TODO reset uid to zero to create new ones - it will allow to integrated existing backup with recently added rows
                storageRepository.saveSubject(*dataset.map { it }.toTypedArray())
                result = Result.success()
            }
        } catch (ex: Exception) {
            val errorMsg = "Failed to restore a vocabulary"
            Log.e(App.TAG, errorMsg, ex)
            result = prepareFailureResult(errorMsg)
        }
        return result
    }

    private fun prepareFailureResult(errorMsg: String): Result {
        val outputData = workDataOf(KEY_ERROR_MSG to errorMsg)
        return Result.failure(outputData)
    }

    private fun getDataFromBackup(backupUri: Uri): MutableList<SubjectToStudy> {
        val result = mutableListOf<SubjectToStudy>()
        Log.d(App.TAG, "Plain read from uri $backupUri")
        val jsonArrayAsString = readTextFromUri(context, backupUri)
        result.addAll(getDatasetFromText(jsonArrayAsString))
        return result
    }

    private fun getDatasetFromText(jsonArrayAsString: String): MutableList<SubjectToStudy> {
        val result = mutableListOf<SubjectToStudy>()
        val jsonArray = JSONArray(jsonArrayAsString)
        Log.d(App.TAG, "JSON array: ${jsonArray.toString()}")
        for (idx in 0 until jsonArray.length()) {
            Log.d(App.TAG, "Json item at index ${idx} is ${jsonArray.getJSONObject(idx).toString()}")
            jsonArray.getJSONObject(idx)
            result.add(
                SubjectToStudy().fromJson(jsonArray.optJSONObject(idx).toString())
            )
        }
        return result
    }

    @Throws(IOException::class)
    fun readTextFromUri(context: Context, uri: Uri?): String {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri!!).use { inputStream ->
            BufferedReader(
                InputStreamReader(Objects.requireNonNull(inputStream))
            ).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
            }
            inputStream!!.close()
        }
        return stringBuilder.toString()
    }

}