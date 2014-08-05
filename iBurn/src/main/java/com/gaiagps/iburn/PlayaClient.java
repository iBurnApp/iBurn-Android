package com.gaiagps.iburn;

import android.content.Context;
import android.content.SharedPreferences;

import com.gaiagps.iburn.database.DBWrapper;
import com.google.android.gms.maps.model.LatLng;

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

    private static SimpleDateFormat sDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    static {
        sDateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final boolean USE_BUNDLED_DB = true;

    private static final String UNLOCK_PW = "snowden";

    /**
     * SharedPreferences Keys
     */
    private static final String GENERAL_PREFS = "gen";
    private static final String FIRST_TIME = "first_time";
    private static final String DB_POPULATED = "db_populated";

    public static Date parseISODate(String isoDate) throws ParseException {
        return sDateFormatter.parse(isoDate);
    }

    public static String getISOString(Date date) {
        return sDateFormatter.format(date);
    }

    public static int getWalkingEstimateHour(int meters) {
        return 60 * (meters / 4);
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

    public static boolean isDbPopulated(Context c) {
        boolean isPopulated = c.getSharedPreferences(GENERAL_PREFS, Context.MODE_PRIVATE).getBoolean(DB_POPULATED, false);
        if (!isPopulated && USE_BUNDLED_DB) {
            // Copy the pre-bundled database
            DBWrapper wrapper = new DBWrapper(c);
            wrapper.getReadableDatabase();
            setDbPopulated(c, true);
        }
        return USE_BUNDLED_DB || isPopulated;
    }

    public static void setDbPopulated(Context c, boolean isPopulated) {
        c.getSharedPreferences(GENERAL_PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(DB_POPULATED, isPopulated)
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
