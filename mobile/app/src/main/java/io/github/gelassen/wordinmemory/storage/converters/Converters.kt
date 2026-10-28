package io.github.gelassen.wordinmemory.storage.converters

import androidx.room.TypeConverter
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.model.convertToJson
import io.github.gelassen.wordinmemory.model.fromJson

class Converters {

    @TypeConverter
    fun subjectToStudyFromDomainToStorage(subj: SubjectToStudy): String {
        return subj.convertToJson()
    }

    @TypeConverter
    fun subjectToStudyFromStorageToDomain(str: String): SubjectToStudy {
        return SubjectToStudy()
            .fromJson(str)
    }
}