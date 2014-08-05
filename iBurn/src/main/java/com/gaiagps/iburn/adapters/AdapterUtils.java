package com.gaiagps.iburn.adapters;

import android.database.Cursor;
import android.location.Location;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.widget.TextView;

import com.gaiagps.iburn.GeoUtils;
import com.gaiagps.iburn.PlayaClient;
import com.gaiagps.iburn.R;

/**
 * Created by davidbrodsky on 8/4/14.
 */
public class AdapterUtils {

    public static void setDistanceText(Location deviceLocation, TextView textView, double lat, double lon) {
        if (deviceLocation != null && lat != 0) {
            int meters = ((Double)(GeoUtils.getDistance(lat, lon, deviceLocation))).intValue();
            int minutes = PlayaClient.getWalkingEstimateHour(meters);
            String distance = String.format("%d m (%.2f mi)", minutes, meters * 0.000621371);
            int endSpan = distance.length();
            Spannable spanRange = new SpannableString(distance);
            TextAppearanceSpan tas = new TextAppearanceSpan(textView.getContext(), R.style.Subdued);
            spanRange.setSpan(tas, distance.indexOf('('), endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(spanRange);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setText("");
            textView.setVisibility(View.GONE);
        }

    }
}
