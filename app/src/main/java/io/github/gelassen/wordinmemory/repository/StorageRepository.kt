package io.github.gelassen.wordinmemory.repository

import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.model.toStorage
import io.github.gelassen.wordinmemory.storage.SubjectToStudyDao
import io.github.gelassen.wordinmemory.storage.SubjectToStudyEntity
import io.github.gelassen.wordinmemory.storage.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StorageRepository(val subjectsDao: SubjectToStudyDao) {

    suspend fun saveSubject(vararg subj: SubjectToStudy) {
        val dbEntities = mutableListOf<SubjectToStudyEntity>()
        subj.forEach { dbEntities.add(it.toStorage()) }
        subjectsDao.insertAll(*dbEntities.map { it }.toTypedArray())
    }

    suspend fun getSubjects(): Flow<List<SubjectToStudy>> {
        return subjectsDao.getAll()
            .map { it -> it.map { it.toDomain() } }
    }

    suspend fun getSubjectsNonFlow(): List<SubjectToStudy> {
        return subjectsDao.getAllNonFlow().map { it -> it.toDomain() }
    }

    suspend fun removeSubject(subj: SubjectToStudy) {
        subjectsDao.delete(subj.toStorage())
    }

    suspend fun getNonCompleteSubjectsOnly(): Flow<List<SubjectToStudy>> {
        return subjectsDao.getNotCompletedOnly()
            .map { it -> it.map { it.toDomain() } }
    }

    suspend fun getCompleteSubjectsOnly(): Flow<List<SubjectToStudy>> {
        return subjectsDao.getCompletedOnly()
            .map { it -> it.map { it.toDomain() } }
    }

    suspend fun getDailyPractice(): List<SubjectToStudy> {
        return subjectsDao.getFirstTenNotCompletedAndLessTutored().map { it.toDomain() }
    }
}