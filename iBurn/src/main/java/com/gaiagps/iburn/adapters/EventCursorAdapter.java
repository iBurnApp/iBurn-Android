package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.widget.TextView;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.GeoUtils;
import com.gaiagps.iburn.PlayaClient;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.location.DeviceLocation;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

public class EventCursorAdapter extends SimpleCursorAdapter {

    private Location mDeviceLocation;
    Calendar nowPlusOneHrDate = Calendar.getInstance();
    Calendar nowDate = Calendar.getInstance();

    public EventCursorAdapter(Context context, Cursor c) {
        super(context, R.layout.triple_listview_item, c, new String[]{} , new int[]{}, 0);
        Date now = new Date();
        nowDate.setTime(now);
        nowPlusOneHrDate.setTime(now);
        nowPlusOneHrDate.add(Calendar.HOUR, 1);

        DeviceLocation.getLastKnownLocation(context, false, new DeviceLocation.LocationResult() {
            @Override
            public void gotLocation(Location location) {
                mDeviceLocation = location;
            }
        });
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        ViewCache view_cache = (ViewCache) view.getTag(R.id.list_item_cache);
        if (view_cache == null) {
        	view_cache = new ViewCache();
        	view_cache.title = (TextView) view.findViewById(R.id.list_item_title);
        	view_cache.subRight = (TextView) view.findViewById(R.id.list_item_sub_right);
            view_cache.subLeft = (TextView) view.findViewById(R.id.list_item_sub_left);
        	//view_cache.thumbnail = (ImageView) view.findViewById(R.id.list_item_image);
            
        	view_cache.title_col = cursor.getColumnIndexOrThrow(EventTable.name);
        	view_cache.sub_col = cursor.getColumnIndexOrThrow(EventTable.startTime);
            view_cache.lat_col = cursor.getColumnIndexOrThrow(PlayaItemTable.latitude);
            view_cache.lon_col = cursor.getColumnIndexOrThrow(PlayaItemTable.longitude);
        	view_cache._id_col = cursor.getColumnIndexOrThrow(EventTable.id);
        	if(cursor.getInt(cursor.getColumnIndexOrThrow(EventTable.allDay)) == 1 ){
        		view_cache.all_day = true;
        		view_cache.time_label = "All " + cursor.getString(cursor.getColumnIndexOrThrow(EventTable.startTimePrint));
        	}
        	else {
        		view_cache.all_day = false;
                view_cache.time_label = getDateString(
                        cursor.getString(cursor.getColumnIndexOrThrow(EventTable.startTime)),
                        cursor.getString(cursor.getColumnIndexOrThrow(EventTable.startTimePrint)),
                        cursor.getString(cursor.getColumnIndexOrThrow(EventTable.endTime)),
                        cursor.getString(cursor.getColumnIndexOrThrow(EventTable.endTimePrint)));
        	}
        	view_cache._id_col = cursor.getColumnIndexOrThrow(EventTable.id);
        }
        view_cache.title.setText(cursor.getString(view_cache.title_col));
        view_cache.subRight.setText(view_cache.time_label);

        AdapterUtils.setDistanceText(mDeviceLocation, view_cache.subLeft,
                cursor.getDouble(view_cache.lat_col), cursor.getDouble(view_cache.lon_col));

        view.setTag(R.id.list_item_related_model, cursor.getInt(view_cache._id_col));
        view.setTag(R.id.list_item_related_model_type, Constants.PLAYA_ITEM.EVENT);
    }
	
	// Cache the views within a ListView row item 
    static class ViewCache {
        TextView title;
        TextView subRight;
        TextView subLeft;
        
        boolean all_day;
        String time_label;
        
        int title_col; 
        int sub_col;
        int _id_col;
        int lat_col;
        int lon_col;
    }

    /**
     * Get a smart date string for an event
     * e.g: Starts in 23 minutes
     *      Ends in 3 hours
     *      Ended 1 hour ago TODO
     */
    private String getDateString(String startDateStr, String prettyStartDateStr, String endDateStr, String prettyEndDateStr) {
        try {
            Date startDate = PlayaClient.parseISODate(startDateStr);
            if (nowDate.before(startDate)) {
                // Has not yet started
                if (nowPlusOneHrDate.getTime().after(startDate)) {
                    return mContext.getString(R.string.starts) + DateUtils.getRelativeTimeSpanString(startDate.getTime()).toString();
                }
                return mContext.getString(R.string.starts) + prettyStartDateStr;
            } else {
                // Already started
                Date endDate = PlayaClient.parseISODate(endDateStr);
                if (endDate.before(nowDate.getTime())) {
                    if (nowPlusOneHrDate.getTime().after(endDate)) {
                        return mContext.getString(R.string.ended) + DateUtils.getRelativeTimeSpanString(endDate.getTime()).toString();
                    }
                    return mContext.getString(R.string.ended) + prettyEndDateStr;
                } else {
                    if (nowPlusOneHrDate.getTime().after(endDate)) {
                        return mContext.getString(R.string.ends) + DateUtils.getRelativeTimeSpanString(endDate.getTime()).toString();
                    }
                    return mContext.getString(R.string.ends) + prettyEndDateStr;
                }

            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return prettyStartDateStr;
    }
}
