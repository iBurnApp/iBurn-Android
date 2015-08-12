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
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.location.LocationProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Bind a playa item (camp, art, event) database row to a view with a simple name & distance display,
 * using the device's location and date when the adapter was constructed.
 */
public class PlayaSearchResponseCursorAdapter extends SectionedCursorAdapter<PlayaItemCursorAdapter.ViewHolder> {

    private static int typeCol;

    public PlayaSearchResponseCursorAdapter(Context context, Cursor cursor, AdapterListener listener) {
        super(context, cursor, listener);
        this.listener = listener;
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
                    .inflate(R.layout.camp_listview_item, parent, false);

            holder = new ViewHolder(itemView);

            itemView.setOnClickListener(view -> {
                int modelId = (int) view.getTag(R.id.list_item_related_model);
                int modelType = (int) view.getTag(R.id.list_item_related_model_type);
                listener.onItemSelected(modelId, DataProvider.getTypeValue(modelType));
            });

            itemView.findViewById(R.id.heart).setOnClickListener(v -> {
                int modelId = (int) ((View) v.getParent()).getTag(R.id.list_item_related_model);
                int modelType = (int) ((View) v.getParent()).getTag(R.id.list_item_related_model_type);
                listener.onItemFavoriteButtonSelected(modelId, DataProvider.getTypeValue(modelType));
            });
        }

        return holder;
    }

    @Override
    protected List<Integer> createHeadersForCursor(Cursor cursor) {
        ArrayList<Integer> headers = new ArrayList<>();
        cacheCursorColumns(cursor);

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

        super.onBindViewHolder(viewHolder, cursor);

        cacheCursorColumns(cursor);

        setLinearSlmParameters(viewHolder, position);

        viewHolder.itemView.setTag(R.id.list_item_related_model, cursor.getInt(idCol));
        viewHolder.itemView.setTag(R.id.list_item_related_model_type, cursor.getInt(typeCol));
    }

    @Override
    protected void onBindViewHolderHeader(ViewHolder holder, Cursor firstSectionItem, int position) {

        setLinearSlmParameters(holder, position);

        cacheCursorColumns(firstSectionItem);

        Constants.PlayaItemType type = DataProvider.getTypeValue(firstSectionItem.getInt(typeCol));
        String headerTitle = null;
        switch (type) {
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

    private void cacheCursorColumns(Cursor cursor) {
        typeCol = cursor.getColumnIndexOrThrow(DataProvider.VirtualType); // This is a virtual column created during UNION queries of multiple tables
    }
}