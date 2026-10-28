package io.github.gelassen.wordinmemory.utils

import android.app.Activity

class ConfigParams {

    private val qualifier: Qualifier = Qualifier()

    fun getAmountOfColumnsForDashboard(activity: Activity): Int {
        val tabletColumnsAmount = 2
        val deviceColumnsAmount = 1
        return if (qualifier.isTablet(activity)) tabletColumnsAmount else deviceColumnsAmount
    }

    fun showDialogAsBottomSheet(activity: Activity): Boolean {
        return qualifier.isScreenBigEnough(activity)
                || qualifier.isTablet(activity)
    }
}