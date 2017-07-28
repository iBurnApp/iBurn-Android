package com.gaiagps.iburn;

import com.gaiagps.iburn.log.DiskLogger;
import com.mapbox.mapboxsdk.Mapbox;

import timber.log.Timber;

import static com.gaiagps.iburn.SECRETSKt.MAPBOX_API_KEY;

/**
 * Created by davidbrodsky on 6/12/15.
 */
public class iBurnApp extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Timber.plant(DiskLogger.getSharedInstance(getApplicationContext()));

        Mapbox.getInstance(getApplicationContext(), MAPBOX_API_KEY);

        // If we abandon Timber logging in this app, enable below line
        // to enable Timber logging in any library modules that use it
        //Logging.forceLogging();
    }
}