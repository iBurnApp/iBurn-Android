package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.CampTable;

/**
 * Bind a playa item (camp, art, event) database row to a view with a simple name & distance display,
 * using the device's location and date when the adapter was constructed.
 * <p>
 * TODO: Update device location periodically
 * TODO: Remove this
 */
public class CampCursorAdapter extends PlayaItemCursorAdapter<PlayaItemCursorAdapter.ViewHolder> {

    public CampCursorAdapter(Context context, Cursor c, AdapterListener listener) {
        super(context, c, listener);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.camp_listview_item, parent, false);
        ViewHolder vh = new ViewHolder(itemView);

        setupClickListeners(vh, Constants.PlayaItemType.CAMP);

        return vh;
    }

    @Override
    public String[] getRequiredProjection() {
        return PlayaItemCursorAdapter.Projection;
    }
}