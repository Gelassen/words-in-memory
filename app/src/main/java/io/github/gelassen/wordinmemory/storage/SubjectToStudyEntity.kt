package io.github.gelassen.wordinmemory.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.gelassen.wordinmemory.model.SubjectToStudy

@Entity(tableName = SubjectToStudyDao.Const.TABLE_NAME)
data class SubjectToStudyEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "data") val subjectToTranslate: String,
    @ColumnInfo(name = "translation") val translation: String,
    @ColumnInfo(name = "completed") val isCompleted: Boolean,
    @ColumnInfo(name = "tutorCounter", defaultValue = "0") val tutorCounter: Int
)

fun SubjectToStudyEntity.toDomain(): SubjectToStudy {
    return SubjectToStudy(
        uid,
        subjectToTranslate,
        translation,
        isCompleted,
        tutorCounter
    )
}

fun SubjectToStudyEntity.fromDomain(subjectToStudy: SubjectToStudy): SubjectToStudyEntity {
    return SubjectToStudyEntity(
        subjectToStudy.uid,
        subjectToStudy.toTranslate,
        subjectToStudy.translation,
        subjectToStudy.isCompleted,
        subjectToStudy.tutorCounter
    )
}