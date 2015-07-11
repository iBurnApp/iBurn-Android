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
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.location.LocationProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Bind a playa item (camp, art, event) database row to a view with a simple name & distance display,
 * using the device's location and date when the adapter was constructed.
 *
 */
public class PlayaSearchResponseCursorAdapter extends SectionedCursorAdapter<PlayaSearchResponseCursorAdapter.ViewHolder> {

    private Context context;
    private Location deviceLocation;
    private AdapterItemSelectedListener listener;

    private static int titleCol;
    private static int latCol;
    private static int lonCol;
    private static int idCol;
    private static int typeCol;

    public PlayaSearchResponseCursorAdapter(Context context, Cursor cursor, AdapterItemSelectedListener listener) {
        super(cursor);
        this.listener = listener;
        this.context = context;

        // TODO : NotifydatasetChanged when location does?
        LocationProvider.getLastLocation(context).
                subscribe(lastLocation -> deviceLocation = lastLocation);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        TextView distanceView;
        View container;

        public ViewHolder(View view) {
            super(view);

            container = view;
            titleView = (TextView) view.findViewById(R.id.list_item_title);
            distanceView = (TextView) view.findViewById(R.id.list_item_sub_left);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        ViewHolder holder = null;

        if (viewType == VIEW_TYPE_HEADER) {

            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listview_header_item, parent, false);

            holder = new ViewHolder(itemView);

        } else if (viewType == VIEW_TYPE_CONTENT) {

            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.search_result_listview_item, parent, false);

            holder = new ViewHolder(itemView);

            itemView.setOnClickListener(view -> {
                int modelId = (int) view.getTag(R.id.list_item_related_model);
                int modelType = (int) view.getTag(R.id.list_item_related_model_type);
                listener.onItemSelected(modelId, DataProvider.getTypeValue(modelType));
            });
        }

        return holder;
    }

    @Override
    protected List<Integer> createHeadersForCursor(Cursor cursor) {
        cacheColumns(cursor);
        ArrayList<Integer> headers = new ArrayList<>();

        int lastType = cursor.getInt(typeCol);
        headers.add(0);
        // We begin at position 2. 0 is first header, 1 is first item
        for (int idx = 2; cursor.moveToNext(); idx++) {
            int thisType = cursor.getInt(typeCol);
            if (thisType != lastType) {
                headers.add(idx);
                idx++; // We must account for the position occupied by the header
                lastType = thisType;
            }
        }
        return headers;
    }

    @Override
    protected void onBindViewHolder(ViewHolder viewHolder, Cursor cursor, int position) {
        setLinearSlmParameters(viewHolder, position);

        viewHolder.titleView.setText(cursor.getString(titleCol));

        AdapterUtils.setDistanceText(deviceLocation, viewHolder.distanceView,
                cursor.getDouble(latCol), cursor.getDouble(lonCol));

        viewHolder.container.setTag(R.id.list_item_related_model, cursor.getInt(idCol));
        viewHolder.container.setTag(R.id.list_item_related_model_type, cursor.getInt(typeCol));
    }

    @Override
    protected void onBindViewHolderHeader(ViewHolder holder, Cursor firstSectionItem, int position) {

        setLinearSlmParameters(holder, position);

        Constants.PlayaItemType type = DataProvider.getTypeValue(firstSectionItem.getInt(typeCol));
        String headerTitle = null;
        switch(type) {
            case CAMP:
                headerTitle = context.getString(R.string.camps_tab);
                break;

            case ART:
                headerTitle = context.getString(R.string.art_tab);
                break;

            case EVENT:
                headerTitle = context.getString(R.string.events_tab);
                break;
        }
        ((TextView) holder.itemView).setText(headerTitle);
    }

    @Override
    public String[] getRequiredProjection() {
        return PlayaItemCursorAdapter.Projection;
    }

    private void cacheColumns(Cursor cursor) {
        titleCol = cursor.getColumnIndexOrThrow(PlayaItemTable.name);
        latCol = cursor.getColumnIndexOrThrow(PlayaItemTable.latitude);
        lonCol = cursor.getColumnIndexOrThrow(PlayaItemTable.longitude);
        idCol = cursor.getColumnIndexOrThrow(PlayaItemTable.id);
        typeCol = cursor.getColumnIndexOrThrow(DataProvider.VirtualType); // This is a virtual column created during UNION queries of multiple tables
    }
}