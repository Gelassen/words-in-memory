package io.github.gelassen.wordinmemory.model

import android.os.Parcel
import android.os.Parcelable
import io.github.gelassen.wordinmemory.storage.SubjectToStudyEntity
import org.json.JSONObject

class SubjectToStudy(): Parcelable {
    var uid: Int = 0
    lateinit var toTranslate: String
    lateinit var translation: String
    var isCompleted: Boolean = false

    constructor(parcel: Parcel) : this() {
        uid = parcel.readInt()
        toTranslate = parcel.readString()!!
        translation = parcel.readString()!!
        isCompleted = parcel.readByte() != 0.toByte()
    }

    constructor(uid: Int, toTranslate: String, translation: String, isCompleted: Boolean) : this() {
        this.uid = uid
        this.toTranslate = toTranslate
        this.translation = translation
        this.isCompleted = isCompleted
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(uid)
        parcel.writeString(toTranslate)
        parcel.writeString(translation)
        parcel.writeByte(if (isCompleted) 1 else 0)
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
}

fun SubjectToStudy.convertToJson(): String {
    val result = JSONObject()
    result.put(SubjectToStudyConst.UID, uid)
    result.put(SubjectToStudyConst.TO_TRANSLATE, toTranslate)
    result.put(SubjectToStudyConst.TRANSLATION, translation)
    result.put(SubjectToStudyConst.IS_COMPLETED, isCompleted)
    return result.toString()
}

fun SubjectToStudy.fromJson(subj: String): SubjectToStudy {
    val json = JSONObject(subj)
    val uid = json.optInt(SubjectToStudyConst.UID)
    val toTranslate = json.optString(SubjectToStudyConst.DATA)
    val isCompleted = json.optBoolean(SubjectToStudyConst.IS_COMPLETED)
    val translation = json.optString(SubjectToStudyConst.TRANSLATION)
    return SubjectToStudy(uid, toTranslate, translation, isCompleted)
}

fun SubjectToStudy.toStorage(): SubjectToStudyEntity {
    return SubjectToStudyEntity(
        uid,
        toTranslate,
        translation,
        isCompleted
    )
}

fun SubjectToStudy.fromStorage(subjectToStudy: SubjectToStudyEntity): SubjectToStudy {
    return SubjectToStudy(
        subjectToStudy.uid,
        subjectToStudy.subjectToTranslate,
        subjectToStudy.translation,
        subjectToStudy.isCompleted
    )
}