package com.gaiagps.iburn.database;

import com.gaiagps.iburn.CurrentDateProvider;
import com.gaiagps.iburn.EventInfo;
import com.gaiagps.iburn.PrefsHelper;

import java.util.Date;

import timber.log.Timber;

/**
 * A data restriction policy that ensures location data never leaves the database
 * before {@link #EMBARGO_DATE} and without {@link PrefsHelper#enteredValidUnlockCode()}
 * <p>
 * Created by davidbrodsky on 7/1/15.
 */
public class Embargo {

    // Embargo date is the day gates open
    // Art locations embargo date (existing behavior)
    public static final Date EMBARGO_DATE = EventInfo.EMBARGO_DATE;
    // Camps and Events locations embargo date (new, separate)
    public static final Date CAMP_EVENT_EMBARGO_DATE = EventInfo.CAMP_EMBARGO_DATE;

    // For mock builds, force user to enter unlock code
    private static final boolean FORCE_EMBARGO = false;

    // We never go from no embargo -> embargo, so stop checking date after embargo ends
    private static boolean didCampEmbargoEnd = false;
    private static boolean didArtEmbargoEnd = false;

    public static boolean isEmbargoActiveForPlayaItem(PrefsHelper prefs, PlayaItem item) {
        // Determine embargo based on which core table is present in the query
        if (item instanceof Art || (item instanceof Event && ((Event) item).hasArtHost())) {
            if (didArtEmbargoEnd) return false;
            boolean result = isEmbargoActiveForArt(prefs);
            // Once the art embargo has ended, it will never be active again
            if (!result) didArtEmbargoEnd = true;
            return result;
        } else if (item instanceof Camp || (item instanceof Event && ((Event) item).hasCampHost())) {
            if (didCampEmbargoEnd) return false;
            boolean result = isEmbargoActiveForCamp(prefs);
            // Once the camp embargo has ended, it will never be active again
            if (!result) didCampEmbargoEnd = true;
            return result;
        }
        Timber.e("Embargo: Cannot determine embargo for unknown PlayaItem type: %s", item.getClass().getSimpleName());
        return false;
    }

    public static boolean isAnyEmbargoActive(PrefsHelper prefs) {
        // Embargo is active if before date and no unlock code present
        return isEmbargoActiveForDate(prefs, EMBARGO_DATE);
    }

    private static boolean isEmbargoActiveForArt(PrefsHelper prefs) {
        return isEmbargoActiveForDate(prefs, EMBARGO_DATE);
    }

    private static boolean isEmbargoActiveForCamp(PrefsHelper prefs) {
        return isEmbargoActiveForDate(prefs, CAMP_EVENT_EMBARGO_DATE);
    }

    private static boolean isEmbargoActiveForDate(PrefsHelper prefs, Date embargoDate) {
        boolean embargoActive = (FORCE_EMBARGO || CurrentDateProvider.getCurrentDate().before(embargoDate));
        return embargoActive && !prefs.enteredValidUnlockCode();
    }
}
