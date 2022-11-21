package io.github.gelassen.wordinmemory.storage

import androidx.room.*
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectToStudyDao {
    object Const {
        const val tableName: String = "Subjects"
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg subjects: SubjectToStudyEntity)

    @Query("SELECT * FROM ${Const.tableName}")
    fun getAll(): Flow<List<SubjectToStudyEntity>>

    @Delete
    fun delete(subject: SubjectToStudyEntity)

    @Query("SELECT * FROM ${Const.tableName} WHERE not completed")
    fun getNotCompletedOnly(): Flow<List<SubjectToStudyEntity>>

    @Query("DELETE FROM ${Const.tableName}")
    fun clean()
}