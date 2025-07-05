package com.gaiagps.iburn

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

object EventInfo {
    const val CURRENT_YEAR = 2024

    private fun createDate(year: Int, month: Int, day: Int): Date {
        val cal = GregorianCalendar(year, month, day)
        cal.timeZone = DateUtil.PLAYA_TIME_ZONE
        return cal.time
    }

    @JvmField
    val EVENT_START_DATE: Date = createDate(CURRENT_YEAR, Calendar.AUGUST, 25)

    @JvmField
    val EVENT_END_DATE: Date = createDate(CURRENT_YEAR, Calendar.SEPTEMBER, 2)

    @JvmField
    val EMBARGO_DATE: Date = createDate(CURRENT_YEAR, Calendar.AUGUST, 25)
}
