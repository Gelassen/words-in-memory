package io.github.gelassen.wordinmemory.backgroundjobs

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.repository.StorageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset


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
            backupToCsvFile()
        }
        return Result.success()
    }

    suspend fun backupToCsvFile() {
        // TODO check write to external storage permissions
        // TODO check SD card availability
        // TODO get cursor with data from database
        // TODO write a valid csv file

        if (isExternalStorageAvailable()) {
            createTestFileInSharedFolder()
            val dataset = storageRepository.getSubjectsNonFlow()

        }
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

    fun createTestFileInSharedFolder() {
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
    }
}