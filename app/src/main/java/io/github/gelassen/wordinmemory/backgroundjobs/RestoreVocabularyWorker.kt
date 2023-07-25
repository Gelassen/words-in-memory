package io.github.gelassen.wordinmemory.backgroundjobs

import android.content.Context
import android.os.Environment
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.siegmar.fastcsv.reader.CsvReader
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.model.fromCsvRow
import io.github.gelassen.wordinmemory.repository.StorageRepository
import io.github.gelassen.wordinmemory.utils.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.File

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
        if (!FileUtils().isExternalStorageAvailable()) {
            // TODO extend with error
            result = Result.failure()
        } else if (!isThereBackupFile()) {
            // TODO extend with error
            result = Result.failure()
        } else {
            val dataset = readCsvFile()
            // TODO reset uid to zero to create new ones - it will allow to integrated existing backup with recently added rows
            storageRepository.saveSubject(*dataset.map { it }.toTypedArray())
        }
        return result
    }

    private fun isThereBackupFile(): Boolean {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val destinationPath = File(downloadsDir, context.getString(R.string.backup_folder))
        val destinationFile = File(destinationPath, context.getString(R.string.backup_file))
        return destinationFile.exists()
    }

    private fun readCsvFile(): MutableList<SubjectToStudy> {
        val result = mutableListOf<SubjectToStudy>()
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val destinationPath = File(downloadsDir, context.getString(R.string.backup_folder))
        val destinationFile = File(destinationPath, context.getString(R.string.backup_file))
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