package com.gaiagps.iburn

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

object EventInfo {
    const val CURRENT_YEAR = EventYear.YEAR

    private fun createDate(year: Int, month: Int, day: Int, hourOfDay: Int = 0, minute: Int = 0): Date {
        val cal = GregorianCalendar(year, month, day, hourOfDay, minute)
        cal.timeZone = DateUtil.PLAYA_TIME_ZONE
        return cal.time
    }

    /**
     * The date when the event starts. Used to populate date selection pickers.
     */
    @JvmField
    val EVENT_START_DATE: Date = createDate(CURRENT_YEAR, Calendar.AUGUST, 24)

    /**
     * The date when the event ends. Used to populate date selection pickers.
     */
    @JvmField
    val EVENT_END_DATE: Date = createDate(CURRENT_YEAR, Calendar.SEPTEMBER, 1)

    /**
     * The date when location data is publicly available without a staff unlock code.
     */
    @JvmField
    val EMBARGO_DATE: Date = EVENT_START_DATE

    /**
     * The "current" date used by the 'mock' build variant.
     */
    @JvmField
    val MOCK_NOW_DATE: Date = createDate(CURRENT_YEAR, Calendar.AUGUST, 25, 10, 5)
}
