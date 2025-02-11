package io.github.gelassen.wordinmemory.storage

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectToStudyDao {
    object Const {
        const val TABLE_NAME: String = "Subjects"
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg subjects: SubjectToStudyEntity)

    @Query("SELECT * FROM ${Const.TABLE_NAME}")
    fun getAll(): Flow<List<SubjectToStudyEntity>>

    @Query("SELECT * FROM ${Const.TABLE_NAME}")
    suspend fun getAllNonFlow(): List<SubjectToStudyEntity>

    @Delete
    fun delete(subject: SubjectToStudyEntity)

    @Query("SELECT * FROM ${Const.TABLE_NAME} WHERE not completed and not redundant")
    fun getNotCompletedOnly(): Flow<List<SubjectToStudyEntity>>

    @Query("SELECT * FROM ${Const.TABLE_NAME} WHERE completed and not redundant")
    fun getCompletedOnly(): Flow<List<SubjectToStudyEntity>>

    @Query("DELETE FROM ${Const.TABLE_NAME}")
    fun clean()

    @Query("SELECT * FROM ${Const.TABLE_NAME} WHERE not completed and not redundant ORDER BY tutorCounter ASC LIMIT 10")
    fun getFirstTenNotCompletedAndLessTutored(): List<SubjectToStudyEntity>
}