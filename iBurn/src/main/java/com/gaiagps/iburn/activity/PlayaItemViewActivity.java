package com.gaiagps.iburn.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.PlayaClient;
import com.gaiagps.iburn.PlayaUtils;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.ArtTable;
import com.gaiagps.iburn.database.CampTable;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.PlayaContentProvider;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.database.generated.PlayaDatabase;
import com.gaiagps.iburn.fragment.GoogleMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Calendar;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Show the detail view for a Camp, Art installation, or Event
 * Created by davidbrodsky on 8/11/13.
 */
public class PlayaItemViewActivity extends AppCompatActivity {

    String modelTable;
    Uri uri;
    int modelId;
    LatLng latLng;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.text_container)
    ViewGroup textContainer;

    @InjectView(R.id.title)
    TextView titleTextView;

    @InjectView(R.id.favorite_button)
    FloatingActionButton favoriteButton;

    @InjectView(R.id.subitem_1)
    TextView subItem1TextView;

    @InjectView(R.id.subitem_2)
    TextView subItem2TextView;

    @InjectView(R.id.subitem_3)
    TextView subItem3TextView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//
//            Fade fade = new Fade();
//            fade.setDuration(250);
////            fade.excludeTarget(android.R.id.statusBarBackground, true);
////            fade.excludeTarget(android.R.id.navigationBarBackground, true);
//            getWindow().setAllowEnterTransitionOverlap(false);
//            getWindow().setAllowReturnTransitionOverlap(false);
//            getWindow().setExitTransition(fade);
//            getWindow().setEnterTransition(fade);
//        }

        setContentView(R.layout.activity_playa_item_view);
        ButterKnife.inject(this);

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // TODO : Assure you can't scroll favorite button off-screen
        populateViews(getIntent());

        setTextContainerMinHeight();
    }

    /**
     * Set the text container within NestedScrollView to have height exactly equal to the
     * full height minus status bar and toolbar. This addresses an issue where the
     * collapsing toolbar pattern gets all screwed up.
     */
    private void setTextContainerMinHeight() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int height = size.y;

        int[] textSizeAttr = new int[] { R.attr.actionBarSize };
        int indexOfAttrTextSize = 0;
        TypedArray a = obtainStyledAttributes(textSizeAttr);
        int abHeight = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
        a.recycle();

        Resources r = getResources();
        int statusBarPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, r.getDisplayMetrics());

        textContainer.setMinimumHeight(height - abHeight - statusBarPx);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu){
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void populateViews(Intent i){
        modelId = i.getIntExtra("model_id",0);
        Constants.PlayaItemType model_type = (Constants.PlayaItemType) i.getSerializableExtra("model_type");
        String selection = "_id = ?";
        String[] projection = null;
        switch(model_type){
            case CAMP:
                projection = null;
                uri = PlayaContentProvider.Camps.CAMPS;
                modelTable = com.gaiagps.iburn.database.PlayaDatabase.CAMPS;
                break;
            case ART:
                projection = null;
                uri = PlayaContentProvider.Art.ART;
                modelTable = com.gaiagps.iburn.database.PlayaDatabase.ART;
                break;
            case EVENT:
                projection = null;
                uri = PlayaContentProvider.Events.EVENTS;
                modelTable = com.gaiagps.iburn.database.PlayaDatabase.EVENTS;
                break;
        }

        Cursor c = getContentResolver().query(uri, projection, selection, new String[]{String.valueOf(modelId)}, null);
        try {
            if (c != null && c.moveToFirst()) {
                final String title = c.getString(c.getColumnIndexOrThrow(PlayaItemTable.name));
                titleTextView.setText(title);
                int isFavorite = c.getInt(c.getColumnIndex(PlayaItemTable.favorite));
                if (isFavorite == 1)
                    favoriteButton.setImageResource(R.drawable.ic_heart_pressed);
                else
                    favoriteButton.setImageResource(R.drawable.ic_heart);

                favoriteButton.setTag(R.id.list_item_related_model, modelId);
                favoriteButton.setTag(R.id.list_item_related_model_type, model_type);
                favoriteButton.setTag(R.id.favorite_button_state, isFavorite);
                favoriteButton.setOnClickListener(favoriteButtonOnClickListener);

                if (!c.isNull(c.getColumnIndex(PlayaItemTable.description))) {
                    ((TextView) findViewById(R.id.body)).setText(c.getString(c.getColumnIndexOrThrow(PlayaItemTable.description)));
                } else
                    findViewById(R.id.body).setVisibility(View.GONE);

                if (PlayaClient.isEmbargoClear(getApplicationContext()) && !c.isNull(c.getColumnIndex(PlayaItemTable.latitude))) {
                    latLng = new LatLng(c.getDouble(c.getColumnIndexOrThrow(PlayaItemTable.latitude)), c.getDouble(c.getColumnIndexOrThrow(PlayaItemTable.longitude)));
                    //TextView locationView = ((TextView) findViewById(R.id.location));
                    final GoogleMapFragment mapFragment = (GoogleMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    LatLng start = new LatLng(Constants.MAN_LAT, Constants.MAN_LON);
                    Log.i("GoogleMapFragment", "adding / centering marker");
                    mapFragment.showcaseMarker(new MarkerOptions().position(latLng));
                    mapFragment.getMap().getUiSettings().setMyLocationButtonEnabled(false);
                    mapFragment.getMap().getUiSettings().setZoomControlsEnabled(false);
                    mapFragment.getMap().setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                        @Override
                        public void onCameraChange(CameraPosition cameraPosition) {
                            if (cameraPosition.zoom >= 20) {
                                mapFragment.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(cameraPosition.target, (float) 19.99));
                            }
                        }
                    });
                    //locationView.setText(String.format("%f, %f", latLng.latitude, latLng.longitude));
                } else {
                    // Adjust the margin / padding show the heart icon doesn't
                    // overlap title + descrition
                    findViewById(R.id.map).setVisibility(View.INVISIBLE);
                    FrameLayout.LayoutParams parms = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 150);
                    findViewById(R.id.map).setLayoutParams(parms);
                }

                switch (model_type) {
                    case ART:
                        if (!c.isNull(c.getColumnIndex(ArtTable.playaAddress))) {
                            subItem1TextView.setText(c.getString(c.getColumnIndexOrThrow(ArtTable.playaAddress)));
                        } else
                            subItem1TextView.setVisibility(View.GONE);

                        if (!c.isNull(c.getColumnIndex(ArtTable.artist))) {
                            subItem2TextView.setText(c.getString(c.getColumnIndexOrThrow(ArtTable.artist)));
                        } else
                            subItem2TextView.setVisibility(View.GONE);

                        if (!c.isNull(c.getColumnIndex(ArtTable.artistLoc))) {
                            subItem3TextView.setText(c.getString(c.getColumnIndexOrThrow(ArtTable.artistLoc)));
                        } else
                            subItem3TextView.setVisibility(View.GONE);
                        break;
                    case CAMP:
                        if (!c.isNull(c.getColumnIndex(CampTable.playaAddress))) {
                            subItem1TextView.setText(c.getString(c.getColumnIndexOrThrow(CampTable.playaAddress)));
                        } else
                            subItem1TextView.setVisibility(View.GONE);

                        if (!c.isNull(c.getColumnIndex(CampTable.hometown))) {
                            subItem2TextView.setText(c.getString(c.getColumnIndexOrThrow(CampTable.hometown)));
                        } else
                            subItem2TextView.setVisibility(View.GONE);
                        subItem3TextView.setVisibility(View.GONE);
                        break;
                    case EVENT:
                        if (!c.isNull(c.getColumnIndex(EventTable.playaAddress))) {
                            subItem1TextView.setText(c.getString(c.getColumnIndexOrThrow(EventTable.playaAddress)));
                        } else
                            subItem1TextView.setVisibility(View.GONE);

                        Date nowDate = new Date();
                        Calendar nowPlusOneHrDate = Calendar.getInstance();
                        nowPlusOneHrDate.setTime(nowDate);
                        nowPlusOneHrDate.add(Calendar.HOUR, 1);

                        subItem2TextView.setText(PlayaUtils.getDateString(this, nowDate, nowPlusOneHrDate.getTime(),
                                c.getString(c.getColumnIndexOrThrow(EventTable.startTime)),
                                c.getString(c.getColumnIndexOrThrow(EventTable.startTimePrint)),
                                c.getString(c.getColumnIndexOrThrow(EventTable.endTime)),
                                c.getString(c.getColumnIndexOrThrow(EventTable.endTimePrint))));
                        subItem3TextView.setVisibility(View.GONE);
                        break;
                }
            }
        } finally {
            if (c != null) c.close();
        }
    }

    View.OnClickListener favoriteButtonOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            boolean makeFavorite = false;
            if ((Integer) v.getTag(R.id.favorite_button_state) == 0) {
                makeFavorite = true;
                v.setTag(R.id.favorite_button_state, 1);
                ((ImageView) v).setImageResource(R.drawable.ic_heart_pressed);
            } else if ((Integer) v.getTag(R.id.favorite_button_state) == 1) {
                v.setTag(R.id.favorite_button_state, 0);
                ((ImageView) v).setImageResource(R.drawable.ic_heart);
            }
            DataProvider.getInstance(PlayaItemViewActivity.this).updateFavorite(modelTable, modelId, makeFavorite);
//            int result = getContentResolver().update(uri, values, PlayaItemTable.id + " = ?", new String[]{String.valueOf(modelId)});
        }
    };
}