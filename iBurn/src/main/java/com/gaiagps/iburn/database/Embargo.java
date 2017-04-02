package com.gaiagps.iburn.database;

import android.support.annotation.NonNull;

import com.gaiagps.iburn.BuildConfig;
import com.gaiagps.iburn.CurrentDateProvider;
import com.gaiagps.iburn.PrefsHelper;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

/**
 * A data restriction policy that ensures location data never leaves the database
 * before {@link #EMBARGO_DATE} and without {@link PrefsHelper#enteredValidUnlockCode()}
 *
 * Created by davidbrodsky on 7/1/15.
 */
public class Embargo implements DataProvider.QueryInterceptor {

    // 2016 Embargo date is August 28, 2016.
    public  static final Date   EMBARGO_DATE   = new GregorianCalendar(2016, Calendar.AUGUST, 28, 0, 0).getTime();

    // For mock builds, force user to enter unlock code
    private static final boolean FORCE_EMBARGO = BuildConfig.MOCK;
    private static final String NULL_LATITUDE  = "NULL AS " + PlayaItemTable.latitude;
    private static final String NULL_LONGITUDE = "NULL AS " + PlayaItemTable.longitude;
    private static final String NULL_ADDRESS = "NULL AS " + PlayaItemTable.playaAddress;

    private PrefsHelper prefs;

    public Embargo(PrefsHelper prefs) {
        this.prefs = prefs;
    }

    @Override
    public String onQueryIntercepted(@NonNull String query, @NonNull Iterable<String> tables) {
        // If Embargo is active and this query does not select from POIS
        if (isEmbargoActive(prefs) && !isPoiTableQuery(tables)) {
            return query.replace(PlayaItemTable.latitude, NULL_LATITUDE)
                    .replace(PlayaItemTable.longitude, NULL_LONGITUDE)
                    .replace(PlayaItemTable.playaAddress, NULL_ADDRESS)
                    .replace("SELECT *", "SELECT *, " + NULL_LATITUDE + ", " + NULL_LONGITUDE + ", " + NULL_ADDRESS);
        }
        return query;
    }

    private boolean isPoiTableQuery(Iterable<String> tables) {
        Iterator<String> tableIterator = tables.iterator();
        String tableName = tableIterator.next();
        return tableName.equals(PlayaDatabase.POIS) && !tableIterator.hasNext();
    }

    public static boolean isEmbargoActive(PrefsHelper prefs) {
        // Embargo is active if before date and no unlock code present
        return (FORCE_EMBARGO || CurrentDateProvider.getCurrentDate().before(EMBARGO_DATE)) && !prefs.enteredValidUnlockCode();
    }
}
