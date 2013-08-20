package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.SectionIndexer;
import android.widget.TextView;
import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.EventTable;

public class EventCursorAdapter extends SimpleCursorAdapter implements SectionIndexer{


	public EventCursorAdapter(Context context, Cursor c){
		super(context, R.layout.double_listview_item, c, new String[]{} , new int[]{}, 0);
	}
	
	@Override
	public int getPositionForSection(int section) {
        /*
		int result;
		//Log.d("EventSection",String.valueOf(section));
		//Log.d("SectionIndexer","getPos for " + String.valueOf(section)+ " : " + String.valueOf(EventFragment.CursorLoaderListFragment.daySections[section]));
		if(section-1 >= EventFragment.CursorLoaderListFragment.daySections.length)
			result = EventFragment.CursorLoaderListFragment.daySections[EventFragment.CursorLoaderListFragment.daySections.length-1];
		else if(section == 0){
			result = 0;
			//result = EventFragment.CursorLoaderListFragment.daySections[0];
		}
		else
			result = EventFragment.CursorLoaderListFragment.daySections[section-1];
		//Log.d("EventSection-Position",String.valueOf(result) + " for " + String.valueOf(section));
		return result;
		*/
        return 0;
	}


	@Override
	public int getSectionForPosition(int position) {
        /*
		int result;
		for(int x=0;x<EventFragment.CursorLoaderListFragment.daySections.length;x++){
			if(EventFragment.CursorLoaderListFragment.daySections[x] < position){
				//Log.d("SectionIndexer","getSection for " + String.valueOf(position)+ " : " + String.valueOf(x));
				result = x;
			}
		}
		// this should never happen
		result = EventFragment.CursorLoaderListFragment.daySections.length;
		//Log.d("EventSection-Section",String.valueOf(result) + " for " + String.valueOf(position));
		return result;
		*/
        return 0;
	}


	@Override
	public Object[] getSections() {
		return new String[]{"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su", "Mo"};
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
            
        	view_cache.title_col = cursor.getColumnIndexOrThrow(EventTable.COLUMN_NAME);
        	view_cache.sub_col = cursor.getColumnIndexOrThrow(EventTable.COLUMN_START_TIME);
        	view_cache._id_col = cursor.getColumnIndexOrThrow(EventTable.COLUMN_ID);
        	if(cursor.getInt(cursor.getColumnIndexOrThrow(EventTable.COLUMN_ALL_DAY)) == 1 ){
        		view_cache.all_day = true;
        		view_cache.time_label = "All " + cursor.getString(cursor.getColumnIndexOrThrow(EventTable.COLUMN_START_TIME_PRINT));
        	}
        	else {
        		view_cache.all_day = false;
        		view_cache.time_label = cursor.getString(cursor.getColumnIndexOrThrow(EventTable.COLUMN_START_TIME_PRINT));
        	}
        	view_cache._id_col = cursor.getColumnIndexOrThrow(EventTable.COLUMN_ID);
        }
        view_cache.title.setText(cursor.getString(view_cache.title_col));
        view_cache.sub.setText(view_cache.time_label);

        view.setTag(R.id.list_item_related_model, cursor.getInt(view_cache._id_col));
        view.setTag(R.id.list_item_related_model_type, Constants.PLAYA_ITEM.EVENT);
    }
	
	// Cache the views within a ListView row item 
    static class ViewCache {
        TextView title;
        TextView sub;
        
        boolean all_day;
        String time_label;
        
        int title_col; 
        int sub_col;
        int _id_col;
    }
}
