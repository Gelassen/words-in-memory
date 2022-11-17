package io.github.gelassen.wordinmemory.repository

import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.model.toStorage
import io.github.gelassen.wordinmemory.storage.SubjectToStudyDao
import io.github.gelassen.wordinmemory.storage.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StorageRepository(val subjectsDao: SubjectToStudyDao) {

    suspend fun saveSubject(subj: SubjectToStudy) {
        subjectsDao.insertAll(subj.toStorage())
    }

    suspend fun getSubjects(): Flow<List<SubjectToStudy>> {
        return subjectsDao.getAll()
            .map { it -> it.map { it.toDomain() } }
    }

    suspend fun removeSubject(subj: SubjectToStudy) {
        subjectsDao.delete(subj.toStorage())
    }

    suspend fun getNonCompleteSubjectsOnly(): Flow<List<SubjectToStudy>> {
        return subjectsDao.getNotCompletedOnly()
            .map { it -> it.map { it.toDomain() } }
    }
}