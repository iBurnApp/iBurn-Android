package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.location.Location;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.Geo;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.squareup.sqlbrite.SqlBrite;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    public static final SimpleDateFormat dayAbbrevFormatter = new SimpleDateFormat("EE M/d", Locale.US);

    static {
//        sDayNames.add("All Days");
//        sDayAbbreviations.add(null);
        sDayNames.add("Monday 8/29");
        sDayAbbreviations.add("8/29");
        sDayNames.add("Tuesday 8/30");
        sDayAbbreviations.add("8/30");
        sDayNames.add("Wednesday 8/31");
        sDayAbbreviations.add("8/31");
        sDayNames.add("Thursday 9/1");
        sDayAbbreviations.add("9/1");
        sDayNames.add("Friday 9/2");
        sDayAbbreviations.add("9/2");
        sDayNames.add("Saturday 9/3");
        sDayAbbreviations.add("9/3");
        sDayNames.add("Sunday 9/4");
        sDayAbbreviations.add("9/4");
        sDayNames.add("Monday 9/5");
        sDayAbbreviations.add("9/5");
        sDayNames.add("Tuesday 9/6");
        sDayAbbreviations.add("9/6");


        sEventTypeAbbreviations.add("work");
        sEventTypeNames.add("Work");
        sEventTypeAbbreviations.add("game");
        sEventTypeNames.add("Game");
        sEventTypeAbbreviations.add("adlt");
        sEventTypeNames.add("Adult");
        sEventTypeAbbreviations.add("prty");
        sEventTypeNames.add("Party");
        sEventTypeAbbreviations.add("perf");
        sEventTypeNames.add("Performance");
        sEventTypeAbbreviations.add("kid");
        sEventTypeNames.add("Kid");
        sEventTypeAbbreviations.add("food");
        sEventTypeNames.add("Food");
        sEventTypeAbbreviations.add("cere");
        sEventTypeNames.add("Ceremony");
        sEventTypeAbbreviations.add("care");
        sEventTypeNames.add("Care");
        sEventTypeAbbreviations.add("fire");
        sEventTypeNames.add("Fire");
    }

    /**
     * @return the abbreviation for the current day, if it's during the burn, else the first day of the burn
     */
    public static String getCurrentOrFirstDayAbbreviation() {
        String todayAbbrev = dayAbbrevFormatter.format(new Date());
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
    public static void setDistanceText(Location deviceLocation, Date nowDate, String startDateStr, String endDateStr, TextView walkTimeView, TextView bikeTimeView, float lat, float lon) {
        if (deviceLocation != null && lat != 0) {
            double metersToTarget = Geo.getDistance(lat, lon, deviceLocation);
            int walkingMinutesToTarget = (int) Geo.getWalkingEstimateMinutes(metersToTarget);
            int bikingMinutesToTarget = (int) Geo.getBikingEstimateMinutes(metersToTarget);

            String distanceText;
            Context context = walkTimeView.getContext();

            try {
                Date startDate = startDateStr != null ? PlayaDateTypeAdapter.iso8601Format.parse(startDateStr) : null;
                Date endDate = endDateStr != null ? PlayaDateTypeAdapter.iso8601Format.parse(endDateStr) : null;

                walkTimeView.setText(createSpannableForDistance(context, walkingMinutesToTarget, nowDate, startDate, endDate));
                bikeTimeView.setText(createSpannableForDistance(context, bikingMinutesToTarget, nowDate, startDate, endDate));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            // If minutes < startDate || minutes < (endDate - now)
            walkTimeView.setVisibility(View.VISIBLE);
            bikeTimeView.setVisibility(View.VISIBLE);
        } else {
            walkTimeView.setText("");
            walkTimeView.setVisibility(View.GONE);
            bikeTimeView.setText("");
            bikeTimeView.setVisibility(View.GONE);
        }
    }

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
                long timeLeftMinutes = ( endDate.getTime() - nowDate.getTime() ) / 1000 / 60;
                //Timber.d("ongoing event ends in " + timeLeftMinutes + " minutes ( " + endDateStr + ") eta " + minutesToTarget + " duration " + duration);
                if ( (timeLeftMinutes - minutesToTarget) > 0) {
                    // If we'll make at least a quarter of the event, Color it yellow
                    int endSpan = distanceText.indexOf("min") + 3;
                    spanRange = new SpannableString(distanceText);
                    TextAppearanceSpan tas = new TextAppearanceSpan(context, R.style.OrangeText);
                    spanRange.setSpan(tas, 0, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else if (startDate.after(nowDate)) {
                long timeUntilStartMinutes = ( startDate.getTime() - nowDate.getTime() ) / 1000 / 60;
                //Timber.d("future event starts in " + timeUntilStartMinutes + " minutes ( " + startDateStr + ") eta " + minutesToTarget + " duration " + duration);
                if ( (timeUntilStartMinutes - minutesToTarget) > 0) {
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
