package io.github.gelassen.wordinmemory.model

import android.os.Parcel
import android.os.Parcelable
import de.siegmar.fastcsv.reader.CsvRow
import io.github.gelassen.wordinmemory.storage.SubjectToStudyEntity
import org.json.JSONObject

class SubjectToStudy() : Parcelable {
    var uid: Int = 0
    lateinit var toTranslate: String
    lateinit var translation: String
    var isCompleted: Boolean = false
    var tutorCounter: Int = 0

    constructor(uid: Int, toTranslate: String, translation: String, isCompleted: Boolean, tutorCounter: Int) : this() {
        this.uid = uid
        this.toTranslate = toTranslate
        this.translation = translation
        this.isCompleted = isCompleted
        this.tutorCounter = tutorCounter
    }
    constructor(parcel: Parcel) : this() {
        uid = parcel.readInt()
        toTranslate = parcel.readString()!!
        translation = parcel.readString()!!
        isCompleted = parcel.readByte() != 0.toByte()
        tutorCounter = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(uid)
        parcel.writeString(toTranslate)
        parcel.writeString(translation)
        parcel.writeByte(if (isCompleted) 1 else 0)
        parcel.writeInt(tutorCounter)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SubjectToStudy> {
        override fun createFromParcel(parcel: Parcel): SubjectToStudy {
            return SubjectToStudy(parcel)
        }

        override fun newArray(size: Int): Array<SubjectToStudy?> {
            return arrayOfNulls(size)
        }
    }
}

private object SubjectToStudyConst {
    const val UID = "UID"
    const val DATA = "DATA"
    const val TO_TRANSLATE = "TO_TRANSLATE"
    const val TRANSLATION = "TRANSLATION"
    const val IS_COMPLETED = "IS_COMPLETED"
    const val TUTOR_COUNTER = "TUTOR_COUNTER"
}

fun SubjectToStudy.convertToJson(): String {
    val result = JSONObject()
    result.put(SubjectToStudyConst.UID, uid)
    result.put(SubjectToStudyConst.TO_TRANSLATE, toTranslate)
    result.put(SubjectToStudyConst.TRANSLATION, translation)
    result.put(SubjectToStudyConst.IS_COMPLETED, isCompleted)
    result.put(SubjectToStudyConst.TUTOR_COUNTER, tutorCounter)
    return result.toString()
}

fun SubjectToStudy.fromJson(subj: String): SubjectToStudy {
    val json = JSONObject(subj)
    val uid = json.optInt(SubjectToStudyConst.UID)
    val toTranslate = json.optString(SubjectToStudyConst.TO_TRANSLATE)
    val isCompleted = json.optBoolean(SubjectToStudyConst.IS_COMPLETED)
    val translation = json.optString(SubjectToStudyConst.TRANSLATION)
    val tutorCounter = json.optInt(SubjectToStudyConst.TUTOR_COUNTER)
    return SubjectToStudy(uid, toTranslate, translation, isCompleted, tutorCounter)
}

fun SubjectToStudy.toStorage(): SubjectToStudyEntity {
    return SubjectToStudyEntity(
        uid,
        toTranslate,
        translation,
        isCompleted,
        tutorCounter
    )
}

fun SubjectToStudy.fromStorage(subjectToStudy: SubjectToStudyEntity): SubjectToStudy {
    return SubjectToStudy(
        subjectToStudy.uid,
        subjectToStudy.subjectToTranslate,
        subjectToStudy.translation,
        subjectToStudy.isCompleted,
        subjectToStudy.tutorCounter
    )
}

fun SubjectToStudy.fromCsvRow(csvRow: CsvRow): SubjectToStudy {
    return SubjectToStudy(
        csvRow.getField(0).toInt(),
        csvRow.getField(1),
        csvRow.getField(2),
        csvRow.getField(3).toBoolean(),
        csvRow.getField(4).toInt()
    )
}