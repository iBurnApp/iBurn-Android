package com.gaiagps.iburn.adapters;

import android.content.ContentValues;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.GeoUtils;
import com.gaiagps.iburn.PlayaClient;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.PlayaContentProvider;
import com.gaiagps.iburn.database.PlayaItemTable;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by davidbrodsky on 8/4/14.
 */
public class AdapterUtils {
    private static final String TAG = "AdapterUtils";

    public static ArrayList<String> sEventTypeAbbreviations = new ArrayList<>();
    public static ArrayList<String> sEventTypeNames = new ArrayList<>();

    public static ArrayList<String> sDayAbbreviations = new ArrayList<>();
    public static ArrayList<String> sDayNames = new ArrayList<>();

    static {
        sDayNames.add("All Days");
        sDayAbbreviations.add(null);
        sDayNames.add("Monday 8/25");
        sDayAbbreviations.add("8/25");
        sDayNames.add("Tuesday 8/26");
        sDayAbbreviations.add("8/26");
        sDayNames.add("Wednesday 8/27");
        sDayAbbreviations.add("8/27");
        sDayNames.add("Thursday 8/28");
        sDayAbbreviations.add("8/28");
        sDayNames.add("Friday 8/29");
        sDayAbbreviations.add("8/29");
        sDayNames.add("Saturday 8/30");
        sDayAbbreviations.add("8/30");
        sDayNames.add("Sunday 8/31");
        sDayAbbreviations.add("8/31");
        sDayNames.add("Monday 9/1");
        sDayAbbreviations.add("9/1");
        sDayNames.add("Tuesday 9/2");
        sDayAbbreviations.add("9/2");

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

    public static String getStringForEventType(String typeAbbreviation) {
        if (typeAbbreviation == null) return null;
        if (sEventTypeAbbreviations.contains(typeAbbreviation))
            return sEventTypeNames.get(sEventTypeAbbreviations.indexOf(typeAbbreviation));
        return null;
    }

    public static double setDistanceText(Location deviceLocation, TextView textView, double lat, double lon) {
        return setDistanceText(deviceLocation, null, null, null, textView, lat, lon);
    }

    /**
     * Get stylized distance text describing the difference between the given
     * device location and a given Latitude and Longitude. The unique
     * method signature owes itself to the precise data available to
     * a {@link com.gaiagps.iburn.adapters.PlayaItemCursorAdapter}
     *
     * @return a time estimate in minutes.
     */
    public static double setDistanceText(Location deviceLocation, Date nowDate, String startDateStr, String endDateStr, TextView textView, double lat, double lon) {
        if (deviceLocation != null && lat != 0) {
            double milesToTarget = (GeoUtils.getDistance(lat, lon, deviceLocation)) * 0.000621371; // meters to miles
            double minutesToTarget = PlayaClient.getWalkingEstimateMinutes(milesToTarget);
            String distanceText;

            try {
                Spannable spanRange;

                if (milesToTarget < 0.01) {
                    distanceText = "<1 m";
                    spanRange = new SpannableString(distanceText);
                } else {
                    distanceText = String.format("%.0f min %.2f miles", minutesToTarget, milesToTarget);
                    int endSpan = distanceText.length();
                    spanRange = new SpannableString(distanceText);
                    TextAppearanceSpan tas = new TextAppearanceSpan(textView.getContext(), R.style.Subdued);
                    spanRange.setSpan(tas, distanceText.indexOf("min") + 3, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                if (nowDate != null && startDateStr != null && endDateStr != null) {
                    // If a date is given, attempt to do coloring of the time estimate (e.g: green if arrival estimate before start date)
                    Date startDate = PlayaClient.parseISODate(startDateStr);
                    Date endDate = PlayaClient.parseISODate(endDateStr);
                    long duration = endDate.getTime() - startDate.getTime() / 1000 / 60; //minutes

                    if (startDate.before(nowDate) && endDate.after(nowDate)) {
                        // Event already started
                        long timeLeftMinutes = ( endDate.getTime() - nowDate.getTime() ) / 1000 / 60;
//                        Log.i(TAG, "ongoing event ends in " + timeLeftMinutes + " minutes ( " + endDateStr + ") eta " + minutesToTarget + " duration " + duration);
                        if ( (timeLeftMinutes - minutesToTarget) > 0) {
                            // If we'll make at least a quarter of the event, Color it yellow
                            int endSpan = distanceText.indexOf("min") + 3;
                            spanRange = new SpannableString(distanceText);
                            TextAppearanceSpan tas = new TextAppearanceSpan(textView.getContext(), R.style.OrangeText);
                            spanRange.setSpan(tas, 0, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    } else if (startDate.after(nowDate)) {
                        long timeUntilStartMinutes = ( startDate.getTime() - nowDate.getTime() ) / 1000 / 60;
//                        Log.i(TAG, "fugure event starts in " + timeUntilStartMinutes + " minutes ( " + startDateStr + ") eta " + minutesToTarget + " duration " + duration);
                        if ( (timeUntilStartMinutes - minutesToTarget) > 0) {
                            // If we'll make the event start, Color it green
                            int endSpan = distanceText.indexOf("min") + 3;
                            TextAppearanceSpan tas = new TextAppearanceSpan(textView.getContext(), R.style.GreenText);
                            spanRange.setSpan(tas, 0, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }

                textView.setText(spanRange);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            // If minutes < startDate || minutes < (endDate - now)
            textView.setVisibility(View.VISIBLE);
            return minutesToTarget;
        } else {
            textView.setText("");
            textView.setVisibility(View.INVISIBLE);
            return -1;
        }
    }

    public static AdapterView.OnItemLongClickListener mListItemLongClickListener = new AdapterView.OnItemLongClickListener(){

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
            int model_id = (Integer) v.getTag(R.id.list_item_related_model);
            Constants.PlayaItemType itemType = (Constants.PlayaItemType) v.getTag(R.id.list_item_related_model_type);
            Uri uri = null;
            switch (itemType) {
                case ART:
                    uri = PlayaContentProvider.Art.ART;
                    break;
                case CAMP:
                    uri = PlayaContentProvider.Camps.CAMPS;
                    break;
                case EVENT:
                    uri = PlayaContentProvider.Events.EVENTS;
                    break;
                default:
                    throw new IllegalStateException("Unknown PLAYA_ITEM");
            }
            Cursor result = v.getContext().getContentResolver().query(uri, new String[] { PlayaItemTable.favorite }, PlayaItemTable.id + " = ?", new String[] { String.valueOf(model_id) }, null);
            if (result != null && result.moveToFirst()) {
                ContentValues values = new ContentValues();
                int isFavorite = result.getInt(result.getColumnIndex(PlayaItemTable.favorite));
                values.put(PlayaItemTable.favorite, (isFavorite == 1 ? 0 : 1));

                if (isFavorite == 1) {
                    Toast.makeText(v.getContext(), "Removed from Favorites", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(v.getContext(), "Added to Favorites", Toast.LENGTH_SHORT).show();
                }

                v.getContext().getContentResolver().update(uri, values, PlayaItemTable.id + " = ?", new String[]{String.valueOf(model_id)});
            }
            return true;
        }
    };
}
