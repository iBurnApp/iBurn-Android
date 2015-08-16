package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.location.LocationProvider;

import java.util.Arrays;

/**
 * A class that assists in the creation of {@link CursorRecyclerViewAdapter}s that
 * bind playa items
 */
public abstract class PlayaItemCursorAdapter<T extends PlayaItemCursorAdapter.ViewHolder> extends CursorRecyclerViewAdapter<T> {

    public static final String[] Projection = new String[]{
            PlayaItemTable.id,
            PlayaItemTable.name,
            PlayaItemTable.description,
            PlayaItemTable.favorite,
            PlayaItemTable.latitude,
            PlayaItemTable.longitude
    };

    protected Context context;
    protected AdapterListener listener;
    protected Location deviceLocation;

    protected int idCol;
    protected int titleCol;
    protected int descCol;
    protected int favoriteCol;
    protected int latCol;
    protected int lonCol;

    public PlayaItemCursorAdapter(Context context, Cursor c, AdapterListener listener) {
        super(c);
        this.context = context;
        this.listener = listener;

        LocationProvider.getLastLocation(context).
                subscribe(lastLocation -> deviceLocation = lastLocation);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        int modelId;

        TextView titleView;
        TextView descView;
        ImageView favoriteView;
        TextView walkTimeView;
        TextView bikeTimeView;

        public ViewHolder(View view) {
            super(view);

            titleView = (TextView) view.findViewById(R.id.title);
            descView = (TextView) view.findViewById(R.id.description);
            favoriteView = (ImageView) view.findViewById(R.id.heart);

            walkTimeView = (TextView) view.findViewById(R.id.walk_time);
            bikeTimeView = (TextView) view.findViewById(R.id.bike_time);
        }
    }

    @Override
    public void onBindViewHolder(T viewHolder, Cursor cursor) {

        if (titleCol == 0) {
            titleCol = cursor.getColumnIndexOrThrow(PlayaItemTable.name);
            latCol = cursor.getColumnIndexOrThrow(PlayaItemTable.latitude);
            lonCol = cursor.getColumnIndexOrThrow(PlayaItemTable.longitude);
            idCol = cursor.getColumnIndexOrThrow(PlayaItemTable.id);
            descCol = cursor.getColumnIndexOrThrow(PlayaItemTable.description);
            favoriteCol = cursor.getColumnIndexOrThrow(PlayaItemTable.favorite);
        }

        viewHolder.titleView.setText(cursor.getString(titleCol));
        viewHolder.descView.setText(cursor.getString(descCol));

        AdapterUtils.setDistanceText(deviceLocation, viewHolder.walkTimeView, viewHolder.bikeTimeView,
                cursor.getDouble(latCol), cursor.getDouble(lonCol));

        if (cursor.getInt(favoriteCol) == 1) {
            viewHolder.favoriteView.setImageResource(R.drawable.ic_heart_full);
        } else {
            viewHolder.favoriteView.setImageResource(R.drawable.ic_heart_empty);
        }

        viewHolder.modelId = cursor.getInt(idCol);
        viewHolder.itemView.setTag(viewHolder.modelId);
    }

    public String[] buildRequiredProjection(String[] complementaryProjection) {
        String[] totalProjection = Arrays.copyOf(Projection, Projection.length + complementaryProjection.length);
        System.arraycopy(complementaryProjection, 0, totalProjection, Projection.length, complementaryProjection.length);
        return totalProjection;
    }

    /**
     * Convenience method to setup item click and favorite button click.
     * Splendidly suitable for calling from {@link #onCreateViewHolder(ViewGroup, int)}
     */
    protected void setupClickListeners(T viewHolder, Constants.PlayaItemType type) {
        viewHolder.itemView.setOnClickListener(view -> {
            if (view.getTag() != null) {
                int modelId = (int) view.getTag();
                listener.onItemSelected(modelId, type);
            }
        });

        viewHolder.favoriteView.setOnClickListener(v -> {
            int modelId = (int) ((View) v.getParent()).getTag();
            listener.onItemFavoriteButtonSelected(modelId, type);
        });
    }
}