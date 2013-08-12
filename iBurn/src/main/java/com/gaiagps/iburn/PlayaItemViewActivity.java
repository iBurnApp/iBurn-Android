package com.gaiagps.iburn;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.gaiagps.iburn.database.ArtTable;
import com.gaiagps.iburn.database.CampTable;
import com.gaiagps.iburn.database.EventTable;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by davidbrodsky on 8/11/13.
 */
public class PlayaItemViewActivity extends Activity {

    Uri uri;
    int model_id;
    LatLng latLng;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playa_item_view);

        populateViews(getIntent());
    }

    private void populateViews(Intent i){
        model_id = i.getIntExtra("model_id",0);
        Constants.PLAYA_ITEM model_type = (Constants.PLAYA_ITEM) i.getSerializableExtra("playa_item");
        String selection = "_id = ?";
        String[] projection = null;
        switch(model_type){
            case CAMP:
                projection = CampTable.COLUMNS;
                uri = PlayaContentProvider.CAMP_URI;
                break;
            case ART:
                projection = ArtTable.COLUMNS;
                uri = PlayaContentProvider.ART_URI;
                break;
            case EVENT:
                projection = EventTable.COLUMNS;
                uri = PlayaContentProvider.EVENT_URI;
                break;
        }

        Cursor c = getContentResolver().query(uri, projection, selection, new String[]{String.valueOf(model_id)}, null);
        if(c != null && c.moveToFirst()){
            final String title = c.getString(c.getColumnIndexOrThrow("name"));
            ((TextView) findViewById(R.id.title)).setText(title);
            int isFavorite = c.getInt(c.getColumnIndex("favorite"));
            View favoriteBtn = findViewById(R.id.favorite_button);
            if(isFavorite == 1)
                ((ImageView)favoriteBtn).setImageResource(android.R.drawable.star_big_on);
            else
                ((ImageView)favoriteBtn).setImageResource(android.R.drawable.star_big_off);
            favoriteBtn.setTag(R.id.list_item_related_model, model_id);
            favoriteBtn.setTag(R.id.list_item_related_model_type, model_type);
            favoriteBtn.setTag(R.id.favorite_button_state, isFavorite);
            favoriteBtn.setOnClickListener(favoriteButtonOnClickListener);

            if(!c.isNull(c.getColumnIndex("description"))){
                ((TextView) findViewById(R.id.body)).setText(c.getString(c.getColumnIndexOrThrow("description")));
                findViewById(R.id.body).setVisibility(View.VISIBLE);
            }

            if(BurnState.embargoClear && !c.isNull(c.getColumnIndex("latitude"))){
                latLng = new LatLng(c.getDouble(c.getColumnIndexOrThrow("latitude")), c.getDouble(c.getColumnIndexOrThrow("longitude")));
                TextView locationView = ((TextView) findViewById(R.id.location));
                locationView.setVisibility(View.VISIBLE);
                locationView.setText(String.format("%f, %f", latLng.latitude, latLng.longitude));
                locationView.setOnTouchListener(new View.OnTouchListener(){

                    @Override
                    public boolean onTouch(View v, MotionEvent me) {
                        if(me.getAction() == MotionEvent.ACTION_DOWN){
                            Intent i = new Intent(PlayaItemViewActivity.this, MainActivity.class);
                            i.putExtra("tab", Constants.TAB_TYPE.MAP);
                            i.putExtra("lat", latLng.latitude);
                            i.putExtra("lon", latLng.longitude);
                            i.putExtra("title", title);
                            PlayaItemViewActivity.this.startActivity(i);
                            return true;
                        }
                        return false;
                    }

                });
            }

            switch(model_type){
                case ART:
                    if(!c.isNull(c.getColumnIndex(ArtTable.COLUMN_ARTIST))){
                        ((TextView) findViewById(R.id.subitem_1)).setText(c.getString(c.getColumnIndexOrThrow(ArtTable.COLUMN_ARTIST)));
                        findViewById(R.id.subitem_1).setVisibility(View.VISIBLE);
                    }
                    if(!c.isNull(c.getColumnIndex(ArtTable.COLUMN_CONTACT))){
                        ((TextView) findViewById(R.id.subitem_2)).setText(c.getString(c.getColumnIndexOrThrow(ArtTable.COLUMN_CONTACT)));
                        findViewById(R.id.subitem_2).setVisibility(View.VISIBLE);
                    }
                    if(!c.isNull(c.getColumnIndex(ArtTable.COLUMN_ARTIST_LOCATION))){
                        ((TextView) findViewById(R.id.subitem_3)).setText(c.getString(c.getColumnIndexOrThrow(ArtTable.COLUMN_ARTIST_LOCATION)));
                        findViewById(R.id.subitem_3).setVisibility(View.VISIBLE);
                    }
                    break;
                case CAMP:
                    if(!c.isNull(c.getColumnIndex(CampTable.COLUMN_CONTACT))){
                        ((TextView) findViewById(R.id.subitem_1)).setText(c.getString(c.getColumnIndexOrThrow(CampTable.COLUMN_CONTACT)));
                        findViewById(R.id.subitem_1).setVisibility(View.VISIBLE);
                    }
                    if(!c.isNull(c.getColumnIndex(CampTable.COLUMN_HOMETOWN))){
                        ((TextView) findViewById(R.id.subitem_2)).setText(c.getString(c.getColumnIndexOrThrow(CampTable.COLUMN_HOMETOWN)));
                        findViewById(R.id.subitem_2).setVisibility(View.VISIBLE);
                    }
                    break;
                case EVENT:
                    if(!c.isNull(c.getColumnIndex(EventTable.COLUMN_HOST_CAMP_NAME))){
                        ((TextView) findViewById(R.id.subitem_1)).setText(c.getString(c.getColumnIndexOrThrow(EventTable.COLUMN_HOST_CAMP_NAME)));
                        findViewById(R.id.subitem_1).setVisibility(View.VISIBLE);
                    }
                    if(BurnState.embargoClear && !c.isNull(c.getColumnIndex(EventTable.COLUMN_LOCATION))){
                        ((TextView) findViewById(R.id.subitem_2)).setText(c.getString(c.getColumnIndexOrThrow(EventTable.COLUMN_LOCATION)));
                        findViewById(R.id.subitem_2).setVisibility(View.VISIBLE);
                    }
                    break;
            }
        }
    }

    View.OnClickListener favoriteButtonOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            ContentValues values = new ContentValues();
            if((Integer)v.getTag(R.id.favorite_button_state) == 0){
                values.put("favorite", 1);
                v.setTag(R.id.favorite_button_state, 1);
                ((ImageView)v).setImageResource(android.R.drawable.star_big_on);
            }
            else if((Integer)v.getTag(R.id.favorite_button_state) == 1){
                values.put("favorite", 0);
                v.setTag(R.id.favorite_button_state, 0);
                ((ImageView)v).setImageResource(android.R.drawable.star_big_off);
            }
            int result = getContentResolver().update(uri.buildUpon().appendPath(String.valueOf(model_id)).build(),
                    values, null, null);
        }
    };
}