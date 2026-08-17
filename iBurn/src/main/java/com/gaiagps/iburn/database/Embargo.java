package com.gaiagps.iburn.database;

import com.gaiagps.iburn.CurrentDateProvider;
import com.gaiagps.iburn.EventInfo;
import com.gaiagps.iburn.PrefsHelper;

import java.util.Date;

import timber.log.Timber;

/**
 * A data restriction policy that ensures location data never leaves the database
 * before {@link #LOCATION_EMBARGO_DATE} and without {@link PrefsHelper#enteredValidUnlockCode()}
 * <p>
 * Created by davidbrodsky on 7/1/15.
 */
public class Embargo {

    // Embargo date is the day gates open
    public static final Date LOCATION_EMBARGO_DATE = EventInfo.LOCATION_EMBARGO_DATE;
    // Camp street addresses are public one week before gates open.
    public static final Date CAMP_ADDRESS_EMBARGO_DATE = EventInfo.CAMP_ADDRESS_EMBARGO_DATE;

    // For mock builds, force user to enter unlock code
    private static final boolean FORCE_EMBARGO = false;

    // We never go from no embargo -> embargo, so stop checking date after embargo ends
    private static boolean didLocationEmbargoEnd = false;
    private static boolean didCampAddressEmbargoEnd = false;

    public static boolean isEmbargoActiveForPlayaItem(PrefsHelper prefs, PlayaItem item) {
        // Determine embargo based on which core table is present in the query
        if (item instanceof MutantVehicle) {
            // The Mutant Vehicle feed does not contain location data.
            return false;
        } else if (item instanceof Art || item instanceof Camp ||
                (item instanceof Event && (((Event) item).hasArtHost() || ((Event) item).hasCampHost()))) {
            if (didLocationEmbargoEnd) return false;
            boolean result = isEmbargoActiveForDate(prefs, LOCATION_EMBARGO_DATE);
            if (!result) didLocationEmbargoEnd = true;
            return result;
        }
        Timber.e("Embargo: Cannot determine embargo for unknown PlayaItem type: %s id %s", item.getClass().getSimpleName(), item.playaId);
        return false;
    }

    public static boolean isAnyEmbargoActive(PrefsHelper prefs) {
        // Embargo is active if before date and no unlock code present
        return isEmbargoActiveForDate(prefs, LOCATION_EMBARGO_DATE);
    }

    /**
     * Addresses have a separate release policy from exact coordinates. Camp addresses become
     * public one week before gates open; art addresses remain hidden until gates open.
     */
    public static boolean isAddressEmbargoActiveForPlayaItem(PrefsHelper prefs, PlayaItem item) {
        if (item instanceof MutantVehicle) return false;
        if (item instanceof Art || (item instanceof Event && ((Event) item).hasArtHost())) {
            return isEmbargoActiveForDate(prefs, LOCATION_EMBARGO_DATE);
        }
        if (item instanceof Camp || (item instanceof Event && ((Event) item).hasCampHost())) {
            if (didCampAddressEmbargoEnd) return false;
            boolean result = isEmbargoActiveForDate(prefs, CAMP_ADDRESS_EMBARGO_DATE);
            if (!result) didCampAddressEmbargoEnd = true;
            return result;
        }
        return false;
    }

    /** Exact camp map data, including boundaries and name layers, unlocks when gates open. */
    public static boolean isCampMapEmbargoActive(PrefsHelper prefs) {
        return isEmbargoActiveForDate(prefs, LOCATION_EMBARGO_DATE);
    }

    private static boolean isEmbargoActiveForDate(PrefsHelper prefs, Date embargoDate) {
        boolean embargoActive = (FORCE_EMBARGO || CurrentDateProvider.getCurrentDate().before(embargoDate));
        return embargoActive && !prefs.enteredValidUnlockCode();
    }
}
