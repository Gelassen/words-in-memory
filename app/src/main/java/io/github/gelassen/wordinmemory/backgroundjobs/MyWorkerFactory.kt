package io.github.gelassen.wordinmemory.backgroundjobs

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import io.github.gelassen.wordinmemory.ml.PlainTranslator
import io.github.gelassen.wordinmemory.repository.NetworkRepository
import io.github.gelassen.wordinmemory.repository.StorageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class MyWorkerFactory @Inject constructor(
    val translator: PlainTranslator,
    val storageRepository: StorageRepository,
    val networkRepository: NetworkRepository,
    val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
): WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when(workerClassName) {
            BackupVocabularyWorker::class.java.name -> {
                BackupVocabularyWorker(
                    context = appContext,
                    params = workerParameters,
                    storageRepository = storageRepository,
                    backgroundDispatcher = backgroundDispatcher
                )
            }
            RestoreVocabularyWorker::class.java.name -> {
                RestoreVocabularyWorker(
                    context = appContext,
                    params = workerParameters,
                    storageRepository = storageRepository,
                    backgroundDispatcher = backgroundDispatcher
                )
            }
            AddNewRecordWorker::class.java.name -> {
                AddNewRecordWorker(
                    context = appContext,
                    params = workerParameters,
                    translator = translator,
                    networkRepository = networkRepository,
                    storageRepository = storageRepository,
                    backgroundDispatcher = backgroundDispatcher
                )
            }
            else -> throw IllegalStateException("Unknown worker. Did you forget to register a new type of worker in the factory?")
        }
    }
}