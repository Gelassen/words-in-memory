package io.github.gelassen.wordinmemory.utils

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import io.github.gelassen.wordinmemory.R

class Qualifier() {

    companion object {
        const val BASELINE_WIDTH = 1080
        const val BASELINE_HEIGHT = 1920
        const val BASELINE_DIAGONAL = 5.0f
    }

    // 5inch, 1080 x 1920 - a baseline for current UX
    // 7inch, - a baseline for two columns on a display

    fun isScreenBigEnough(activity: Activity): Boolean {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return isBaselineDimensionsOrHigher(displayMetrics)
                && getDiagonalInInches(displayMetrics) >= BASELINE_DIAGONAL
                && !isTablet(activity)

    }

    fun isTablet(context: Context): Boolean {
        return context.resources.getBoolean(R.bool.isTablet)
    }

    private fun isBaselineDimensionsOrHigher(displayMetrics: DisplayMetrics): Boolean {
        return displayMetrics.heightPixels >= BASELINE_HEIGHT
                && displayMetrics.widthPixels >= BASELINE_WIDTH
    }

    private fun getDiagonalInInches(displayMetrics: DisplayMetrics): Double {
        val yInches: Float = displayMetrics.heightPixels / displayMetrics.ydpi
        val xInches: Float = displayMetrics.widthPixels / displayMetrics.xdpi
        val diagonalInches = Math.sqrt((xInches * xInches + yInches * yInches).toDouble())
        return diagonalInches
    }

}