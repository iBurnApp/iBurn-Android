package com.gaiagps.iburn;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by davidbrodsky on 7/8/15.
 */
public class CurrentDateProvider {

    /**
     * Date to use as "now" for Debug builds
     */
    public static Date MOCK_NOW_DATE = new GregorianCalendar(2015, Calendar.AUGUST, 31, 15, 43).getTime();

    public static Date getCurrentDate() {
        return BuildConfig.MOCK ? MOCK_NOW_DATE : new Date();
    }

    public static long getCurrentTimeMillis() {
        return BuildConfig.MOCK ? MOCK_NOW_DATE.getTime() : new Date().getTime();
    }
}
