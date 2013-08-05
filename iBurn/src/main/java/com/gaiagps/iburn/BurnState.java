package com.gaiagps.iburn;

/**
 * This class keeps track of application-wide state
 * Created by davidbrodsky on 8/4/13.
 */
public class BurnState {
    // These values are persisted in SharedPreferences
    // and are cached here for convenience
    public static boolean embargoClear = true;
    public static boolean dbReady = true;
}
