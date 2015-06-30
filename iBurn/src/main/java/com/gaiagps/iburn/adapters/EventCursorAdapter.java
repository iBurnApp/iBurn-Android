package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.PlayaUtils;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.location.DeviceLocation;

import java.util.Calendar;
import java.util.Date;

import timber.log.Timber;

/**
 * Bind an event database row to a view with name, distance, and delta time display,
 * using the device's location and current time when the adapter was constructed.
 * <p/>
 * TODO: Update device location and delta time periodically
 */
public class EventCursorAdapter extends CursorRecyclerViewAdapter<EventCursorAdapter.ViewHolder> {

    static final String[] Projection = new String[] {
            EventTable.id,
            EventTable.name,
            EventTable.startTime,
            EventTable.startTimePrint,
            EventTable.endTime,
            EventTable.endTimePrint,
            EventTable.allDay,
            EventTable.favorite,
            EventTable.latitude,
            EventTable.longitude,
            EventTable.eventType
    };

    private Context context;
    private AdapterItemSelectedListener listener;

    private static int titleCol;
    private static int eventTypeCol;
    private static int startTimeCol;
    private static int startTimePrettyCol;
    private static int endTimeCol;
    private static int endTimePrettyCol;
    private static int idCol;
    private static int latCol;
    private static int lonCol;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        TextView typeView;
        TextView timeView;
        TextView distanceView;
        View container;

        int modelId;
        boolean allDay;
        String timeLabel;

        public ViewHolder(View view) {
            super(view);

            container = view;
            titleView = (TextView) view.findViewById(R.id.list_item_title);
            timeView = (TextView) view.findViewById(R.id.list_item_sub_right);
            distanceView = (TextView) view.findViewById(R.id.list_item_sub_left);
            typeView = (TextView) view.findViewById(R.id.list_item_subtitle);
        }
    }

    private Location mDeviceLocation;
    Calendar nowPlusOneHrDate = Calendar.getInstance();
    Calendar nowDate = Calendar.getInstance();

    public EventCursorAdapter(Context context, Cursor c, AdapterItemSelectedListener listener) {
        super(context, c);
        this.context = context;
        this.listener = listener;
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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.quad_listview_item, parent, false);
        ViewHolder vh = new ViewHolder(itemView);

        itemView.setOnClickListener(view -> {
            int modelId = (int) view.getTag();
            listener.onItemSelected(modelId, Constants.PlayaItemType.EVENT);
        });

        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {

        if (titleCol == 0) {
            titleCol = cursor.getColumnIndexOrThrow(EventTable.name);
            eventTypeCol = cursor.getColumnIndexOrThrow(EventTable.eventType);
            startTimeCol = cursor.getColumnIndexOrThrow(EventTable.startTime);
            startTimePrettyCol = cursor.getColumnIndexOrThrow(EventTable.startTimePrint);
            endTimeCol = cursor.getColumnIndexOrThrow(EventTable.endTime);
            endTimePrettyCol = cursor.getColumnIndexOrThrow(EventTable.endTimePrint);
            latCol = cursor.getColumnIndexOrThrow(PlayaItemTable.latitude);
            lonCol = cursor.getColumnIndexOrThrow(PlayaItemTable.longitude);
            idCol = cursor.getColumnIndexOrThrow(EventTable.id);
        }

        try {
            if (cursor.getInt(cursor.getColumnIndexOrThrow(EventTable.allDay)) == 1) {
                viewHolder.allDay = true;
                viewHolder.timeLabel = "All " + cursor.getString(cursor.getColumnIndexOrThrow(EventTable.startTimePrint));

            } else {
                viewHolder.allDay = false;
                viewHolder.timeLabel = PlayaUtils.getDateString(context, nowDate.getTime(), nowPlusOneHrDate.getTime(),
                        cursor.getString(startTimeCol),
                        cursor.getString(startTimePrettyCol),
                        cursor.getString(endTimeCol),
                        cursor.getString(endTimePrettyCol));
            }

        } catch (IllegalArgumentException e) {
            Timber.d("ok");
        }
        viewHolder.typeView.setText(
                AdapterUtils.getStringForEventType(cursor.getString(eventTypeCol)));

        AdapterUtils.setDistanceText(mDeviceLocation,
                nowDate.getTime(),
                cursor.getString(startTimeCol),
                cursor.getString(endTimeCol),
                viewHolder.distanceView,
                cursor.getDouble(latCol),
                cursor.getDouble(lonCol));

        viewHolder.titleView.setText(cursor.getString(titleCol));
        viewHolder.timeView.setText(viewHolder.timeLabel);

        viewHolder.modelId = cursor.getInt(idCol);
        viewHolder.container.setTag(viewHolder.modelId);
    }

    @Override
    public String[] getRequiredProjection() {
        return Projection;
    }
}
