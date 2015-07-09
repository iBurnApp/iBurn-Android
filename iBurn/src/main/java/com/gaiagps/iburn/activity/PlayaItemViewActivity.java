package com.gaiagps.iburn.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.CurrentDateProvider;
import com.gaiagps.iburn.DateUtil;
import com.gaiagps.iburn.Geo;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.EventCursorAdapter;
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter;
import com.gaiagps.iburn.database.ArtTable;
import com.gaiagps.iburn.database.CampTable;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.fragment.GoogleMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.sqlbrite.SqlBrite;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Show the detail view for a Camp, Art installation, or Event
 * Created by davidbrodsky on 8/11/13.
 */
public class PlayaItemViewActivity extends AppCompatActivity {

    DataProvider provider;

    String modelTable;
    int modelId;
    LatLng latLng;

    boolean isFavorite;
    boolean showingLocation;

    MenuItem favoriteMenuItem;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.map_container)
    FrameLayout mapContainer;

    @InjectView(R.id.overflow_container)
    LinearLayout overflowContainer;

    @InjectView(R.id.text_container)
    LinearLayout textContainer;

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

    @InjectView(R.id.collapsing_toolbar)
    CollapsingToolbarLayout collapsingToolbarLayout;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playa_item_view);
        ButterKnife.inject(this);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        populateViews(getIntent());
        setTextContainerMinHeight();

        AlphaAnimation fadeAnimation = new AlphaAnimation(0, 1);
        fadeAnimation.setDuration(1000);
        fadeAnimation.setStartOffset(250);
        fadeAnimation.setFillAfter(true);
        fadeAnimation.setFillEnabled(true);
//        mapContainer.setAlpha(0);
        mapContainer.startAnimation(fadeAnimation);

        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
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

        int[] textSizeAttr = new int[]{R.attr.actionBarSize};
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_playa_item, menu);

        favoriteMenuItem = menu.findItem(R.id.favorite_menu);
        if (isFavorite) favoriteMenuItem.setIcon(R.drawable.ic_heart_full);
        if (showingLocation) favoriteMenuItem.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.favorite_menu:
                setFavorite(!isFavorite);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void populateViews(Intent i) {
        modelId = i.getIntExtra("model_id", 0);
        Constants.PlayaItemType model_type = (Constants.PlayaItemType) i.getSerializableExtra("model_type");
        switch (model_type) {
            case CAMP:
                modelTable = PlayaDatabase.CAMPS;
                break;
            case ART:
                modelTable = PlayaDatabase.ART;
                break;
            case EVENT:
                modelTable = PlayaDatabase.EVENTS;
                break;
        }

        DataProvider.getInstance(this)
                .subscribeOn(Schedulers.computation())
                .doOnNext(dataProvider -> this.provider = dataProvider)
                .flatMap(dataProvider -> dataProvider.createQuery(modelTable, "SELECT * FROM " + modelTable + " WHERE _id = ?", String.valueOf(modelId)))
                .first()    // Do we want to receive updates?
                .map(SqlBrite.Query::run)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(itemCursor -> {
                    try {
                        if (itemCursor != null && itemCursor.moveToFirst()) {
                            final String title = itemCursor.getString(itemCursor.getColumnIndexOrThrow(PlayaItemTable.name));
                            titleTextView.setText(title);
                            isFavorite = itemCursor.getInt(itemCursor.getColumnIndex(PlayaItemTable.favorite)) == 1;
                            if (isFavorite)
                                favoriteButton.setImageResource(R.drawable.ic_heart_pressed);
                            else
                                favoriteButton.setImageResource(R.drawable.ic_heart);

                            favoriteButton.setTag(R.id.list_item_related_model, modelId);
                            favoriteButton.setTag(R.id.list_item_related_model_type, model_type);
                            favoriteButton.setOnClickListener(favoriteButtonOnClickListener);

                            if (!itemCursor.isNull(itemCursor.getColumnIndex(PlayaItemTable.description))) {
                                ((TextView) findViewById(R.id.body)).setText(itemCursor.getString(itemCursor.getColumnIndexOrThrow(PlayaItemTable.description)));
                            } else
                                findViewById(R.id.body).setVisibility(View.GONE);

                            showingLocation = !itemCursor.isNull(itemCursor.getColumnIndex(PlayaItemTable.latitude));
                            if (showingLocation) {
                                favoriteButton.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                                    if (favoriteMenuItem != null)
                                        favoriteMenuItem.setVisible(v.getVisibility() == View.GONE);
                                });
                                latLng = new LatLng(itemCursor.getDouble(itemCursor.getColumnIndexOrThrow(PlayaItemTable.latitude)), itemCursor.getDouble(itemCursor.getColumnIndexOrThrow(PlayaItemTable.longitude)));
                                //TextView locationView = ((TextView) findViewById(R.id.location));
                                LatLng start = new LatLng(Geo.MAN_LAT, Geo.MAN_LON);
                                Log.i("GoogleMapFragment", "adding / centering marker");
                                GoogleMapFragment mapFragment = (GoogleMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                                mapFragment.showcaseMarker(new MarkerOptions().position(latLng));
                                mapFragment.getMapAsync(googleMap -> {
                                    UiSettings settings = googleMap.getUiSettings();
                                    settings.setMyLocationButtonEnabled(false);
                                    settings.setZoomControlsEnabled(false);
                                    googleMap.setOnCameraChangeListener(cameraPosition -> {
                                        if (cameraPosition.zoom >= 20) {
                                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cameraPosition.target, (float) 19.99));
                                        }
                                    });
                                });
                                //favoriteMenuItem.setVisible(false);
                                //locationView.setText(String.format("%f, %f", latLng.latitude, latLng.longitude));
                            } else {
                                // Adjust the margin / padding show the heart icon doesn't
                                // overlap title + descrition
//                                findViewById(R.id.map_container).setVisibility(View.GONE);
                                //getSupportFragmentManager().beginTransaction().remove(mapFragment).commit();
                                collapsingToolbarLayout.setBackgroundResource(android.R.color.transparent);
                                CollapsingToolbarLayout.LayoutParams parms = new CollapsingToolbarLayout.LayoutParams(CollapsingToolbarLayout.LayoutParams.MATCH_PARENT, 24);
                                mapContainer.setLayoutParams(parms);
                                favoriteButton.setVisibility(View.GONE);
                                //favoriteMenuItem.setVisible(true);
                            }

                            switch (model_type) {
                                case ART:
                                    if (!itemCursor.isNull(itemCursor.getColumnIndex(ArtTable.playaAddress))) {
                                        subItem1TextView.setText(itemCursor.getString(itemCursor.getColumnIndexOrThrow(ArtTable.playaAddress)));
                                    } else
                                        subItem1TextView.setVisibility(View.GONE);

                                    if (!itemCursor.isNull(itemCursor.getColumnIndex(ArtTable.artist))) {
                                        subItem2TextView.setText(itemCursor.getString(itemCursor.getColumnIndexOrThrow(ArtTable.artist)));
                                    } else
                                        subItem2TextView.setVisibility(View.GONE);

                                    if (!itemCursor.isNull(itemCursor.getColumnIndex(ArtTable.artistLoc))) {
                                        subItem3TextView.setText(itemCursor.getString(itemCursor.getColumnIndexOrThrow(ArtTable.artistLoc)));
                                    } else
                                        subItem3TextView.setVisibility(View.GONE);
                                    break;
                                case CAMP:
                                    if (!itemCursor.isNull(itemCursor.getColumnIndex(CampTable.playaAddress))) {
                                        subItem1TextView.setText(itemCursor.getString(itemCursor.getColumnIndexOrThrow(CampTable.playaAddress)));
                                    } else
                                        subItem1TextView.setVisibility(View.GONE);

                                    if (!itemCursor.isNull(itemCursor.getColumnIndex(CampTable.hometown))) {
                                        subItem2TextView.setText(itemCursor.getString(itemCursor.getColumnIndexOrThrow(CampTable.hometown)));
                                    } else
                                        subItem2TextView.setVisibility(View.GONE);
                                    subItem3TextView.setVisibility(View.GONE);
                                    break;
                                case EVENT:
                                    if (!itemCursor.isNull(itemCursor.getColumnIndex(EventTable.playaAddress))) {
                                        subItem1TextView.setText(itemCursor.getString(itemCursor.getColumnIndexOrThrow(EventTable.playaAddress)));
                                    } else
                                        subItem1TextView.setVisibility(View.GONE);

                                    Date nowDate = CurrentDateProvider.getCurrentDate();
                                    Calendar nowPlusOneHrDate = Calendar.getInstance();
                                    nowPlusOneHrDate.setTime(nowDate);
                                    nowPlusOneHrDate.add(Calendar.HOUR, 1);

                                    subItem2TextView.setText(DateUtil.getDateString(this, nowDate, nowPlusOneHrDate.getTime(),
                                            itemCursor.getString(itemCursor.getColumnIndexOrThrow(EventTable.startTime)),
                                            itemCursor.getString(itemCursor.getColumnIndexOrThrow(EventTable.startTimePrint)),
                                            itemCursor.getString(itemCursor.getColumnIndexOrThrow(EventTable.endTime)),
                                            itemCursor.getString(itemCursor.getColumnIndexOrThrow(EventTable.endTimePrint))));
                                    subItem3TextView.setVisibility(View.GONE);
                                    break;
                            }

                            // Look up hosted events or other occurrences
                            int playaId = itemCursor.getInt(itemCursor.getColumnIndex(PlayaItemTable.playaId));

                            if (modelTable.equals(PlayaDatabase.CAMPS)) {
                                // Lookup hosted events

                                EventCursorAdapter adapter = new EventCursorAdapter(this, null, true, (relatedEventId, type) -> {
                                    Intent intent = new Intent(this, PlayaItemViewActivity.class);
                                    intent.putExtra("model_id", relatedEventId);
                                    intent.putExtra("model_type", type);
                                    startActivity(intent);
                                });

                                provider.createQuery(PlayaDatabase.EVENTS, "SELECT " + DataProvider.makeProjectionString(adapter.getRequiredProjection()) + " FROM " + PlayaDatabase.EVENTS + " WHERE " + EventTable.campPlayaId + " = ? GROUP BY " + PlayaItemTable.name, String.valueOf(playaId))
                                        .first()
                                        .map(SqlBrite.Query::run)
                                        .subscribe(eventsCursor -> {

                                            if (eventsCursor == null) return;

                                            Timber.d("Got %d hosted events", eventsCursor.getCount());

                                            if (eventsCursor.getCount() == 0) {
                                                eventsCursor.close();
                                                return;
                                            }

                                            int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());

                                            ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.PlayaTextItem);

                                            TextView hostedEventsTitle = new TextView(wrapper);
                                            hostedEventsTitle.setText(R.string.hosted_events);
                                            hostedEventsTitle.setTextSize(32);
                                            hostedEventsTitle.setPadding(pad, pad, pad, pad);

                                            overflowContainer.addView(hostedEventsTitle);

                                            adapter.swapCursor(eventsCursor);

                                            for (int idx = 0; idx < eventsCursor.getCount(); idx++) {
                                                EventCursorAdapter.ViewHolder holder = adapter.createViewHolder(overflowContainer, 0);
                                                adapter.bindViewHolder(holder, idx);
                                                overflowContainer.addView(holder.itemView);
                                            }
                                            eventsCursor.close();
                                        }, throwable -> Timber.e(throwable, "Failed to bind hosted events"));

                            } else if (modelTable.equals(PlayaDatabase.EVENTS)) {
                                // Lookup other event occurrences
                                provider.createQuery(PlayaDatabase.EVENTS, "SELECT * FROM " + PlayaDatabase.EVENTS + " WHERE " + EventTable.playaId + " = ? AND " + EventTable.startTime + " != ?", String.valueOf(playaId), itemCursor.getString(itemCursor.getColumnIndex(EventTable.startTime)))
                                        .first()
                                        .map(SqlBrite.Query::run)
                                        .subscribe(eventsCursor -> {
                                            if (eventsCursor == null) return;

                                            Timber.d("Got %d other occurrences", eventsCursor.getCount());

                                            if (eventsCursor.getCount() == 0) {
                                                eventsCursor.close();
                                                return;
                                            }

                                            int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
                                            ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.PlayaTextItem);

                                            TextView occurrencesTitle = new TextView(wrapper);
                                            occurrencesTitle.setText(R.string.also_at);
                                            occurrencesTitle.setTextSize(32);
                                            occurrencesTitle.setPadding(pad,pad,pad,pad);
                                            overflowContainer.addView(occurrencesTitle);

                                            final SimpleDateFormat timeDayFormatter = new SimpleDateFormat("EEEE, M/d 'at' h:mm a", Locale.US);

                                            while (eventsCursor.moveToNext()) {
                                                TextView event = new TextView(wrapper);
                                                event.setTextSize(20);
                                                event.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                                try {
                                                    event.setText(timeDayFormatter.format(PlayaDateTypeAdapter.iso8601Format.parse(eventsCursor.getString(eventsCursor.getColumnIndex(EventTable.startTime)))));
                                                } catch (ParseException e) {
                                                    Timber.w(e, "Unable to parse date, using pre-computed");
                                                    event.setText(eventsCursor.getString(eventsCursor.getColumnIndex(EventTable.startTimePrint)).toUpperCase());
                                                }
                                                event.setOnClickListener(new RelatedItemOnClickListener(eventsCursor.getInt(eventsCursor.getColumnIndex(PlayaItemTable.id)), Constants.PlayaItemType.EVENT));
                                                event.setPadding(pad,pad,pad,pad);

                                                TypedValue outValue = new TypedValue();
                                                getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                                                event.setBackgroundResource(outValue.resourceId);
                                                overflowContainer.addView(event);
                                            }
                                            eventsCursor.close();
                                        }, throwable -> Timber.e(throwable, "Failed to bind event occurrences"));
                            }
                        }
                    } finally {
                        if (itemCursor != null) itemCursor.close();
                    }
                }, throwable -> Timber.e(throwable, "Failed to populate views from item"));
    }

    class RelatedItemOnClickListener implements View.OnClickListener {

        int modelId;
        Constants.PlayaItemType modeltype;

        public RelatedItemOnClickListener(int modelId, Constants.PlayaItemType modeltype) {
            this.modelId = modelId;
            this.modeltype = modeltype;
        }

        @Override
        public void onClick(View v) {
            Intent i = new Intent(PlayaItemViewActivity.this, PlayaItemViewActivity.class);
            i.putExtra("model_id", modelId);
            i.putExtra("model_type", modeltype);
            startActivity(i);
        }
    }

    View.OnClickListener favoriteButtonOnClickListener = (View v) -> {
        setFavorite(!isFavorite);
    };

    private void setFavorite(boolean isFavorite) {
        int newFabDrawableResId = isFavorite ? R.drawable.ic_heart_pressed : R.drawable.ic_heart;
        int newMenuDrawableResId = isFavorite ? R.drawable.ic_heart_full : R.drawable.ic_heart_empty;
        favoriteButton.setImageResource(newFabDrawableResId);
        favoriteMenuItem.setIcon(newMenuDrawableResId);

        DataProvider.getInstance(PlayaItemViewActivity.this)
                .subscribe(dataProvider -> dataProvider.updateFavorite(modelTable, modelId, isFavorite));
        this.isFavorite = isFavorite;
    }
}