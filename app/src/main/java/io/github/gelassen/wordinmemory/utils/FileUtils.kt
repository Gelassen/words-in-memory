package io.github.gelassen.wordinmemory.utils

import android.os.Environment
import android.util.Log
import io.github.gelassen.wordinmemory.App

class FileUtils {

    fun isExternalStorageAvailable(): Boolean {
        var isExternalStorageAvailable = false
        var isExternalStorageWriteable = false
        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED == state) {
            isExternalStorageWriteable = true
            isExternalStorageAvailable = isExternalStorageWriteable
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY == state) {
            isExternalStorageAvailable = true
            isExternalStorageWriteable = false
        } else {
            isExternalStorageWriteable = false
            isExternalStorageAvailable = isExternalStorageWriteable
        }
        Log.d(
            App.TAG, "External storage state availability (${isExternalStorageAvailable}) " +
                "and writeability(${isExternalStorageWriteable})")
        return isExternalStorageAvailable && isExternalStorageWriteable
    }
}