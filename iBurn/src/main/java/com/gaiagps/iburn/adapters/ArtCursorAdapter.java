package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.ArtTable;
import com.gaiagps.iburn.location.LocationProvider;

/**
 * Bind a playa item (camp, art, event) database row to a view with a simple name & distance display,
 * using the device's location and date when the adapter was constructed.
 * <p>
 * TODO: Update device location periodically
 */
public class ArtCursorAdapter extends PlayaItemCursorAdapter<ArtCursorAdapter.ViewHolder> {

    static final String[] Projection = new String[]{
            ArtTable.artist,
            ArtTable.artistLoc,
    };

    private int artistCol;
    private int artistLocCol;

    public ArtCursorAdapter(Context context, Cursor cursor, AdapterListener listener) {
        super(context, cursor, listener);
        this.listener = listener;

        LocationProvider.getLastLocation(context).
                subscribe(lastLocation -> deviceLocation = lastLocation);
    }

    public static class ViewHolder extends PlayaItemCursorAdapter.ViewHolder {
        TextView artistView;

        public ViewHolder(View view) {
            super(view);

            artistView = (TextView) view.findViewById(R.id.artist);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.art_listview_item, parent, false);
        ViewHolder vh = new ViewHolder(itemView);

        setupClickListeners(vh, Constants.PlayaItemType.ART);

        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {

        super.onBindViewHolder(viewHolder, cursor);

        if (artistCol == 0) {
            artistCol = cursor.getColumnIndexOrThrow(ArtTable.artist);
            artistLocCol = cursor.getColumnIndexOrThrow(ArtTable.artistLoc);
        }

        viewHolder.artistView.setText(cursor.getString(artistCol));
    }

    @Override
    public String[] getRequiredProjection() {
        return buildRequiredProjection(Projection);
    }
}