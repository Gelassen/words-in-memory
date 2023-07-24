package io.github.gelassen.wordinmemory.backgroundjobs

import android.content.Context
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class BackupVocabularyWorker(
    context: Context,
    params: WorkerParameters) : BaseWorker(context, params) {

    object Builder {
        fun build(): Data {
            return workDataOf()
        }
    }

    override suspend fun doWork(): Result {
        return super.doWork()
    }

    fun backupToCsvFile() {
        // TODO check write to external storage permissions
        // TODO check SD card availability
        // TODO get cursor with data from database
        // TODO write a valid csv file
    }
}