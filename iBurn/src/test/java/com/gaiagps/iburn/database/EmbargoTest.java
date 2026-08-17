package com.gaiagps.iburn.database;

import com.gaiagps.iburn.CurrentDateProvider;
import com.gaiagps.iburn.EventInfo;
import com.gaiagps.iburn.PrefsHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EmbargoTest {

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private PrefsHelper mockPrefs(boolean hasUnlock) {
        PrefsHelper prefs = mock(PrefsHelper.class);
        when(prefs.enteredValidUnlockCode()).thenReturn(hasUnlock);
        return prefs;
    }

    private Date dayBefore(Date date) {
        return new Date(date.getTime() - DAY_MS);
    }

    private Date dayAfter(Date date) {
        return new Date(date.getTime() + DAY_MS);
    }

    @Before
    public void resetEmbargoFlags() throws Exception {
        // Reset Embargo's static state between tests to avoid cross-test leakage
        java.lang.reflect.Field locationEnded = Embargo.class.getDeclaredField("didLocationEmbargoEnd");
        locationEnded.setAccessible(true);
        locationEnded.setBoolean(null, false);
        java.lang.reflect.Field campAddressEnded = Embargo.class.getDeclaredField("didCampAddressEmbargoEnd");
        campAddressEnded.setAccessible(true);
        campAddressEnded.setBoolean(null, false);
    }

    @Test
    public void art_beforeLocationEmbargo_withoutUnlock_returnsTrue() {
        PrefsHelper prefs = mockPrefs(false);
        Art art = new Art();

        try (MockedStatic<CurrentDateProvider> mocked = org.mockito.Mockito.mockStatic(CurrentDateProvider.class)) {
            mocked.when(CurrentDateProvider::getCurrentDate).thenReturn(dayBefore(EventInfo.LOCATION_EMBARGO_DATE));
            assertTrue(Embargo.isEmbargoActiveForPlayaItem(prefs, art));
        }
    }

    @Test
    public void camp_beforeGatesOpen_withoutUnlock_exactLocationIsEmbargoed() {
        PrefsHelper prefs = mockPrefs(false);
        Camp camp = new Camp();

        try (MockedStatic<CurrentDateProvider> mocked = org.mockito.Mockito.mockStatic(CurrentDateProvider.class)) {
            mocked.when(CurrentDateProvider::getCurrentDate).thenReturn(dayBefore(EventInfo.LOCATION_EMBARGO_DATE));
            assertTrue(Embargo.isEmbargoActiveForPlayaItem(prefs, camp));
        }
    }

    @Test
    public void event_withCampHost_beforeGatesOpen_withoutUnlock_exactLocationIsEmbargoed() {
        PrefsHelper prefs = mockPrefs(false);
        Event event = new Event();
        event.campPlayaId = "123";

        try (MockedStatic<CurrentDateProvider> mocked = org.mockito.Mockito.mockStatic(CurrentDateProvider.class)) {
            mocked.when(CurrentDateProvider::getCurrentDate).thenReturn(dayBefore(EventInfo.LOCATION_EMBARGO_DATE));
            assertTrue(Embargo.isEmbargoActiveForPlayaItem(prefs, event));
        }
    }

    @Test
    public void event_withArtHost_beforeLocationEmbargo_withoutUnlock_returnsTrue() {
        PrefsHelper prefs = mockPrefs(false);
        Event event = new Event();
        event.artPlayaId = "A1";

        try (MockedStatic<CurrentDateProvider> mocked = org.mockito.Mockito.mockStatic(CurrentDateProvider.class)) {
            mocked.when(CurrentDateProvider::getCurrentDate).thenReturn(dayBefore(EventInfo.LOCATION_EMBARGO_DATE));
            assertTrue(Embargo.isEmbargoActiveForPlayaItem(prefs, event));
        }
    }

    @Test
    public void oneWeekBeforeGates_campAddressVisible_butExactLocationsRemainEmbargoed() {
        PrefsHelper prefs = mockPrefs(false);
        Art art = new Art();
        Camp camp = new Camp();
        Event eventCamp = new Event();
        eventCamp.campPlayaId = "123";
        Event eventArt = new Event();
        eventArt.artPlayaId = "A1";

        Date afterCampAddressEmbargoBeforeGates = dayAfter(EventInfo.CAMP_ADDRESS_EMBARGO_DATE);
        if (!afterCampAddressEmbargoBeforeGates.before(EventInfo.LOCATION_EMBARGO_DATE)) {
            throw new AssertionError("Test setup invalid: expected a date before gates open");
        }

        try (MockedStatic<CurrentDateProvider> mocked = org.mockito.Mockito.mockStatic(CurrentDateProvider.class)) {
            mocked.when(CurrentDateProvider::getCurrentDate).thenReturn(afterCampAddressEmbargoBeforeGates);
            assertTrue(Embargo.isEmbargoActiveForPlayaItem(prefs, art));
            assertTrue(Embargo.isEmbargoActiveForPlayaItem(prefs, camp));
            assertTrue(Embargo.isEmbargoActiveForPlayaItem(prefs, eventCamp));
            assertTrue(Embargo.isEmbargoActiveForPlayaItem(prefs, eventArt));
            assertFalse(Embargo.isAddressEmbargoActiveForPlayaItem(prefs, camp));
            assertFalse(Embargo.isAddressEmbargoActiveForPlayaItem(prefs, eventCamp));
            assertTrue(Embargo.isAddressEmbargoActiveForPlayaItem(prefs, art));
            assertTrue(Embargo.isAddressEmbargoActiveForPlayaItem(prefs, eventArt));
            assertTrue(Embargo.isCampMapEmbargoActive(prefs));
        }
    }

    @Test
    public void beforeOneWeekWindow_campAddressIsEmbargoed() {
        PrefsHelper prefs = mockPrefs(false);
        Camp camp = new Camp();

        try (MockedStatic<CurrentDateProvider> mocked = org.mockito.Mockito.mockStatic(CurrentDateProvider.class)) {
            mocked.when(CurrentDateProvider::getCurrentDate).thenReturn(dayBefore(EventInfo.CAMP_ADDRESS_EMBARGO_DATE));
            assertTrue(Embargo.isAddressEmbargoActiveForPlayaItem(prefs, camp));
        }
    }

    @Test
    public void campAddressEmbargo_startsExactlyOneWeekBeforeEventStart() {
        assertEquals(7L * DAY_MS,
                EventInfo.EVENT_START_DATE.getTime() - EventInfo.CAMP_ADDRESS_EMBARGO_DATE.getTime());

        PrefsHelper prefs = mockPrefs(false);
        Camp camp = new Camp();
        try (MockedStatic<CurrentDateProvider> mocked = org.mockito.Mockito.mockStatic(CurrentDateProvider.class)) {
            mocked.when(CurrentDateProvider::getCurrentDate).thenReturn(EventInfo.CAMP_ADDRESS_EMBARGO_DATE);
            assertFalse(Embargo.isAddressEmbargoActiveForPlayaItem(prefs, camp));
        }
    }

    @Test
    public void afterLocationEmbargo_withoutUnlock_allFalse() {
        PrefsHelper prefs = mockPrefs(false);
        Art art = new Art();
        Camp camp = new Camp();
        Event eventCamp = new Event();
        eventCamp.campPlayaId = "123";
        Event eventArt = new Event();
        eventArt.artPlayaId = "A1";

        try (MockedStatic<CurrentDateProvider> mocked = org.mockito.Mockito.mockStatic(CurrentDateProvider.class)) {
            mocked.when(CurrentDateProvider::getCurrentDate).thenReturn(EventInfo.LOCATION_EMBARGO_DATE);
            assertFalse(Embargo.isEmbargoActiveForPlayaItem(prefs, art));
            assertFalse(Embargo.isEmbargoActiveForPlayaItem(prefs, camp));
            assertFalse(Embargo.isEmbargoActiveForPlayaItem(prefs, eventCamp));
            assertFalse(Embargo.isEmbargoActiveForPlayaItem(prefs, eventArt));
            assertFalse(Embargo.isCampMapEmbargoActive(prefs));
        }
    }

    @Test
    public void withUnlock_beforeEmbargo_allFalse() {
        PrefsHelper prefs = mockPrefs(true);
        Art art = new Art();
        Camp camp = new Camp();
        Event eventCamp = new Event();
        eventCamp.campPlayaId = "123";
        Event eventArt = new Event();
        eventArt.artPlayaId = "A1";

        // Pick a date before the earliest embargo to ensure it would otherwise be active
        Date beforeAnyEmbargo = dayBefore(EventInfo.CAMP_ADDRESS_EMBARGO_DATE);

        try (MockedStatic<CurrentDateProvider> mocked = org.mockito.Mockito.mockStatic(CurrentDateProvider.class)) {
            mocked.when(CurrentDateProvider::getCurrentDate).thenReturn(beforeAnyEmbargo);
            assertFalse(Embargo.isEmbargoActiveForPlayaItem(prefs, art));
            assertFalse(Embargo.isEmbargoActiveForPlayaItem(prefs, camp));
            assertFalse(Embargo.isEmbargoActiveForPlayaItem(prefs, eventCamp));
            assertFalse(Embargo.isEmbargoActiveForPlayaItem(prefs, eventArt));
        }
    }

    @Test
    public void event_withNoHost_neverEmbargoed() {
        PrefsHelper prefs = mockPrefs(false);
        Event event = new Event();

        try (MockedStatic<CurrentDateProvider> mocked = org.mockito.Mockito.mockStatic(CurrentDateProvider.class)) {
            // Before any embargo
            mocked.when(CurrentDateProvider::getCurrentDate).thenReturn(dayBefore(EventInfo.CAMP_ADDRESS_EMBARGO_DATE));
            assertFalse(Embargo.isEmbargoActiveForPlayaItem(prefs, event));

            // Between camp-address and exact-location embargoes
            mocked.when(CurrentDateProvider::getCurrentDate).thenReturn(dayAfter(EventInfo.CAMP_ADDRESS_EMBARGO_DATE));
            assertFalse(Embargo.isEmbargoActiveForPlayaItem(prefs, event));

            // After location embargo
            mocked.when(CurrentDateProvider::getCurrentDate).thenReturn(dayAfter(EventInfo.LOCATION_EMBARGO_DATE));
            assertFalse(Embargo.isEmbargoActiveForPlayaItem(prefs, event));
        }
    }
}
