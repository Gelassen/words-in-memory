package io.github.gelassen.wordinmemory.providers

import android.text.format.DateUtils
import android.util.Log
import io.github.gelassen.wordinmemory.App
import java.text.SimpleDateFormat
import java.util.Locale

class DashboardProvider {

    fun isInputEmpty(input: String): Boolean {
        return input.isEmpty()
    }

    fun isTimeToShowDailyTraining(lastShownTime: Long, currentTime: Long) : Boolean {
        Log.d(App.TAG, "isTimeToShowDailyTraining()")
/*        if (currentTime < lastShownTime) return true // it shouldn't happened, just protect function from occasional garbage
        // TODO consider to use lastShownTime.seconds.toComponents {}
        return !DateUtils.isToday(lastShownTime)*/
        return true
    }

    fun getLegacyDateDifference(fromDate: String, toDate: String,
                                formatter: String= "yyyy-MM-dd HH:mm:ss",
                                locale: Locale = Locale.getDefault()): Map<String, Long> {

        val fmt = SimpleDateFormat(formatter, locale)
        val bgn = fmt.parse(fromDate)
        val end = fmt.parse(toDate)

        val milliseconds = end.time - bgn.time
        val days = milliseconds / 1000 / 3600 / 24
        val hours = milliseconds / 1000 / 3600
        val minutes = milliseconds / 1000 / 3600
        val seconds = milliseconds / 1000
        val weeks = days.div(7)

        return mapOf("days" to days, "hours" to hours, "minutes" to minutes, "seconds" to seconds, "weeks" to weeks)
    }
}