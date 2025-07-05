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
import java.util.Locale;

/**
 * Created by davidbrodsky on 8/4/14.
 */
public class AdapterUtils {

    public static final ArrayList<String> sEventTypeAbbreviations = new ArrayList<>();
    public static final ArrayList<String> sEventTypeNames = new ArrayList<>();

    public static final ArrayList<String> sDayAbbreviations = new ArrayList<>();
    public static final ArrayList<String> sDayNames = new ArrayList<>();

    public static final SimpleDateFormat dayLabelFormatter = DateUtil.getPlayaTimeFormat("EE M/d");
    public static final SimpleDateFormat dayAbbrevFormatter = DateUtil.getPlayaTimeFormat("M/d");

    public static final Date EVENT_START_DATE = EventInfo.EVENT_START_DATE;
    public static final Date EVENT_END_DATE = EventInfo.EVENT_END_DATE;

    public static final String EVENT_TYPE_ABBREVIATION_UNKNOWN = "unknwn";
    public static final String EVENT_TYPE_NAME_UNKNOWN = "Uncategorized";

    static {

        dayLabelFormatter.setTimeZone(DateUtil.PLAYA_TIME_ZONE);
        dayAbbrevFormatter.setTimeZone(DateUtil.PLAYA_TIME_ZONE);

        populateDayRanges(EVENT_START_DATE, EVENT_END_DATE);
        sEventTypeAbbreviations.add("work");
        sEventTypeNames.add("Class/Workshop");
        sEventTypeAbbreviations.add("perf");
        sEventTypeNames.add("Performance");
        sEventTypeAbbreviations.add("care");
        sEventTypeNames.add("Self Care");
        sEventTypeAbbreviations.add("prty");
        sEventTypeNames.add("Gathering/Party");
        sEventTypeAbbreviations.add("cere");
        sEventTypeNames.add("Ritual/Ceremony");
        sEventTypeAbbreviations.add("game");
        sEventTypeNames.add("Games");
        sEventTypeAbbreviations.add("fire");
        sEventTypeNames.add("Fire/Spectacle");
        sEventTypeAbbreviations.add("adlt");
        sEventTypeNames.add("Mature Audiences");
        sEventTypeAbbreviations.add("kid");
        sEventTypeNames.add("For Kids");
        sEventTypeAbbreviations.add("food");
        sEventTypeNames.add("Food & Drink");
        sEventTypeAbbreviations.add("othr");
        sEventTypeNames.add("Miscellaneous");
        sEventTypeAbbreviations.add("arts");
        sEventTypeNames.add("Arts & Crafts");
        sEventTypeAbbreviations.add("live");
        sEventTypeNames.add("Live Music");
        sEventTypeAbbreviations.add("RIDE");
        sEventTypeNames.add("Diversity & Inclusion");
        sEventTypeAbbreviations.add("repr");
        sEventTypeNames.add("Repair");
        sEventTypeAbbreviations.add("sust");
        sEventTypeNames.add("Sustainability/Greening Your Burn");
        sEventTypeAbbreviations.add("yoga");
        sEventTypeNames.add("Yoga/Movement/Fitness");
        // Initial 2017 data had uncategorized events but first update with official location
        // has all events categorized
//        sEventTypeAbbreviations.add(EVENT_TYPE_ABBREVIATION_UNKNOWN);
//        sEventTypeNames.add(EVENT_TYPE_NAME_UNKNOWN);
    }

    private static void populateDayRanges(Date start, Date end) {
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(start);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(end);

        sDayNames.clear();
        sDayAbbreviations.clear();

        for (Date date = startCal.getTime(); startCal.before(endCal); startCal.add(Calendar.DATE, 1), date = startCal.getTime()) {
            sDayNames.add(dayLabelFormatter.format(date));
            sDayAbbreviations.add(dayAbbrevFormatter.format(date));
        }
    }

    private static final DateFormat apiDateFormat = PlayaDateTypeAdapter.buildIso8601Format();

    /**
     * @return the abbreviation for the current day, if it's during the burn, else the first day of the burn
     */
    public static String getCurrentOrFirstDayAbbreviation() {
        Date now = CurrentDateProvider.getCurrentDate();
        String todayAbbrev = dayAbbrevFormatter.format(now);
        if (sDayAbbreviations.contains(todayAbbrev)) return todayAbbrev;

        return sDayAbbreviations.get(0);
    }

    public static String getStringForEventType(String typeAbbreviation) {
        if (typeAbbreviation == null) return null;
        if (sEventTypeAbbreviations.contains(typeAbbreviation))
            return sEventTypeNames.get(sEventTypeAbbreviations.indexOf(typeAbbreviation));
        return null;
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
