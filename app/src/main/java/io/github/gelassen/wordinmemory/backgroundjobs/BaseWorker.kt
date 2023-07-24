package io.github.gelassen.wordinmemory.backgroundjobs

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

open abstract class BaseWorker
constructor(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    object Consts {
        const val KEY_ERROR_MSG = "KEY_ERROR_MSG"
    }
    override suspend fun doWork(): Result {
        TODO("Not yet implemented")
    }
}