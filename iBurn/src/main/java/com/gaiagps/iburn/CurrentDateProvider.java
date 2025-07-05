package com.gaiagps.iburn;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.gaiagps.iburn.EventInfo;

/**
 * Created by davidbrodsky on 7/8/15.
 */
public class CurrentDateProvider {

    /**
     * Date to use as "now" for Debug builds
     */
    private static Date MOCK_NOW_DATE = new GregorianCalendar(
            EventInfo.CURRENT_YEAR, Calendar.AUGUST, 25, 10, 05)
            .getTime();

    public static Date getCurrentDate() {
        return BuildConfig.MOCK ? MOCK_NOW_DATE : new Date();
    }

    public static long getCurrentTimeMillis() {
        return BuildConfig.MOCK ? MOCK_NOW_DATE.getTime() : new Date().getTime();
    }
}
