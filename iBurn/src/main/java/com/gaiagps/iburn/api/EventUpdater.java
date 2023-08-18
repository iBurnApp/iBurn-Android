package com.gaiagps.iburn.api;

import android.content.Context;

import com.gaiagps.iburn.api.response.DataManifest;
import com.gaiagps.iburn.api.response.ResourceManifest;
import com.gaiagps.iburn.database.DataProvider;

import java.util.Date;

/**
 * The initial 2023 bundled db had 'pretty' time columns in EDT instead of PDT.
 * Fix this colossal bug by re-creating event table from JSON for affected installs.
 */
public class EventUpdater extends MockIBurnApi {
    public EventUpdater(Context context) {
        super(context);
    }

    public static boolean needsFix(Context context) {
        // As a test, use Black Rock City 5k event, which should have a formatted start time of
        // "Thu 8/31 9:30 AM", but was recorded as "Thu 8/31 9:30 AM" in the initial borked DB
        return DataProvider.Companion.getInstance(context)
                .flatMap(dataProvider -> dataProvider.observeEventByPlayaId("3y6Fs46vcvfYCVdmR8kU").toObservable())
                .map(event -> event.startTimePretty.equals("Thu 8/31 12:30 PM"))
                .blockingFirst();

    }

    @Override
    protected DataManifest buildManifest() {
        // Only indicate new event data is available
        ResourceManifest event = new ResourceManifest("event.json", new Date());
        ResourceManifest art = new ResourceManifest("art.json", new Date(0));
        ResourceManifest camp = new ResourceManifest("camp.json", new Date(0));
        return new DataManifest(art, camp, event);
    }
}

