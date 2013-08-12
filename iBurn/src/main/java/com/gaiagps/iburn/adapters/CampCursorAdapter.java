package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.TextView;
import com.gaiagps.iburn.BurnState;
import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.CampTable;

public class CampCursorAdapter extends SimpleCursorAdapter {
	
	public CampCursorAdapter(Context context, Cursor c){
		super(context, R.layout.camp_listview_item, c, new String[]{} , new int[]{}, 0);
	}

	
	@Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        ViewCache view_cache = (ViewCache) view.getTag(R.id.list_item_cache);
        if (view_cache == null) {
        	view_cache = new ViewCache();
        	view_cache.title = (TextView) view.findViewById(R.id.list_item_title);
        	view_cache.sub = (TextView) view.findViewById(R.id.list_item_sub);
        	//view_cache.thumbnail = (ImageView) view.findViewById(R.id.list_item_image);
            
        	view_cache.title_col = cursor.getColumnIndexOrThrow(CampTable.COLUMN_NAME);
        	view_cache.sub_col = cursor.getColumnIndexOrThrow(CampTable.COLUMN_LOCATION);
        	//view_cache.thumbnail_col = cursor.getColumnIndexOrThrow(SQLiteWrapper.COLUMN_THUMBNAIL_PATH);
        	view_cache._id_col = cursor.getColumnIndexOrThrow(CampTable.COLUMN_ID);
            view.setTag(R.id.list_item_cache, view_cache);
            //tag view with timelapse id
            view.setTag(R.id.list_item_related_model, cursor.getInt(view_cache._id_col));
        }
        //Log.d("bindView","yeah");
        view_cache.title.setText(cursor.getString(view_cache.title_col));

        if(BurnState.embargoClear && view_cache.sub != null){
        	view_cache.sub.setText(cursor.getString(view_cache.sub_col));
        }
        //view_cache.thumbnail.setImageBitmap(BitmapFactory.decodeFile(cursor.getString(view_cache.thumbnail_col)));
        view.setTag(R.id.list_item_related_model, cursor.getInt(view_cache._id_col));
        view.setTag(R.id.list_item_related_model_type, Constants.PLAYA_ITEM.CAMP);
    }
	
	// Cache the views within a ListView row item 
    static class ViewCache {
        TextView title;
        //TextView body;
        TextView sub;
        
        int title_col; 
        int sub_col;
        int _id_col;
    }
}
