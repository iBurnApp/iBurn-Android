package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.TextView;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.GeoUtils;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.location.DeviceLocation;

public class PlayaItemCursorAdapter extends SimpleCursorAdapter {

    private Location mDeviceLocation;
    private Constants.PLAYA_ITEM mType;

	public PlayaItemCursorAdapter(Context context, Cursor c, Constants.PLAYA_ITEM type){
		super(context, R.layout.double_listview_item, c, new String[]{} , new int[]{}, 0);
        mType = type;

        DeviceLocation.getLastKnownLocation(context, false, new DeviceLocation.LocationResult() {
            @Override
            public void gotLocation(Location location) {
                mDeviceLocation = location;
            }
        });
	}

	@Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        ViewCache view_cache = (ViewCache) view.getTag(R.id.list_item_cache);
        if (view_cache == null) {
        	view_cache = new ViewCache();
        	view_cache.title = (TextView) view.findViewById(R.id.list_item_title);
            view_cache.distance = (TextView) view.findViewById(R.id.list_item_sub_right);
            
        	view_cache.title_col = cursor.getColumnIndexOrThrow(PlayaItemTable.name);
        	view_cache._id_col = cursor.getColumnIndexOrThrow(PlayaItemTable.id);
            view_cache.lat_col = cursor.getColumnIndexOrThrow(PlayaItemTable.latitude);
            view_cache.lon_col = cursor.getColumnIndexOrThrow(PlayaItemTable.longitude);
            view.setTag(R.id.list_item_cache, view_cache);
            view.setTag(R.id.list_item_related_model, cursor.getInt(view_cache._id_col));
        }

        view_cache.title.setText(cursor.getString(view_cache.title_col));

        // Approx distance
        if (mDeviceLocation != null && cursor.getDouble(view_cache.lat_col) != 0) {
            view_cache.distance.setText(String.format("%d m",
                    ((Double)(GeoUtils.getDistance(cursor.getDouble(view_cache.lat_col),
                            cursor.getDouble(view_cache.lon_col), mDeviceLocation))).intValue()));
        } else {
            view_cache.distance.setText("");
        }

        view.setTag(R.id.list_item_related_model, cursor.getInt(view_cache._id_col));
        view.setTag(R.id.list_item_related_model_type, mType);
    }
	
	// Cache the views within a ListView row item 
    static class ViewCache {
        TextView title;
        TextView distance;
        
        int title_col;
        int lat_col;
        int lon_col;
        int _id_col;
    }
}
