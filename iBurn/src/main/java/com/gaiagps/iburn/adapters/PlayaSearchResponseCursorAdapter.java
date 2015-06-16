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
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.location.DeviceLocation;

/**
 * Bind a playa item (camp, art, event) database row to a view with a simple name & distance display,
 * using the device's location and date when the adapter was constructed.
 *
 * TODO: Update device location periodically
 */
public class PlayaSearchResponseCursorAdapter extends CursorRecyclerViewAdapter<PlayaSearchResponseCursorAdapter.ViewHolder> {

    private Location deviceLocation;
    private AdapterItemSelectedListener listener;

    private static int titleCol;
    private static int latCol;
    private static int lonCol;
    private static int idCol;

    public PlayaSearchResponseCursorAdapter(Context context, Cursor cursor, AdapterItemSelectedListener listener) {
        super(context, cursor);
        this.listener = listener;

        DeviceLocation.getLastKnownLocation(context, false, new DeviceLocation.LocationResult() {
            @Override
            public void gotLocation(Location location) {
                deviceLocation = location;
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        int modelId;
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
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_result_listview_item, parent, false);
        ViewHolder vh = new ViewHolder(itemView);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int modelId = (int) v.getTag();
                listener.onItemSelected(modelId, Constants.PlayaItemType.CAMP);
            }
        });

        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {

        if (titleCol == 0) {
            titleCol = cursor.getColumnIndexOrThrow(PlayaItemTable.name);
            latCol = cursor.getColumnIndexOrThrow(PlayaItemTable.latitude);
            lonCol = cursor.getColumnIndexOrThrow(PlayaItemTable.longitude);
            idCol = cursor.getColumnIndexOrThrow(PlayaItemTable.id);
        }

        viewHolder.titleView.setText(cursor.getString(titleCol));

        AdapterUtils.setDistanceText(deviceLocation, viewHolder.distanceView,
                cursor.getDouble(latCol), cursor.getDouble(lonCol));

        viewHolder.modelId = cursor.getInt(idCol);
        viewHolder.container.setTag(viewHolder.modelId);
    }
}