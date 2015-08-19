package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
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

import java.text.DateFormat;
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
public class EventCursorAdapter extends PlayaItemCursorAdapter<EventCursorAdapter.ViewHolder> {

    /** Parsers for start time section header derivation */
    private static final DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final DateFormat humanFormat = new SimpleDateFormat("E ha", Locale.US);

    private List<String> startTimeSections;
    private List<Integer> startTimeSectionPositions;

    static final String[] Projection = new String[]{
            EventTable.startTime,
            EventTable.startTimePrint,
            EventTable.endTime,
            EventTable.endTimePrint,
            EventTable.allDay,
            EventTable.eventType,
            EventTable.playaAddress
    };

    /** Projection when Events are grouped by name */
    static final String[] GroupedProjection = new String[]{
            EventTable.startTime,
            EventTable.startTimePrint,
            EventTable.endTime,
            EventTable.endTimePrint,
            EventTable.allDay,
            EventTable.eventType,
            EventTable.playaAddress,
            "count(" + EventTable.name + ")",
            "min(" + EventTable.startTime + ")",
            "max(" + EventTable.startTime + ")"

    };

    private String[] finalProjection;
    private boolean areItemsGrouped;
    private SimpleDateFormat dayFormatter;
    private SimpleDateFormat timeFormatter;

    private int eventTypeCol;
    private int addressCol;
    private int startTimeCol;
    private int startTimePrettyCol;
    private int endTimeCol;
    private int endTimePrettyCol;
    private int allDayCol;

    public static class ViewHolder extends PlayaItemCursorAdapter.ViewHolder {
        TextView typeView;
        TextView timeView;
        TextView addressView;

        String timeLabel;

        public ViewHolder(View view) {
            super(view);

            timeView = (TextView) view.findViewById(R.id.time);
            typeView = (TextView) view.findViewById(R.id.type);
            addressView = (TextView) view.findViewById(R.id.address);
        }
    }

    Calendar nowPlusOneHrDate = Calendar.getInstance();
    Calendar nowDate = Calendar.getInstance();

    public EventCursorAdapter(Context context, Cursor c, boolean areItemsGrouped, AdapterListener listener) {
        super(context, c, listener);
        this.areItemsGrouped = areItemsGrouped;
        Date now = CurrentDateProvider.getCurrentDate();
        nowDate.setTime(now);
        nowPlusOneHrDate.setTime(now);
        nowPlusOneHrDate.add(Calendar.HOUR, 1);

        if (areItemsGrouped) {
            dayFormatter = new SimpleDateFormat("EE M/d", Locale.US);
            timeFormatter = new SimpleDateFormat("h:mm a", Locale.US);
        }

        finalProjection = this.areItemsGrouped ? buildRequiredProjection(GroupedProjection) : buildRequiredProjection(Projection);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.event_list_view_item, parent, false);
        ViewHolder vh = new ViewHolder(itemView);

        setupClickListeners(vh, Constants.PlayaItemType.EVENT);

        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {

        super.onBindViewHolder(viewHolder, cursor);

        if (eventTypeCol == 0) {
            eventTypeCol = cursor.getColumnIndexOrThrow(EventTable.eventType);
            addressCol = cursor.getColumnIndexOrThrow(EventTable.playaAddress);
            startTimeCol = cursor.getColumnIndexOrThrow(EventTable.startTime);
            startTimePrettyCol = cursor.getColumnIndexOrThrow(EventTable.startTimePrint);
            endTimeCol = cursor.getColumnIndexOrThrow(EventTable.endTime);
            endTimePrettyCol = cursor.getColumnIndexOrThrow(EventTable.endTimePrint);
            allDayCol = cursor.getColumnIndexOrThrow(EventTable.allDay);
        }

        try {
            if (areItemsGrouped) {

                int numEvents = cursor.getInt(finalProjection.length - 3);
                String minDate = cursor.getString(finalProjection.length - 2);
                String maxDate = cursor.getString(finalProjection.length - 1);

                Date firstStartDate = PlayaDateTypeAdapter.iso8601Format.parse(minDate);
                Date lastStartDate = PlayaDateTypeAdapter.iso8601Format.parse(maxDate);

                if (numEvents > 1)
                    viewHolder.timeLabel = /*String.valueOf(numEvents) + " times " + */ dayFormatter.format(firstStartDate) + " - " + dayFormatter.format(lastStartDate) + " at " + timeFormatter.format(lastStartDate);
                else
                    viewHolder.timeLabel = cursor.getString(startTimePrettyCol);
            } else {
                if (cursor.getInt(cursor.getColumnIndexOrThrow(EventTable.allDay)) == 1) {
                    viewHolder.timeLabel = "All " + cursor.getString(cursor.getColumnIndexOrThrow(EventTable.startTimePrint));

                } else {
                    viewHolder.timeLabel = DateUtil.getDateString(context, nowDate.getTime(), nowPlusOneHrDate.getTime(),
                            cursor.getString(startTimeCol),
                            cursor.getString(startTimePrettyCol),
                            cursor.getString(endTimeCol),
                            cursor.getString(endTimePrettyCol));
                }
            }
        } catch (IllegalArgumentException | ParseException e) {
            Timber.e(e, "Failed to bind event");
        }
        viewHolder.typeView.setText(
                AdapterUtils.getStringForEventType(cursor.getString(eventTypeCol)));

        if (areItemsGrouped) {
            viewHolder.walkTimeView.setVisibility(View.GONE);
            viewHolder.bikeTimeView.setVisibility(View.GONE);
        }
        // if items not grouped, time set by super.onBindViewHolder

        viewHolder.timeView.setText(viewHolder.timeLabel);

        String playaAddress = cursor.getString(addressCol);
        if (!TextUtils.isEmpty(playaAddress)) {
            viewHolder.addressView.setText(playaAddress);
            viewHolder.addressView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.addressView.setVisibility(View.GONE);
        }
    }

    @Override
    public String[] getRequiredProjection() {
        return finalProjection;
    }

    // <editor-fold desc="SectionIndexer">

    @Override
    public Object[] getSections() {
        if (startTimeSections == null) {
            mCursor.moveToFirst();
            startTimeSections = new ArrayList<>();
            startTimeSectionPositions = new ArrayList<>();

            addSectionStringForCursor(mCursor);
            startTimeSectionPositions.add(0);

            while (mCursor.moveToNext()) {
                if (!startTimeSections.get(startTimeSections.size()-1).equals(mCursor.getString(startTimeCol))) {
                    addSectionStringForCursor(mCursor);
                    startTimeSectionPositions.add(startTimeSections.size()-1);
                }
            }
        }
        return startTimeSections.toArray();
    }

    private void addSectionStringForCursor(Cursor cursor) {
        if (mCursor.getInt(allDayCol) == 1) {
            startTimeSections.add("All " + cursor.getString(startTimePrettyCol));
        } else {
            try {
                startTimeSections.add(humanFormat.format(iso8601Format.parse(cursor.getString(startTimeCol))));
            } catch (ParseException e) {
                startTimeSections.add(cursor.getString(startTimePrettyCol));
            }
        }
    }

    private int getSectionForCursorPosition(int position) {
        for (int idx = 0; idx < startTimeSectionPositions.size(); idx++) {
            if (startTimeSectionPositions.get(idx) > position) return idx - 1;
        }
        return startTimeSectionPositions.size() - 1;
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        // not needed
        return 0;
    }

    @Override
    public int getSectionForPosition(int position) {
        if (position == mCursor.getCount()) return startTimeSections.size() - 1;
        if (startTimeSections == null) return 0;

        return getSectionForCursorPosition(position);
    }

    // </editor-fold desc="SectionIndexer">
}
