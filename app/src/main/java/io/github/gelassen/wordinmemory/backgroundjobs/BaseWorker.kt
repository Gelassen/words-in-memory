package io.github.gelassen.wordinmemory.backgroundjobs

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

open abstract class BaseWorker
constructor(
    context: Context,
    params: WorkerParameters,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CoroutineWorker(context, params) {

    object Consts {
        const val KEY_ERROR_MSG = "KEY_ERROR_MSG"
    }
    override suspend fun doWork(): Result {
        TODO("Not yet implemented")
    }
}