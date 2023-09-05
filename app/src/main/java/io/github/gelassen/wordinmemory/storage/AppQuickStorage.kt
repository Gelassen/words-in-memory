package io.github.gelassen.wordinmemory.storage

import android.app.Activity
import android.content.Context

class AppQuickStorage {

    companion object {
        const val KEY_LAST_TRAINED = "KEY_LAST_TRAINED"
    }

    // FIXME rewrite shared preferences to data storage API to avoid disk read penalty from StrictMode
    fun saveLastTrainedTime(activity: Activity, time: Long) {
/*        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        sharedPref
            ?.edit()
            ?.putLong(KEY_LAST_TRAINED, time)
            ?.apply()*/

    }

    fun getLastTrainedTime(activity: Activity): Long {
/*        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        return sharedPref?.getLong(KEY_LAST_TRAINED, 0L)!!*/
        return 0L
    }
}