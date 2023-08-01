package io.github.gelassen.wordinmemory

import org.junit.Assert
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class DateAndTimeApiUnitTest {

    @Test
    fun checkTimeComponents_defaultCase_shownTotalAmountOfDaysAndHours() {
        val time = 1690897602699//System.currentTimeMillis()
        System.out.println("Time $time")
        time.seconds.toComponents { days: Long, hours: Int, minutes: Int, seconds: Int, nanoseconds: Int ->
            System.out.println("days ${days} ${hours}:${minutes}:${seconds}")
            Assert.assertEquals(days, 19570574)
            Assert.assertEquals(hours, 2)
        }
    }
}