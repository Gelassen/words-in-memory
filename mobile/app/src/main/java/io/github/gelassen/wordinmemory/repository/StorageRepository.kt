package io.github.gelassen.wordinmemory.repository

import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.model.toStorage
import io.github.gelassen.wordinmemory.storage.SubjectToStudyDao
import io.github.gelassen.wordinmemory.storage.SubjectToStudyEntity
import io.github.gelassen.wordinmemory.storage.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StorageRepository(val subjectsDao: SubjectToStudyDao) {

    /**
     * Batch save in one Room transaction / one observer notification.
     */
    suspend fun saveSubject(vararg subj: SubjectToStudy) {
        if (subj.isEmpty()) return
        val dbEntities = subj.map { it.toStorage() }
        subjectsDao.insertAllBatched(dbEntities)
    }

    suspend fun getSubjects(): Flow<List<SubjectToStudy>> {
        return subjectsDao.getAll()
            .map { list -> list.map { it.toDomain() } }
    }

    suspend fun getSubjectsNonFlow(): List<SubjectToStudy> {
        return subjectsDao.getAllNonFlow().map { it.toDomain() }
    }

    suspend fun removeSubject(subj: SubjectToStudy) {
        subjectsDao.delete(subj.toStorage())
    }

    suspend fun getNonCompleteSubjectsOnly(): Flow<List<SubjectToStudy>> {
        return subjectsDao.getNotCompletedOnly()
            .map { list -> list.map { it.toDomain() } }
    }

    suspend fun getCompleteSubjectsOnly(): Flow<List<SubjectToStudy>> {
        return subjectsDao.getCompletedOnly()
            .map { list -> list.map { it.toDomain() } }
    }

    suspend fun getDailyPractice(): List<SubjectToStudy> {
        return subjectsDao.getFirstTenNotCompletedAndLessTutored().map { it.toDomain() }
    }

    suspend fun getRedundantSubjectsFromList(vararg subj: SubjectToStudy): List<SubjectToStudy> {
        val names: List<String> = subj.map { it.toTranslate }
        return subjectsDao.getRedundantSubjects(names)
            .map { it.toDomain() }
    }
}
