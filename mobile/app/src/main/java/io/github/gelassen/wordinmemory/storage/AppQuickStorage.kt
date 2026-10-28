package io.github.gelassen.wordinmemory.storage

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppQuickStorage {

    companion object {
        const val KEY_LAST_TRAINED = "KEY_LAST_TRAINED"
    }

    // FIXME rewrite shared preferences to data storage API to avoid disk read penalty from StrictMode
    fun saveLastTrainedTime(activity: Activity, time: Long) {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        sharedPref
            ?.edit()
            ?.putLong(KEY_LAST_TRAINED, time)
            ?.apply()
    }

    fun getLastTrainedTime(activity: Activity): Long {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        return sharedPref?.getLong(KEY_LAST_TRAINED, 0L)!!
    }

    suspend fun enableExtendedMode(activity: Activity) {
        withContext(Dispatchers.IO) {
            val sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .putBoolean("extended_mode", true)
                .commit()
        }
    }

    suspend fun disableExtendedMode(activity: Activity) {
        withContext(Dispatchers.IO) {
            val sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .putBoolean("extended_mode", false)
                .commit()
        }
    }

    fun isExtendedModeEnabled(activity: Activity): Boolean {
        val sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("extended_mode", false)
    }

    fun getBackendIp(activity: Activity,) : String {
        val sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)
        return sharedPreferences.getString("server_ip", "192.168.1.1")!!
    }

    fun getBackendPort(activity: Activity,) : String {
        val sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)
        return sharedPreferences.getString("server_port", "80")!!
    }
}