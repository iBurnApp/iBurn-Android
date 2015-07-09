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
import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslim.LinearSLM;

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
public class EventSectionedCursorAdapter extends CursorRecyclerViewAdapter<EventSectionedCursorAdapter.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0x01;

    private static final int VIEW_TYPE_CONTENT = 0x00;

    static final String[] Projection = new String[]{
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
    private boolean areItemsGrouped;
    private SimpleDateFormat dayFormatter;
    private SimpleDateFormat timeFormatter;

    List<Integer> headerPositions;

    private int titleCol;
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
        TextView typeView;
        TextView timeView;
        TextView distanceView;
        View container;

        int modelId;
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

        initializeWithNewCursor(c);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView;
        if (viewType == VIEW_TYPE_HEADER) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listview_header_item, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quad_listview_item, parent, false);
        }

        ViewHolder vh = new ViewHolder(itemView);

        itemView.setOnClickListener(view -> {
            int modelId = (int) view.getTag();
            listener.onItemSelected(modelId, Constants.PlayaItemType.EVENT);
        });

        return vh;
    }

    @Override
    public void onBindViewHolder(EventSectionedCursorAdapter.ViewHolder viewHolder, int position) {
        if (!mDataValid) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (isHeaderPosition(position)) {
            mCursor.moveToPosition(getCursorPositionForPosition(position+1)); // the next element informed this header
            onBindViewHolderHeader(viewHolder, mCursor, position);
            return;
        }
        else if (!mCursor.moveToPosition(getCursorPositionForPosition(position))) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        onBindViewHolder(viewHolder, mCursor, position);
    }

    public void onBindViewHolderHeader(ViewHolder holder, Cursor cursor, int position) {
        final LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) holder.itemView.getLayoutParams();

        params.setSlm(LinearSLM.ID);
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        int headerPos = getHeaderPositionForPosition(position);
        //Timber.d("Header Position %d header %d", position, headerPos);

        params.setFirstPosition(headerPos);

        holder.itemView.setLayoutParams(params);

        try {
            ((TextView) holder.itemView).setText(DateUtil.getStartDateString(
                    PlayaDateTypeAdapter.iso8601Format.parse(cursor.getString(startTimeCol)),
                    CurrentDateProvider.getCurrentDate()));
        } catch (ParseException e) {
            ((TextView) holder.itemView).setText("Starts " + cursor.getString(startTimePrettyCol));
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
        viewHolder.timeView.setText(viewHolder.timeLabel);

        viewHolder.modelId = cursor.getInt(idCol);
        viewHolder.container.setTag(viewHolder.modelId);

        /** Embed SuperSLIM section configuration. **/

        final LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams();

        //params.setSlm(LinearSLM.ID);

        // Position of the first item in the section. This doesn't have to
        // be a header. However, if an item is a header, it must then be the
        // first item in a section.
        params.setSlm(LinearSLM.ID);
        int headerPos = getHeaderPositionForPosition(position);
        //Timber.d("Position %d header %d", position, headerPos);
        params.setFirstPosition(headerPos);
        viewHolder.itemView.setLayoutParams(params);
    }

    @Override
    public String[] getRequiredProjection() {
        return Projection;
    }

    @Override
    public int getItemViewType(int position) {
        // TODO : Get header positions from query. count(s_date < +30m), count(s_date < +2hr)
        return isHeaderPosition(position) ? VIEW_TYPE_HEADER : VIEW_TYPE_CONTENT;
    }

    @Override
    public int getItemCount() {
        int superCount = super.getItemCount();
        if (superCount != 0)
            return superCount + (headerPositions == null ? 0 : headerPositions.size());
        return superCount;
    }

    @Override
    public long getItemId(int position) {
        if (isHeaderPosition(position)) {
            return getHeaderId(position);
        }
        return super.getItemId(getCursorPositionForPosition(position));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {
        //unused
        Timber.w("Unused method called");
    }

    public void changeCursor(Cursor cursor) {
        initializeWithNewCursor(cursor);


        super.changeCursor(cursor);
    }

    public Cursor swapCursor(Cursor newCursor) {
        initializeWithNewCursor(newCursor);

        return super.swapCursor(newCursor);
    }

    boolean isHeaderPosition(int position) {
        return headerPositions != null && headerPositions.contains(position);
    }

    private long getHeaderId(int position) {
        // return something unlikely to conflict with database ids
        return Long.MAX_VALUE - headerPositions.indexOf(position);
    }

    private void initializeWithNewCursor(Cursor newCursor) {
        if (newCursor != null && newCursor.getCount() > 0) {
            setColumnsFromCursor(newCursor);
            calculateHeadersForCursor(newCursor);
        }
    }

    private void calculateHeadersForCursor(Cursor cursor) {
        headerPositions = new ArrayList<>();
        cursor.moveToFirst();
        headerPositions.add(0);
        String lastDate = cursor.getString(startTimeCol);
        // We begin at position 2. 0 is first header, 1 is first item
        for (int position = 2; cursor.moveToNext(); position++) {
            String thisDate = cursor.getString(startTimeCol);
            if (!lastDate.equals(thisDate)) {
                headerPositions.add(position);
                position++; // We must account for the position occupied by the header
            }
            lastDate = thisDate;
        }
        cursor.moveToFirst();
    }

    private void setColumnsFromCursor(Cursor cursor) {
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

    /**
     * @return the position of the header for the corresponding item position.
     * The value will be less than or equal to position.
     */
    int getHeaderPositionForPosition(int position) {
        // TODO : Do a binary search? IF -1 return last header index?
        int headerIdx = getHeaderIndexForPosition(position);
        return headerIdx == -1 ? position : headerPositions.get(headerIdx);
    }

    /**
     * @return the index of the header for the current position, or -1 if none found
     */
    int getHeaderIndexForPosition(int position) {
        // TODO : Do a binary search?
        for (int idx = headerPositions.size() - 1; idx >= 0; idx--) {
            if (headerPositions.get(idx) <= position)
                return idx;
        }
        return -1;
    }

    /**
     * @return the cursor position for the corresponding item position. Compensate for the presence of headers
     * e.g: Position 1 is cursor position 0, because position 0 is always the first header
     */
    int getCursorPositionForPosition(int position) {
        return position - (getHeaderIndexForPosition(position) + 1);
    }
}
