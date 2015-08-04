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
 */
public class CampCursorAdapter extends PlayaItemCursorAdapter<CampCursorAdapter.ViewHolder> {

    static final String[] Projection = new String[]{
            CampTable.playaAddress,
    };

    private int addressCol;

    public CampCursorAdapter(Context context, Cursor cursor, AdapterListener listener) {
        super(context, cursor, listener);
    }

    public static class ViewHolder extends PlayaItemCursorAdapter.ViewHolder {
        TextView addressView;

        public ViewHolder(View view) {
            super(view);

            addressView = (TextView) view.findViewById(R.id.address);
        }
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
    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {

        super.onBindViewHolder(viewHolder, cursor);

        if (addressCol == 0) {
            addressCol = cursor.getColumnIndexOrThrow(CampTable.playaAddress);
        }

        String address = cursor.getString(addressCol);
        if (!TextUtils.isEmpty(address)) {
            viewHolder.addressView.setText(cursor.getString(addressCol));
        } else {
            viewHolder.addressView.setVisibility(View.GONE);
        }
    }

    @Override
    public String[] getRequiredProjection() {
        return buildRequiredProjection(Projection);
    }
}