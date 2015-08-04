package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
            EventTable.startTime,
            EventTable.startTimePrint,
            EventTable.endTime,
            EventTable.endTimePrint,
            EventTable.allDay,
            EventTable.eventType,
            EventTable.playaAddress
    };

    private SimpleDateFormat dayFormatter;
    private SimpleDateFormat timeFormatter;

    List<Integer> headerPositions;

    private int eventTypeCol;
    private int addressCol;
    private int startTimeCol;
    private int startTimePrettyCol;
    private int endTimeCol;
    private int endTimePrettyCol;

    public static class ViewHolder extends PlayaItemCursorAdapter.ViewHolder {
        TextView typeView;
        TextView timeView;
        TextView addressView;

        String timeLabel;

        public ViewHolder(View view) {
            super(view);

            typeView = (TextView) view.findViewById(R.id.type);
            timeView = (TextView) view.findViewById(R.id.time);
            addressView = (TextView) view.findViewById(R.id.address);
        }
    }

    Calendar nowPlusOneHrDate = Calendar.getInstance();
    Calendar nowDate = Calendar.getInstance();

    public EventSectionedCursorAdapter(Context context, Cursor c, AdapterListener listener) {
        super(context, c, listener);

        Date now = CurrentDateProvider.getCurrentDate();
        nowDate.setTime(now);
        nowPlusOneHrDate.setTime(now);
        nowPlusOneHrDate.add(Calendar.HOUR, 1);

        dayFormatter = new SimpleDateFormat("EE M/d", Locale.US);
        timeFormatter = new SimpleDateFormat("h:mm a", Locale.US);
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

        if (viewType == VIEW_TYPE_CONTENT) setupClickListeners(vh, Constants.PlayaItemType.EVENT);

        return vh;
    }

    public void onBindViewHolderHeader(ViewHolder holder, Cursor cursor, int position) {

        setLinearSlmParameters(holder, position);

        try {
            if (startTimeCol == 0) {
                startTimeCol = cursor.getColumnIndexOrThrow(EventTable.startTime);
                startTimePrettyCol = cursor.getColumnIndexOrThrow(EventTable.startTimePrint);
            }
            ((TextView) holder.itemView).setText(DateUtil.getStartDateString(
                    PlayaDateTypeAdapter.iso8601Format.parse(cursor.getString(startTimeCol)),
                    CurrentDateProvider.getCurrentDate()).toUpperCase());
        } catch (ParseException e) {
            ((TextView) holder.itemView).setText("STARTS " + cursor.getString(startTimePrettyCol).toUpperCase());
            Timber.e(e, "Failed to parse event start date");
        }
    }

    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor, int position) {

        super.onBindViewHolder(viewHolder, cursor);

        if (eventTypeCol == 0) {
            eventTypeCol = cursor.getColumnIndexOrThrow(EventTable.eventType);
            addressCol = cursor.getColumnIndexOrThrow(EventTable.playaAddress);
            startTimeCol = cursor.getColumnIndexOrThrow(EventTable.startTime);
            startTimePrettyCol = cursor.getColumnIndexOrThrow(EventTable.startTimePrint);
            endTimeCol = cursor.getColumnIndexOrThrow(EventTable.endTime);
            endTimePrettyCol = cursor.getColumnIndexOrThrow(EventTable.endTimePrint);
        }

        viewHolder.typeView.setText(
                AdapterUtils.getStringForEventType(cursor.getString(eventTypeCol)));

        viewHolder.timeView.setVisibility(View.INVISIBLE); // currently unused. Keep invisible so addressView doesn't lose anchor

        // These views are sectioned by start time but
        // Perhaps we will eventually want to display end time.

        String playaAddress = cursor.getString(addressCol);
        if (!TextUtils.isEmpty(playaAddress)) {
            viewHolder.addressView.setText(playaAddress);
            viewHolder.addressView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.addressView.setVisibility(View.GONE);
        }

        setLinearSlmParameters(viewHolder, position);
    }

    @Override
    public String[] getRequiredProjection() {
        return buildRequiredProjection(Projection);
    }

    @Override
    protected List<Integer> createHeadersForCursor(Cursor cursor) {
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
}
