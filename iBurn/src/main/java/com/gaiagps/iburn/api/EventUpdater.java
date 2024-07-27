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

