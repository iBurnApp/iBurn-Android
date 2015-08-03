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
import com.gaiagps.iburn.CurrentDateProvider;
import com.gaiagps.iburn.DateUtil;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.location.LocationProvider;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * Bind an event database row to a view with name, distance, and delta time display,
 * using the device's location and current time when the adapter was constructed.
 * <p>
 * TODO: Update device location and delta time periodically
 */
public class EventSectionedCursorAdapter extends SectionedCursorAdapter<EventSectionedCursorAdapter.ViewHolder> {

    static final String[] Projection = new String[]{
            EventTable.id,
            EventTable.name,
            EventTable.description,
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
    private boolean areItemsGrouped;
    private SimpleDateFormat dayFormatter;
    private SimpleDateFormat timeFormatter;

    List<Integer> headerPositions;

    private int titleCol;
    private int descCol;
    private int eventTypeCol;
    private int startTimeCol;
    private int startTimePrettyCol;
    private int endTimeCol;
    private int endTimePrettyCol;
    private int idCol;
    private int latCol;
    private int lonCol;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        TextView descView;
        TextView typeView;
        TextView distanceView;
        View container;

        int modelId;
        String timeLabel;

        public ViewHolder(View view) {
            super(view);

            container = view;
            titleView = (TextView) view.findViewById(R.id.list_item_title);
            descView = (TextView) view.findViewById(R.id.list_item_description);
            distanceView = (TextView) view.findViewById(R.id.list_item_sub_left);
            typeView = (TextView) view.findViewById(R.id.list_item_subtitle);
        }
    }

    private Location mDeviceLocation;
    Calendar nowPlusOneHrDate = Calendar.getInstance();
    Calendar nowDate = Calendar.getInstance();

    public EventSectionedCursorAdapter(Context context, Cursor c, boolean isGrouped, AdapterItemSelectedListener listener) {
        super(c);
        this.context = context;
        this.listener = listener;
        this.areItemsGrouped = isGrouped;
        Date now = CurrentDateProvider.getCurrentDate();
        nowDate.setTime(now);
        nowPlusOneHrDate.setTime(now);
        nowPlusOneHrDate.add(Calendar.HOUR, 1);

        LocationProvider.getLastLocation(context).
                subscribe(lastLocation -> mDeviceLocation = lastLocation);

        if (isGrouped) {
            dayFormatter = new SimpleDateFormat("EE M/d", Locale.US);
            timeFormatter = new SimpleDateFormat("h:mm a", Locale.US);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView;
        if (viewType == VIEW_TYPE_HEADER) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listview_header_item, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.event_list_view_item, parent, false);
        }

        ViewHolder vh = new ViewHolder(itemView);

        itemView.setOnClickListener(view -> {
            if (view.getTag() != null) {
                int modelId = (int) view.getTag();
                listener.onItemSelected(modelId, Constants.PlayaItemType.EVENT);
            }
        });

        return vh;
    }

    public void onBindViewHolderHeader(ViewHolder holder, Cursor cursor, int position) {

        setLinearSlmParameters(holder, position);

        try {
            ((TextView) holder.itemView).setText(DateUtil.getStartDateString(
                    PlayaDateTypeAdapter.iso8601Format.parse(cursor.getString(startTimeCol)),
                    CurrentDateProvider.getCurrentDate()).toUpperCase());
        } catch (ParseException e) {
            ((TextView) holder.itemView).setText("STARTS " + cursor.getString(startTimePrettyCol).toUpperCase());
            Timber.e(e, "Failed to parse event start date");
        }
    }

    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor, int position) {

        try {
            if (areItemsGrouped) {

                int numEvents = cursor.getInt(11);
                String minDate = cursor.getString(12);
                String maxDate = cursor.getString(13);

                Date firstStartDate = PlayaDateTypeAdapter.iso8601Format.parse(minDate);
                Date lastStartDate = PlayaDateTypeAdapter.iso8601Format.parse(maxDate);

                if (numEvents > 1)
                    viewHolder.timeLabel = /*String.valueOf(numEvents) + " times " + */ dayFormatter.format(firstStartDate) + " - " + dayFormatter.format(lastStartDate) + " at " + timeFormatter.format(lastStartDate);
                else
                    viewHolder.timeLabel = cursor.getString(startTimePrettyCol);
            } else {
//                if (cursor.getInt(cursor.getColumnIndexOrThrow(EventTable.allDay)) == 1) {
//                    viewHolder.timeLabel = "All " + cursor.getString(cursor.getColumnIndexOrThrow(EventTable.startTimePrint));
//
//                } else {
//                    viewHolder.timeLabel = DateUtil.getEndDateString(PlayaDateTypeAdapter.iso8601Format.parse(cursor.getString(endTimeCol)), CurrentDateProvider.getCurrentDate());
//                }
            }
        } catch (IllegalArgumentException | ParseException e) {
            Timber.e(e, "Failed to bind event");
        }
        viewHolder.typeView.setText(
                AdapterUtils.getStringForEventType(cursor.getString(eventTypeCol)));

        if (!areItemsGrouped) {
            AdapterUtils.setDistanceText(mDeviceLocation,
                    nowDate.getTime(),
                    cursor.getString(startTimeCol),
                    cursor.getString(endTimeCol),
                    viewHolder.distanceView,
                    cursor.getDouble(latCol),
                    cursor.getDouble(lonCol));
        } else {
            viewHolder.distanceView.setVisibility(View.GONE);
        }

        viewHolder.titleView.setText(cursor.getString(titleCol));
        viewHolder.descView.setText(cursor.getString(descCol));

        viewHolder.modelId = cursor.getInt(idCol);
        viewHolder.container.setTag(viewHolder.modelId);

        setLinearSlmParameters(viewHolder, position);
    }

    @Override
    public String[] getRequiredProjection() {
        return Projection;
    }

    @Override
    protected List<Integer> createHeadersForCursor(Cursor cursor) {
        cacheColumns(cursor);
        List<Integer> headerPositions = new ArrayList<>();
        headerPositions.add(0);
        String lastDate = cursor.getString(startTimeCol);
        // We begin at position 2. 0 is first header, 1 is first item
        for (int position = 2; cursor.moveToNext(); position++) {
            String thisDate = cursor.getString(startTimeCol);
            if (!lastDate.equals(thisDate)) {
                headerPositions.add(position);
                position++; // We must account for the position occupied by the header
                lastDate = thisDate;
            }
        }
        return headerPositions;
    }

    private void cacheColumns(Cursor cursor) {
        titleCol = cursor.getColumnIndexOrThrow(EventTable.name);
        descCol = cursor.getColumnIndexOrThrow(EventTable.description);
        eventTypeCol = cursor.getColumnIndexOrThrow(EventTable.eventType);
        startTimeCol = cursor.getColumnIndexOrThrow(EventTable.startTime);
        startTimePrettyCol = cursor.getColumnIndexOrThrow(EventTable.startTimePrint);
        endTimeCol = cursor.getColumnIndexOrThrow(EventTable.endTime);
        endTimePrettyCol = cursor.getColumnIndexOrThrow(EventTable.endTimePrint);
        latCol = cursor.getColumnIndexOrThrow(PlayaItemTable.latitude);
        lonCol = cursor.getColumnIndexOrThrow(PlayaItemTable.longitude);
        idCol = cursor.getColumnIndexOrThrow(EventTable.id);
    }
}
