package com.gaiagps.iburn;

import static com.gaiagps.iburn.EventInfo.MOCK_NOW_DATE;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.gaiagps.iburn.EventInfo;

/**
 * Created by davidbrodsky on 7/8/15.
 */
public class CurrentDateProvider {

    public static Date getCurrentDate() {
        return BuildConfig.MOCK ? MOCK_NOW_DATE : new Date();
    }

    public static long getCurrentTimeMillis() {
        return BuildConfig.MOCK ? MOCK_NOW_DATE.getTime() : new Date().getTime();
    }
}
