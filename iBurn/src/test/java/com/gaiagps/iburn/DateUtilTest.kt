package com.gaiagps.iburn

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class DateUtilTest {

    @Test
    fun getDateString_compactsTimedRangeOnSamePlayaDay() {
        val start = playaDate("2026-09-01T14:30:00-0700")
        val end = playaDate("2026-09-01T16:00:00-0700")

        assertEquals(
            "Tue 9/1 2:30PM-4:00PM",
            DateUtil.getDateString(
                null,
                start,
                start,
                "Tue 9/1 2:30 PM",
                end,
                "Tue 9/1 4:00 PM"
            )
        )
    }

    @Test
    fun getDateString_keepsDatesForRangeAcrossPlayaDays() {
        val start = playaDate("2026-09-01T23:30:00-0700")
        val end = playaDate("2026-09-02T00:30:00-0700")

        assertEquals(
            "Tue 9/1 11:30 PM - Wed 9/2 12:30 AM",
            DateUtil.getDateString(
                null,
                start,
                start,
                "Tue 9/1 11:30 PM",
                end,
                "Wed 9/2 12:30 AM"
            )
        )
    }

    @Test
    fun getDateString_showsSameDayAllDayRangeOnce() {
        val start = playaDate("2026-09-01T10:00:00-0700")
        val end = playaDate("2026-09-01T20:00:00-0700")

        assertEquals(
            "Tue 9/1",
            DateUtil.getDateString(null, start, start, "Tue 9/1", end, "Tue 9/1")
        )
    }

    private fun playaDate(value: String): Date = DateUtil.getIso8601Format().parse(value)!!
}
