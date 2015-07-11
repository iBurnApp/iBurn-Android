package com.gaiagps.iburn.database;

import com.gaiagps.iburn.CurrentDateProvider;
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
    public  static final Date   EMBARGO_DATE   = new GregorianCalendar(2014, Calendar.AUGUST, 25, 0, 0).getTime();

    private static final String NULL_LATITUDE  = "NULL AS " + PlayaItemTable.latitude;
    private static final String NULL_LONGITUDE = "NULL AS " + PlayaItemTable.longitude;

    private PrefsHelper prefs;

    public Embargo(PrefsHelper prefs) {
        this.prefs = prefs;
    }

    @Override
    public String onQueryIntercepted(String query, String... tables) {
        // If Embargo is active and this query does not select from POIS
        if (isEmbargoActive(prefs) && !isPoiTableQuery(tables)) {
            return query.replace(PlayaItemTable.latitude, NULL_LATITUDE)
                    .replace(PlayaItemTable.longitude, NULL_LONGITUDE);
        }
        return query;
    }

    private boolean isPoiTableQuery(String... tables) {
        return tables.length == 1 && tables[0].equals(PlayaDatabase.POIS);
    }

    public static boolean isEmbargoActive(PrefsHelper prefs) {
        return CurrentDateProvider.getCurrentDate().before(EMBARGO_DATE) && !prefs.enteredValidUnlockCode();
    }
}
