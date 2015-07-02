package com.gaiagps.iburn;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by davidbrodsky on 7/1/15.
 */
public class PrefsHelper {

    private static final String SHOWED_WELCOME = "welcomed";                // boolean
    private static final String POPULATED_DB_VERSION = "db_populated_ver";  // long
    private static final String VALID_UNLOCK_CODE = "unlocked_2015";        // boolean

    private static final String SHARED_PREFS_NAME = PrefsHelper.class.getSimpleName();

    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor editor;

    public PrefsHelper(Context context) {
        sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        editor = sharedPrefs.edit();
    }

    /**
     * @return whether a valid unlock code has been presented for this year
     */
    public boolean enteredValidUnlockCode() {
        return sharedPrefs.getBoolean(VALID_UNLOCK_CODE, false);
    }

    public void setEnteredValidUnlockCode(boolean didEnter) {
        editor.putBoolean(VALID_UNLOCK_CODE, didEnter);
    }

    public boolean didShowWelcome() {
        return sharedPrefs.getBoolean(SHOWED_WELCOME, true);
    }

    public void setDidShowWelcome(boolean didShow) {
        editor.putBoolean(SHOWED_WELCOME, didShow).apply();
    }

    public void setDatabaseVersion(long version) {
        editor.putLong(POPULATED_DB_VERSION, version);
        editor.commit();
    }

    public long getDatabaseVersion() {
        return sharedPrefs.getLong(POPULATED_DB_VERSION, 0);
    }
}
