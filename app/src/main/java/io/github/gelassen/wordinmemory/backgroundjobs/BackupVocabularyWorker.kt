package io.github.gelassen.wordinmemory.backgroundjobs

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.backgroundjobs.BaseWorker.Consts.KEY_ERROR_MSG
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.model.convertToJson
import io.github.gelassen.wordinmemory.repository.StorageRepository
import io.github.gelassen.wordinmemory.utils.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.Objects


class BackupVocabularyWorker(
    val context: Context,
    val params: WorkerParameters,
    val storageRepository: StorageRepository,
    val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO) : BaseWorker(context, params) {

    object Builder {

        const val EXTRA_BACKUP_URI = "EXTRA_BACKUP_URI"
        fun build(uriToBackupDataFile: Uri): Data {
            return workDataOf(EXTRA_BACKUP_URI to uriToBackupDataFile.toString())
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
            val destinationUri = Uri.parse(inputData.getString(RestoreVocabularyWorker.Builder.EXTRA_BACKUP_URI))
            writeAsJsonArray(dataset, destinationUri)
        } catch (ex: Exception) {
            val errorMsg = "Failed to backup database into external storage file"
            Log.e(App.TAG, errorMsg, ex)
            val outputData = workDataOf(KEY_ERROR_MSG to errorMsg)
            result = Result.failure(outputData)
        }
        return result
    }

    private fun writeAsJsonArray(dataset: List<SubjectToStudy>, destinationUri: Uri) {
        Log.d(App.TAG, "Save file to a destination folder $destinationUri")
        val jsonArray = JSONArray()
        dataset.forEach { it -> jsonArray.put(JSONObject(it.convertToJson())) }
        writeTextToUri(context, jsonArray.toString(), destinationUri)
    }

    @Throws(IOException::class)
    fun writeTextToUri(context: Context, dataset: String, uri: Uri?) {
        context.contentResolver.openOutputStream(uri!!).use { outputStream ->
            BufferedWriter(
                OutputStreamWriter(Objects.requireNonNull(outputStream))
            ).use { writer ->
                writer.write(dataset)
                writer.flush()
                writer.close()
            }
        }
    }


}