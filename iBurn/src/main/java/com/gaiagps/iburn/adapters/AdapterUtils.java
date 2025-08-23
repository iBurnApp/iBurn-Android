package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.location.Location;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.widget.TextView;

import com.gaiagps.iburn.CurrentDateProvider;
import com.gaiagps.iburn.DateUtil;
import com.gaiagps.iburn.EventInfo;
import com.gaiagps.iburn.Geo;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by davidbrodsky on 8/4/14.
 */
public class AdapterUtils {

    /**
     * Mapping of event type abbreviation to display name. The linked map
     * preserves insertion order which is relied upon by UI components.
     */
    public static final LinkedHashMap<String, String> sEventTypes = new LinkedHashMap<>();

    public static final ArrayList<String> sDayAbbreviations = new ArrayList<>();
    public static final ArrayList<String> sDayNames = new ArrayList<>();

    public static final SimpleDateFormat dayLabelFormatter = DateUtil.getPlayaTimeFormat("EE M/d");
    public static final SimpleDateFormat dayAbbrevFormatter = DateUtil.getPlayaTimeFormat("M/d");

    public static final Date EVENT_START_DATE = EventInfo.EVENT_START_DATE;
    public static final Date EVENT_END_DATE = EventInfo.EVENT_END_DATE;

    public static final String EVENT_TYPE_ABBREVIATION_UNKNOWN = "unknwn";

    static {

        dayLabelFormatter.setTimeZone(DateUtil.PLAYA_TIME_ZONE);
        dayAbbrevFormatter.setTimeZone(DateUtil.PLAYA_TIME_ZONE);

        populateDayRanges(EVENT_START_DATE, EVENT_END_DATE);

        // Event types sourced from the iOS EventType enum
        sEventTypes.put("arts", "Arts & Crafts");
        sEventTypes.put("work", "Class/Workshop");
        sEventTypes.put("tea", "Coffee/Tea");
//        sEventTypes.put("RIDE", "Diversity & Inclusion");
//        sEventTypes.put("fire", "Fire/Spectacle");
        sEventTypes.put("food", "Food & Drink");
//        sEventTypes.put("game", "Games");
        sEventTypes.put("prty", "Gathering/Party");
//        sEventTypes.put("heal", "Healing/Massage/Spa");
        sEventTypes.put("kid", "Kids");
//        sEventTypes.put("LGBT", "LGBTQIA2S+");
//        sEventTypes.put("live", "Live Music");
        sEventTypes.put("adlt", "Mature Audiences");
        sEventTypes.put("othr", "Miscellaneous");
//        sEventTypes.put("para", "Parade");
//        sEventTypes.put("perf", "Performance");
//        sEventTypes.put("repr", "Repair");
//        sEventTypes.put("cere", "Ritual/Ceremony");
//        sEventTypes.put("sust", "Sustainability/Greening Your Burn");
//        sEventTypes.put("yoga", "Yoga/Movement/Fitness");

        // Initial 2017 data had uncategorized events but first update with official location
        // has all events categorized
    }

    private static void populateDayRanges(Date start, Date end) {
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(start);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(end);

        sDayNames.clear();
        sDayAbbreviations.clear();

        // Add "All Days" option as the first item
        sDayNames.add("All Days");
        sDayAbbreviations.add("");

        for (Date date = startCal.getTime(); startCal.before(endCal); startCal.add(Calendar.DATE, 1), date = startCal.getTime()) {
            sDayNames.add(dayLabelFormatter.format(date));
            sDayAbbreviations.add(dayAbbrevFormatter.format(date));
        }
    }

    private static final DateFormat apiDateFormat = PlayaDateTypeAdapter.buildIso8601Format();

    public static List<String> getEventTypeAbbreviations() {
        return new ArrayList<>(sEventTypes.keySet());
    }

    public static List<String> getEventTypeNames() {
        return new ArrayList<>(sEventTypes.values());
    }

    public static int getEventTypeCount() {
        return sEventTypes.size();
    }

    /**
     * @return the abbreviation for the current day, if it's during the burn, else "All Days"
     */
    public static String getCurrentOrFirstDayAbbreviation() {
        Date now = CurrentDateProvider.getCurrentDate();
        String todayAbbrev = dayAbbrevFormatter.format(now);
        // Skip index 0 which is "All Days" (empty string) when checking for current day
        if (sDayAbbreviations.size() > 1 && sDayAbbreviations.subList(1, sDayAbbreviations.size()).contains(todayAbbrev)) {
            return todayAbbrev;
        }

        // Default to "All Days" (empty string at index 0)
        return sDayAbbreviations.get(0);
    }

    public static String getStringForEventType(String typeAbbreviation) {
        if (typeAbbreviation == null) return null;
        return sEventTypes.get(typeAbbreviation);
    }

    public static void setDistanceText(Location deviceLocation, TextView walkTimeView, TextView bikeTimeView, float lat, float lon) {
        setDistanceText(deviceLocation, null, null, null, walkTimeView, bikeTimeView, lat, lon);
    }

    /**
     * Get stylized distance text describing the difference between the given
     * device location and a given Latitude and Longitude. The unique
     * method signature owes itself to the precise data available to
     * a {@link com.gaiagps.iburn.adapters.PlayaItemCursorAdapter}
     *
     * @return a time estimate in minutes.
     */
    public static void setDistanceText(Location deviceLocation, Date nowDate, Date startDate, Date endDate, TextView walkTimeView, TextView bikeTimeView, float lat, float lon) {
        if (deviceLocation != null && lat != 0) {
            double metersToTarget = Geo.getDistance(lat, lon, deviceLocation);
            int walkingMinutesToTarget = (int) Geo.getWalkingEstimateMinutes(metersToTarget);
            int bikingMinutesToTarget = (int) Geo.getBikingEstimateMinutes(metersToTarget);

            String distanceText;
            Context context = walkTimeView.getContext();

            walkTimeView.setText(createSpannableForDistance(context, walkingMinutesToTarget, nowDate, startDate, endDate));
            bikeTimeView.setText(createSpannableForDistance(context, bikingMinutesToTarget, nowDate, startDate, endDate));
            // If minutes < startDate || minutes < (endDate - now)
            walkTimeView.setVisibility(View.VISIBLE);
            bikeTimeView.setVisibility(View.VISIBLE);
        } else {
            walkTimeView.setText("");
            walkTimeView.setVisibility(View.INVISIBLE);
            bikeTimeView.setText("");
            bikeTimeView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * {@link #createSpannableForDistance(Context, int, Date, Date, Date)} colors walking / biking times
     * if they're reachable before ending time. However, don't color events far out into the future
     * as reachable because I think that adds more noise than value
     */
    private static final int SOON_EVENT_TIME_MS = 2 * 60 * 60 * 1000; // 2 hr

    private static Spannable createSpannableForDistance(Context context, int minutesToTarget, Date nowDate, Date startDate, Date endDate) {
        String distanceText;
        Spannable spanRange;

        if (minutesToTarget < 1) {
            distanceText = "<1 m";
            spanRange = new SpannableString(distanceText);
        } else {
            distanceText = String.format(Locale.US, "%d min", minutesToTarget);
            spanRange = new SpannableString(distanceText);
        }

        if (nowDate != null && startDate != null && endDate != null) {
            // If a date is given, attempt to do coloring of the time estimate (e.g: green if arrival estimate before start date)
            long duration = endDate.getTime() - startDate.getTime() / 1000 / 60; //minutes

            if (startDate.before(nowDate) && endDate.after(nowDate)) {
                // Event already started
                long timeLeftMinutes = (endDate.getTime() - nowDate.getTime()) / 1000 / 60;
                //Timber.d("ongoing event ends in " + timeLeftMinutes + " minutes ( " + endDateStr + ") eta " + minutesToTarget + " duration " + duration);
                if ((timeLeftMinutes - minutesToTarget) > 0) {
                    // If we'll make at least a quarter of the event, Color it yellow
                    int endSpan = distanceText.indexOf("min") + 3;
                    spanRange = new SpannableString(distanceText);
                    TextAppearanceSpan tas = new TextAppearanceSpan(context, R.style.OrangeText);
                    spanRange.setSpan(tas, 0, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else if (startDate.after(nowDate) && (startDate.getTime() - nowDate.getTime() < SOON_EVENT_TIME_MS)) {
                long timeUntilStartMinutes = (startDate.getTime() - nowDate.getTime()) / 1000 / 60;
                //Timber.d("future event starts in " + timeUntilStartMinutes + " minutes ( " + startDateStr + ") eta " + minutesToTarget + " duration " + duration);
                if ((timeUntilStartMinutes - minutesToTarget) > 0) {
                    // If we'll make the event start, Color it green
                    int endSpan = distanceText.indexOf("min") + 3;
                    TextAppearanceSpan tas = new TextAppearanceSpan(context, R.style.GreenText);
                    spanRange.setSpan(tas, 0, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        return spanRange;
    }
}
