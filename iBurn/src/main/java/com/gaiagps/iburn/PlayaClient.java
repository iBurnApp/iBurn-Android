package com.gaiagps.iburn;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.gaiagps.iburn.database.DBWrapper;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * An API for interacting with iBurn application state. Wraps interacting
 * with {@link android.content.SharedPreferences} and also the
 * {@link com.gaiagps.iburn.database.PlayaContentProvider}
 * <p/>
 * Created by davidbrodsky on 8/4/13.
 */
public class PlayaClient {

    /** Used by PlayaDatabase and DBWrapper */
    public static final int DATABASE_VERSION = 3;

    /** Geographic Bounds of Black Rock City
     * Used to determining whether a location lies
     * within the general vicinity
     * */
    public static final double MAX_LAT = 40.812161;
    public static final double MAX_LON = -119.170061;
    public static final double MIN_LAT = 40.764702;
    public static final double MIN_LON = -119.247798;

    public static LatLngBounds BRC_BOUNDS = LatLngBounds.builder()
            .include(new LatLng(MAX_LAT, MIN_LON))
            .include(new LatLng(MIN_LAT, MAX_LON))
            .build();

    private static SimpleDateFormat sDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    static {
        sDateFormatter.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
    }

    private static final boolean USE_BUNDLED_DB = true;

    private static final String UNLOCK_PW = SECRETS.UNLOCK_CODE;

    /**
     * SharedPreferences Keys
     */
    private static final String GENERAL_PREFS = "gen";
    private static final String FIRST_TIME = "first_time";
    private static final String POPULATED_DB_VERSION = "db_populated_ver";

    public static Date parseISODate(String isoDate) throws ParseException {
        return sDateFormatter.parse(isoDate);
    }

    public static String getISOString(Date date) {
        return sDateFormatter.format(date);
    }

    public static double getWalkingEstimateMinutes(double miles) {
        return 60 * (miles / 3.1);
    }

    /**
     * Will return true the first time this method is called
     * per app-installation.
     */
    public static boolean isFirstLaunch(Context c) {
        boolean firstTime = c.getSharedPreferences(GENERAL_PREFS, Context.MODE_PRIVATE)
                .getBoolean(FIRST_TIME, true);

        if (firstTime) {
            c.getSharedPreferences(GENERAL_PREFS, Context.MODE_PRIVATE).edit()
                    .putBoolean(FIRST_TIME, false)
                    .apply();
        }
        return firstTime;
    }

    /**
     * Returns whether the submitted guess is the correct
     * unlock password. As we don't believe in these things,
     * no security precautions are taken.
     */
    public static boolean validateUnlockPassword(String guess) {
        return guess.equals(UNLOCK_PW);
    }

    public static boolean isEmbargoClear(Context c) {
        boolean isClear = c.getSharedPreferences(GENERAL_PREFS, Context.MODE_PRIVATE).getBoolean(Constants.EMBARGO_CLEAR, false);
        if (!isClear && Constants.EMBARGO_DATE.before(new GregorianCalendar())) {
            setEmbargoClear(c, true);
            isClear = !isClear;
        }
        return isClear;
    }

    public static void setEmbargoClear(Context c, boolean isClear) {
        c.getSharedPreferences(GENERAL_PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(Constants.EMBARGO_CLEAR, isClear)
                .apply();
    }

    public static boolean isUsingBundledDb() {
        return USE_BUNDLED_DB;
    }

    public static boolean isDbPopulated(Context c) {
        // We added populated db ver tracking at DB version 2
        int dbVer = c.getSharedPreferences(GENERAL_PREFS, Context.MODE_PRIVATE).getInt(POPULATED_DB_VERSION, 2);
        if (USE_BUNDLED_DB && dbVer < DATABASE_VERSION) {
            Log.i("PC", "copying database");
            // Copy the pre-bundled database
            DBWrapper wrapper = new DBWrapper(c);
            wrapper.getReadableDatabase();
            setDbPopulated(c, DATABASE_VERSION);
        }
        return USE_BUNDLED_DB || (dbVer > 0);
    }

    public static void setDbPopulated(Context c, int populatedVer) {
        c.getSharedPreferences(GENERAL_PREFS, Context.MODE_PRIVATE).edit()
                .putInt(POPULATED_DB_VERSION, populatedVer)
                .apply();
    }

    public static LatLng getHomeLatLng(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(GENERAL_PREFS, Context.MODE_PRIVATE);
        double lat = prefs.getFloat(Constants.HOME_LAT, 0);
        double lon = prefs.getFloat(Constants.HOME_LON, 0);
        if (lat == 0 || lon == 0)
            return null;
        else
            return new LatLng(lat, lon);
    }

    public static void setHomeLatLng(Context c, LatLng latLng) {
        SharedPreferences.Editor editor = c.getSharedPreferences(GENERAL_PREFS, Context.MODE_PRIVATE).edit();
        editor.putFloat(Constants.HOME_LAT, (float) latLng.latitude);
        editor.putFloat(Constants.HOME_LON, (float) latLng.longitude);
        editor.apply();
    }
}
