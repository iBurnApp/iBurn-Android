package com.gaiagps.iburn.database;

import com.gaiagps.iburn.PrefsHelper;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A data restriction policy that ensures location data never leaves the database
 * before {@link #EMBARGO_DATE} and without {@link PrefsHelper#enteredValidUnlockCode()}
 *
 * Created by davidbrodsky on 7/1/15.
 */
public class Embargo implements DataProvider.QueryInterceptor {

    // August 25, 2015
    public  static final Date   EMBARGO_DATE   = new GregorianCalendar(2015, Calendar.AUGUST, 25, 12, 0).getTime();

    private static final String NULL_LATITUDE  = "NULL AS " + PlayaItemTable.latitude;
    private static final String NULL_LONGITUDE = "NULL AS " + PlayaItemTable.longitude;

    private PrefsHelper prefs;

    public Embargo(PrefsHelper prefs) {
        this.prefs = prefs;
    }

    @Override
    public String onQueryIntercepted(String query) {
        if (isEmbargoActive(prefs)) {
            return query.replace(PlayaItemTable.latitude, NULL_LATITUDE)
                    .replace(PlayaItemTable.longitude, NULL_LONGITUDE);
        }
        return query;
    }

    public static boolean isEmbargoActive(PrefsHelper prefs) {
        return new Date().before(EMBARGO_DATE) && !prefs.enteredValidUnlockCode();
    }
}
