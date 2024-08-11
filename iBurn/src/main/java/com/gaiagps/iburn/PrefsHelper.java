package com.gaiagps.iburn;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by davidbrodsky on 7/1/15.
 */
public class PrefsHelper {

    // Year specific preferences: These keys should be combined with the
    // current event year.
    private static final String SHOWED_WELCOME = "welcomed";                // boolean
    private static final String VALID_UNLOCK_CODE = "unlocked";             // boolean
    private static final String COPIED_MBTILES_VERSION = "copied_tiles";    // long

    private static final String POPULATED_DB_VERSION = "db_populated_ver";  // long
    private static final String SCHEDULED_UPDATE = "sched_update";          // boolean

    private static final String DEFAULT_RESOURCE_VERSION = "resver";        // long
    private static final String RESOURCE_VERSION_PREFIX = "res-";           // long

    private static final String SHARED_PREFS_NAME = PrefsHelper.class.getSimpleName();

    private Context context;
    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor editor;

    public PrefsHelper(Context context) {
        this.context = context;
        sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        editor = sharedPrefs.edit();
    }

    /**
     * @return a key that is a combination of the given baseKey with the current app year.
     * This is handy when you want preferences set in a previous app release to be cleared
     * on update.
     */
    private String getAnnualKey(String baseKey) {
        return String.format("%s_%s", baseKey, context.getString(R.string.current_year));
    }

    /**
     * @return whether a valid unlock code has been presented for this year
     */
    public boolean enteredValidUnlockCode() {
        return sharedPrefs.getBoolean(getAnnualKey(VALID_UNLOCK_CODE), false);
    }

    public void setEnteredValidUnlockCode(boolean didEnter) {
        editor.putBoolean(getAnnualKey(VALID_UNLOCK_CODE), didEnter).apply();
    }

    public boolean didShowWelcome() {
        return sharedPrefs.getBoolean(getAnnualKey(SHOWED_WELCOME), false);
    }

    public void setDidShowWelcome(boolean didShow) {
        editor.putBoolean(getAnnualKey(SHOWED_WELCOME), didShow).commit();
    }

    public void setCopiedMbtilesVersion(long version) {
        editor.putLong(getAnnualKey(getAnnualKey(COPIED_MBTILES_VERSION)), version).commit();
    }

    public long getCopiedMbtilesVersion() {
        return sharedPrefs.getLong(COPIED_MBTILES_VERSION, 0);
    }

    public void setDatabaseVersion(long version) {
        editor.putLong(POPULATED_DB_VERSION, version).commit();
    }

    public long getDatabaseVersion() {
        return sharedPrefs.getLong(POPULATED_DB_VERSION, 0);
    }

    /**
     * @return whether the application successfully registered a {@link com.gaiagps.iburn.service.DataUpdateService} task
     */
    public boolean didScheduleUpdate() {
        return sharedPrefs.getBoolean(getAnnualKey(SCHEDULED_UPDATE), false);
    }

    public void setDidScheduleUpdate(boolean didScheduleUpdate) {
        editor.putBoolean(getAnnualKey(SCHEDULED_UPDATE), didScheduleUpdate).apply();
    }

    public void setBaseResourcesVersion(long version) {
        editor.putLong(DEFAULT_RESOURCE_VERSION, version).commit();
    }

    public long getResourceVersion(String resourceName) {
        return sharedPrefs.getLong(RESOURCE_VERSION_PREFIX + resourceName, sharedPrefs.getLong(DEFAULT_RESOURCE_VERSION, 0));
    }

    public void setResourceVersion(String resourceName, long resourceVersion) {
        editor.putLong(RESOURCE_VERSION_PREFIX + resourceName, resourceVersion).apply();
    }
}
