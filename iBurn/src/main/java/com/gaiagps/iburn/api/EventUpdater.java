package com.gaiagps.iburn.api;

import android.content.Context;

import com.gaiagps.iburn.api.response.DataManifest;
import com.gaiagps.iburn.api.response.ResourceManifest;
import com.gaiagps.iburn.database.DataProvider;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * The initial 2023 bundled db had 'pretty' time columns in EDT instead of PDT.
 * Fix this colossal bug by re-creating event table from JSON for affected installs.
 *
 * Then, that initial fix which bundled only event json ended up wiping all event locations,
 * since those are cross referenced from camp and art locations.
 */
public class EventUpdater extends MockIBurnApi {
    public EventUpdater(Context context) {
        super(context);
    }

    public static boolean needsFix(Context context) {
        // As a test, use Black Rock City 5k event, which should have a formatted start time of
        // "Thu 8/31 9:30 AM", but was recorded as "Thu 8/31 12:30 PM" in the initial borked DB
        boolean generalLocationOrTimeIssuesPresent = DataProvider.Companion.getInstance(context)
                .flatMap(dataProvider -> dataProvider.observeEventByPlayaId("3y6Fs46vcvfYCVdmR8kU").toObservable())
                .timeout(1, TimeUnit.SECONDS)
                .map(event -> {
                    boolean hasWrongTime = event.startTimePretty.equals("Thu 8/31 12:30 PM");
                    boolean hasMissingLocation = !event.hasLocation();
                    return hasWrongTime || hasMissingLocation;
                })
                .onErrorReturnItem(false)
                .blockingFirst();
        if (generalLocationOrTimeIssuesPresent) {
            return true;
        }
        // Paranormal hour event has a location set by a camp in a plaza, which was not
        // geocoded properly due to presence of 'None None' string in playa address. This is
        // representative of a large group of events.
        return DataProvider.Companion.getInstance(context)
                .flatMap(dataProvider -> dataProvider.observeEventByPlayaId("5np2awDjcSkko2YXgprV").toObservable())
                .timeout(1, TimeUnit.SECONDS)
                .map(event -> {
                    boolean hasBadPlayaAddress = event.playaAddress.contains("None None");
                    boolean hasMissingLocation = !event.hasLocation();
                    return hasBadPlayaAddress || hasMissingLocation;
                })
                .onErrorReturnItem(false)
                .blockingFirst();
    }

    @Override
    protected DataManifest buildManifest() {
        // We'll update all data from JSON, even though it's only the event table that is
        // problematic because I believe the lowest risk option is using the most tested (normal) update path
        ResourceManifest event = new ResourceManifest("event.json", new Date());
        ResourceManifest art = new ResourceManifest("art.json", new Date());
        ResourceManifest camp = new ResourceManifest("camp.json", new Date());
        return new DataManifest(art, camp, event);
    }
}

