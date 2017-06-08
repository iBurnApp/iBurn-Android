package com.gaiagps.iburn;

import com.mapbox.mapboxsdk.Mapbox;

import timber.log.Timber;

/**
 * Created by davidbrodsky on 6/12/15.
 */
public class iBurnApp extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        Mapbox.getInstance(getApplicationContext(), SECRETS.MAPBOX_API_KEY);

        // If we abandon Timber logging in this app, enable below line
        // to enable Timber logging in any library modules that use it
        //Logging.forceLogging();
    }
}