package com.gaiagps.iburn.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gaiagps.iburn.AudioTourManager;
import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.CurrentDateProvider;
import com.gaiagps.iburn.DateUtil;
import com.gaiagps.iburn.Geo;
import com.gaiagps.iburn.MapboxMapFragment;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.view.AnimatedFloatingActionButton;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.squareup.sqlbrite.SqlBrite;

import org.prx.playerhater.PlayerHaterListener;
import org.prx.playerhater.Song;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * Show the detail view for a Camp, Art installation, or Event
 * Created by davidbrodsky on 8/11/13.
 */
public class PlayaItemViewActivity extends AppCompatActivity implements PlayerHaterListener {

    private static final boolean USE_MAPBOX_MAP = true;

    public static final String EXTRA_MODEL_ID = "model-id";
    public static final String EXTRA_MODEL_TYPE = "model-type";
    public static final String EXTRA_PLAYA_ITEM = "playa-item";

    DataProvider provider;

    String modelTable;
    int modelId;
    LatLng latLng;

    boolean isFavorite;
    boolean showingLocation;

    MenuItem favoriteMenuItem;

    private AudioTourManager audioTourManager;
    private boolean didPopulateViews;
    private TextView audioTourToggle;
    private String audioTourUrl;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.map_container)
    FrameLayout mapContainer;

    @BindView(R.id.overflow_container)
    LinearLayout overflowContainer;

    @BindView(R.id.text_container)
    LinearLayout textContainer;

    @BindView(R.id.title)
    TextView titleTextView;

    @BindView(R.id.fab)
    AnimatedFloatingActionButton favoriteButton;

    @BindView(R.id.subitem_1)
    TextView subItem1TextView;

    @BindView(R.id.subitem_2)
    TextView subItem2TextView;

    @BindView(R.id.subitem_3)
    TextView subItem3TextView;

    @BindView(R.id.collapsing_toolbar)
    CollapsingToolbarLayout collapsingToolbarLayout;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playa_item_view);
        ButterKnife.bind(this);
        didPopulateViews = false;

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        // Need to defer populating views until AudioTourManagerReady
        setTextContainerMinHeight();

        AlphaAnimation fadeAnimation = new AlphaAnimation(0, 1);
        fadeAnimation.setDuration(1000);
        fadeAnimation.setStartOffset(250);
        fadeAnimation.setFillAfter(true);
        fadeAnimation.setFillEnabled(true);
        mapContainer.startAnimation(fadeAnimation);

        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    @Override
    public void onResume() {
        super.onResume();
        audioTourManager = new AudioTourManager(this, this);
        if (!didPopulateViews) {
            populateViews(getIntent());
            didPopulateViews = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        audioTourManager.release();
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
                setFavorite(!isFavorite, true);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void populateViews(Intent i) {
//        modelId = i.getIntExtra(EXTRA_MODEL_ID, 0);
//        Constants.PlayaItemType model_type = (Constants.PlayaItemType) i.getSerializableExtra(EXTRA_PLAYA_ITEM);
//        switch (model_type) {
//            case CAMP:
//                modelTable = PlayaDatabase.CAMPS;
//                break;
//            case ART:
//                modelTable = PlayaDatabase.ART;
//                break;
//            case EVENT:
//                modelTable = PlayaDatabase.EVENTS;
//                break;
//        }
//
//        DataProvider.getInstance(getApplicationContext())
//                .subscribeOn(getIoScheduler())
//                .doOnNext(dataProvider -> this.provider = dataProvider)
//                .flatMap(dataProvider -> dataProvider.createQuery(modelTable, "SELECT * FROM " + modelTable + " WHERE _id = ?", String.valueOf(modelId)))
//                .first()    // Do we want to receive updates?
//                .map(SqlBrite.Query::run)
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(itemCursor -> {
//                    try {
//                        if (itemCursor != null && itemCursor.moveToFirst()) {
//                            final String title = itemCursor.getString(itemCursor.getColumnIndexOrThrow(PlayaItemTable.name));
//                            titleTextView.setText(title);
//                            isFavorite = itemCursor.getInt(itemCursor.getColumnIndex(PlayaItemTable.favorite)) == 1;
//                            setFavorite(isFavorite, false);
//
//                            favoriteButton.setTag(R.id.list_item_related_model, modelId);
//                            favoriteButton.setTag(R.id.list_item_related_model_type, model_type);
//                            favoriteButton.setOnClickListener(favoriteButtonOnClickListener);
//
//                            if (!itemCursor.isNull(itemCursor.getColumnIndex(PlayaItemTable.description))) {
//                                ((TextView) findViewById(R.id.body)).setText(itemCursor.getString(itemCursor.getColumnIndexOrThrow(PlayaItemTable.description)));
//                            } else
//                                findViewById(R.id.body).setVisibility(View.GONE);
//
//                            showingLocation = !itemCursor.isNull(itemCursor.getColumnIndex(PlayaItemTable.latitude)) && (itemCursor.getDouble(itemCursor.getColumnIndexOrThrow(PlayaItemTable.latitude)) != 0);
//                            if (showingLocation) {
//                                favoriteButton.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
//                                    if (favoriteMenuItem != null)
//                                        favoriteMenuItem.setVisible(v.getVisibility() == View.GONE);
//                                });
//                                latLng = new LatLng(itemCursor.getDouble(itemCursor.getColumnIndexOrThrow(PlayaItemTable.latitude)), itemCursor.getDouble(itemCursor.getColumnIndexOrThrow(PlayaItemTable.longitude)));
//                                //TextView locationView = ((TextView) findViewById(R.id.location));
//                                LatLng start = new LatLng(Geo.MAN_LAT, Geo.MAN_LON);
//                                Timber.d("adding / centering marker on %f, %f", latLng.latitude, latLng.longitude);
//                                GoogleMapFragment mapFragment = GoogleMapFragment.newInstance();
//                                mapFragment.showcaseMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.pin)).anchor(.5f, .5f));
//                                getSupportFragmentManager().beginTransaction().add(R.id.map_container, mapFragment).commit();
//                                mapFragment.getMapAsync(googleMap -> {
//                                    UiSettings settings = googleMap.getUiSettings();
//                                    settings.setMyLocationButtonEnabled(false);
//                                    settings.setZoomControlsEnabled(false);
//                                    googleMap.setOnCameraChangeListener(cameraPosition -> {
//                                        if (cameraPosition.zoom >= 20) {
//                                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cameraPosition.target, (float) 19.99));
//                                        }
//                                    });
//                                });
//                                //favoriteMenuItem.setVisible(false);
//                                //locationView.setText(String.format("%f, %f", latLng.latitude, latLng.longitude));
//                            } else {
//                                // Adjust the margin / padding show the heart icon doesn't
//                                // overlap title + descrition
//                                findViewById(R.id.map_container).setVisibility(View.GONE);
//                                //GoogleMapFragment mapFragment = (GoogleMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
//                                //getSupportFragmentManager().beginTransaction().remove(mapFragment).commit();
//                                collapsingToolbarLayout.setBackgroundResource(android.R.color.transparent);
//                                CollapsingToolbarLayout.LayoutParams parms = new CollapsingToolbarLayout.LayoutParams(CollapsingToolbarLayout.LayoutParams.MATCH_PARENT, 24);
//                                mapContainer.setLayoutParams(parms);
//                                favoriteButton.setVisibility(View.GONE);
//                                //favoriteMenuItem.setVisible(true);
//                            }
//
//                            switch (model_type) {
//                                case ART:
//                                    if (!itemCursor.isNull(itemCursor.getColumnIndex(ArtTable.playaAddress))) {
//                                        subItem1TextView.setText(itemCursor.getString(itemCursor.getColumnIndexOrThrow(ArtTable.playaAddress)));
//                                    } else {
//                                        subItem1TextView.setVisibility(View.GONE);
//                                    }
//
//                                    if (!itemCursor.isNull(itemCursor.getColumnIndex(ArtTable.artist))) {
//                                        subItem2TextView.setText(itemCursor.getString(itemCursor.getColumnIndexOrThrow(ArtTable.artist)));
//                                    } else {
//                                        subItem2TextView.setVisibility(View.GONE);
//                                    }
//
//                                    if (!itemCursor.isNull(itemCursor.getColumnIndex(ArtTable.artistLoc))) {
//                                        subItem3TextView.setText(itemCursor.getString(itemCursor.getColumnIndexOrThrow(ArtTable.artistLoc)));
//                                    } else {
//                                        subItem3TextView.setVisibility(View.GONE);
//                                    }
//
//                                    if (!itemCursor.isNull(itemCursor.getColumnIndexOrThrow(ArtTable.audioTourUrl))) {
//                                        audioTourUrl = itemCursor.getString(itemCursor.getColumnIndexOrThrow(ArtTable.audioTourUrl));
//
//                                        Timber.d("Add audio tour control view");
//                                        audioTourToggle = new TextView(this);
//                                        audioTourToggle.setTextColor(getResources().getColor(R.color.regular_text));
//                                        audioTourToggle.setTextSize(22);
//                                        audioTourToggle.setCompoundDrawablePadding(12);  // 8 dp
//                                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//                                        params.bottomMargin = 12; // 8 dp
//                                        audioTourToggle.setLayoutParams(params);
//                                        if (isAudioTourManagerPlayingThis()) {
//                                            audioTourToggle.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_pause_circle_outline_light_24dp), null, null, null);
//                                            audioTourToggle.setText(R.string.pause_audio_tour);
//                                        } else {
//                                            audioTourToggle.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_play_circle_outline_light_24dp), null, null, null);
//                                            audioTourToggle.setText(R.string.play_audio_tour);
//                                        }
//                                        audioTourToggle.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_play_circle_outline_light_24dp), null, null, null);
//                                        LinearLayout contentContainer = (LinearLayout) findViewById(R.id.content_container);
//                                        contentContainer.addView(audioTourToggle, 3);
//
//                                        audioTourToggle.setOnClickListener(view -> {
//                                            view.setSelected(!view.isSelected());
//                                            onAudioTourToggleSelectedChanged();
//
//                                            if (view.isSelected()) {
//                                                // Playing. Show Pause button
//                                                audioTourManager.playAudioTourUrl(audioTourUrl, title);
//                                            } else {
//                                                // Paused. Show Play button
//                                                audioTourManager.pause();
//                                            }
//                                        });
//                                    }
//
//                                    break;
//                                case CAMP:
//                                    if (!itemCursor.isNull(itemCursor.getColumnIndex(CampTable.playaAddress))) {
//                                        subItem1TextView.setText(itemCursor.getString(itemCursor.getColumnIndexOrThrow(CampTable.playaAddress)));
//                                    } else
//                                        subItem1TextView.setVisibility(View.GONE);
//
//                                    if (!itemCursor.isNull(itemCursor.getColumnIndex(CampTable.hometown))) {
//                                        subItem2TextView.setText(itemCursor.getString(itemCursor.getColumnIndexOrThrow(CampTable.hometown)));
//                                    } else
//                                        subItem2TextView.setVisibility(View.GONE);
//                                    subItem3TextView.setVisibility(View.GONE);
//                                    break;
//                                case EVENT:
//                                    if (!itemCursor.isNull(itemCursor.getColumnIndex(EventTable.playaAddress))) {
//                                        subItem1TextView.setText(itemCursor.getString(itemCursor.getColumnIndexOrThrow(EventTable.playaAddress)));
//                                    } else
//                                        subItem1TextView.setVisibility(View.GONE);
//
//                                    Date nowDate = CurrentDateProvider.getCurrentDate();
//                                    Calendar nowPlusOneHrDate = Calendar.getInstance();
//                                    nowPlusOneHrDate.setTime(nowDate);
//                                    nowPlusOneHrDate.add(Calendar.HOUR, 1);
//
//                                    subItem2TextView.setText(DateUtil.getDateString(this, nowDate, nowPlusOneHrDate.getTime(),
//                                            itemCursor.getString(itemCursor.getColumnIndexOrThrow(EventTable.startTime)),
//                                            itemCursor.getString(itemCursor.getColumnIndexOrThrow(EventTable.startTimePrint)),
//                                            itemCursor.getString(itemCursor.getColumnIndexOrThrow(EventTable.endTime)),
//                                            itemCursor.getString(itemCursor.getColumnIndexOrThrow(EventTable.endTimePrint))));
//                                    subItem3TextView.setVisibility(View.GONE);
//                                    break;
//                            }
//
//                            // Look up hosted events or other occurrences
//                            int playaId = itemCursor.getInt(itemCursor.getColumnIndex(PlayaItemTable.playaId));
//
//                            if (modelTable.equals(PlayaDatabase.CAMPS)) {
//                                // Lookup hosted events
//
//                                EventCursorAdapter adapter = new EventCursorAdapter(this, null, true, new AdapterListener() {
//                                    @Override
//                                    public void onItemSelected(int modelId, Constants.PlayaItemType type) {
//                                        Intent intent = new Intent(PlayaItemViewActivity.this, PlayaItemViewActivity.class);
//                                        intent.putExtra(PlayaItemViewActivity.EXTRA_MODEL_ID, modelId);
//                                        intent.putExtra(PlayaItemViewActivity.EXTRA_PLAYA_ITEM, type);
//                                        startActivity(intent);
//                                    }
//
//                                    @Override
//                                    public void onItemFavoriteButtonSelected(int modelId, Constants.PlayaItemType type) {
//                                        final String modelTable;
//                                        switch (type) {
//                                            case CAMP:
//                                                modelTable = PlayaDatabase.CAMPS;
//                                                break;
//                                            case ART:
//                                                modelTable = PlayaDatabase.ART;
//                                                break;
//                                            case EVENT:
//                                                modelTable = PlayaDatabase.EVENTS;
//                                                break;
//
//                                            default:
//                                                throw new IllegalArgumentException("Invalid type " + type);
//                                        }
//
//                                        provider.toggleFavorite(modelTable, modelId);
//                                    }
//                                });
//
//                                provider.createQuery(PlayaDatabase.EVENTS, "SELECT " + DataProvider.makeProjectionString(adapter.getRequiredProjection()) + " FROM " + PlayaDatabase.EVENTS + " WHERE " + EventTable.campPlayaId + " = ? GROUP BY " + PlayaItemTable.name, String.valueOf(playaId))
//                                        .map(SqlBrite.Query::run)
//                                        .subscribe(eventsCursor -> {
//
//                                            if (eventsCursor == null) return;
//
//                                            Timber.d("Got %d hosted events", eventsCursor.getCount());
//
//                                            if (eventsCursor.getCount() == 0) {
//                                                eventsCursor.close();
//                                                return;
//                                            }
//
//                                            int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
//
//                                            ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.PlayaTextItem);
//
//                                            TextView hostedEventsTitle = new TextView(wrapper);
//                                            hostedEventsTitle.setText(R.string.hosted_events);
//                                            hostedEventsTitle.setTextSize(32);
//                                            hostedEventsTitle.setPadding(pad, pad, pad, pad);
//
//                                            overflowContainer.removeAllViews();
//
//                                            overflowContainer.addView(hostedEventsTitle);
//
//                                            adapter.swapCursor(eventsCursor);
//
//                                            for (int idx = 0; idx < eventsCursor.getCount(); idx++) {
//                                                EventCursorAdapter.ViewHolder holder = adapter.createViewHolder(overflowContainer, 0);
//                                                adapter.bindViewHolder(holder, idx);
//                                                overflowContainer.addView(holder.itemView);
//                                            }
//                                            eventsCursor.close();
//                                        }, throwable -> Timber.e(throwable, "Failed to bind hosted events"));
//
//                            } else if (modelTable.equals(PlayaDatabase.EVENTS)) {
//                                // Lookup other event occurrences
//
//                                final ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.PlayaTextItem);
//                                final Typeface condensed = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
//                                int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
//
//                                if (!itemCursor.isNull(itemCursor.getColumnIndex(EventTable.campPlayaId))) {
//                                    final TextView hostedByCamp = new TextView(wrapper);
//                                    hostedByCamp.setTag(itemCursor.getInt(itemCursor.getColumnIndex(EventTable.campPlayaId)));
//                                    hostedByCamp.setTypeface(condensed);
//                                    hostedByCamp.setTextSize(32);
//                                    hostedByCamp.setPadding(pad, pad, pad, 0);
//
//                                    provider.createQuery(PlayaDatabase.CAMPS, "SELECT * FROM " + PlayaDatabase.CAMPS + " WHERE " + CampTable.playaId + " = ?", String.valueOf(itemCursor.getInt(itemCursor.getColumnIndex(EventTable.campPlayaId))))
//                                            .first()
//                                            .map(SqlBrite.Query::run)
//                                            .subscribe(campCursor -> {
//                                                if (campCursor != null && campCursor.moveToFirst()) {
//                                                    hostedByCamp.setOnClickListener(new RelatedItemOnClickListener(campCursor.getInt(campCursor.getColumnIndex(PlayaItemTable.id)), Constants.PlayaItemType.CAMP));
//                                                    String campName = campCursor.getString(campCursor.getColumnIndex(PlayaItemTable.name));
//                                                    hostedByCamp.setText("Hosted by " + campName);
//                                                    campCursor.close();
//                                                }
//                                            });
//                                    overflowContainer.addView(hostedByCamp);
//                                }
//
//                                provider.createQuery(PlayaDatabase.EVENTS, "SELECT * FROM " + PlayaDatabase.EVENTS + " WHERE " + EventTable.playaId + " = ? AND " + EventTable.startTime + " != ?", String.valueOf(playaId), itemCursor.getString(itemCursor.getColumnIndex(EventTable.startTime)))
//                                        .first()
//                                        .map(SqlBrite.Query::run)
//                                        .subscribe(eventsCursor -> {
//                                            if (eventsCursor == null) return;
//
//                                            Timber.d("Got %d other occurrences", eventsCursor.getCount());
//
//                                            if (eventsCursor.getCount() == 0) {
//                                                eventsCursor.close();
//                                                return;
//                                            }
//
//                                            TextView occurrencesTitle = new TextView(wrapper);
//                                            occurrencesTitle.setText(R.string.also_at);
//                                            occurrencesTitle.setTypeface(condensed);
//                                            occurrencesTitle.setTextSize(32);
//                                            occurrencesTitle.setPadding(pad,pad,pad,0);
//                                            overflowContainer.addView(occurrencesTitle);
//
//                                            final SimpleDateFormat timeDayFormatter = new SimpleDateFormat("EEEE, M/d 'at' h:mm a", Locale.US);
//
//                                            while (eventsCursor.moveToNext()) {
//                                                TextView event = new TextView(wrapper);
//                                                event.setTypeface(condensed);
//                                                event.setTextSize(20);
//                                                event.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
//                                                try {
//                                                    event.setText(timeDayFormatter.format(PlayaDateTypeAdapter.iso8601Format.parse(eventsCursor.getString(eventsCursor.getColumnIndex(EventTable.startTime)))));
//                                                } catch (ParseException e) {
//                                                    Timber.w(e, "Unable to parse date, using pre-computed");
//                                                    event.setText(eventsCursor.getString(eventsCursor.getColumnIndex(EventTable.startTimePrint)).toUpperCase());
//                                                }
//                                                event.setOnClickListener(new RelatedItemOnClickListener(eventsCursor.getInt(eventsCursor.getColumnIndex(PlayaItemTable.id)), Constants.PlayaItemType.EVENT));
//                                                event.setPadding(pad,pad,pad,pad);
//
//                                                TypedValue outValue = new TypedValue();
//                                                getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
//                                                event.setBackgroundResource(outValue.resourceId);
//                                                overflowContainer.addView(event);
//                                            }
//                                            eventsCursor.close();
//                                        }, throwable -> Timber.e(throwable, "Failed to bind event occurrences"));
//                            }
//                        }
//                    } finally {
//                        if (itemCursor != null) itemCursor.close();
//                    }
//                }, throwable -> Timber.e(throwable, "Failed to populate views from item"));
    }

    @Override
    public void onStopped() {
        if (isAudioTourManagerPlayingThis()) {
            audioTourToggle.setSelected(false);
            onAudioTourToggleSelectedChanged();
        }
    }

    @Override
    public void onPaused(Song song) {
        if (isAudioTourManagerPlayingThis()) {
            audioTourToggle.setSelected(false);
            onAudioTourToggleSelectedChanged();
        }
    }

    @Override
    public void onLoading(Song song) {
        // unused
    }

    @Override
    public void onPlaying(Song song, int i) {
        if (isAudioTourManagerPlayingThis()) {
            audioTourToggle.setSelected(true);
            onAudioTourToggleSelectedChanged();
        }
    }

    @Override
    public void onStreaming(Song song) {
        // unused
    }

    private boolean isAudioTourManagerPlayingThis() {
        return audioTourManager != null && audioTourManager.getCurrentAudioTourUrl() != null &&
                audioTourManager.getCurrentAudioTourUrl().equals(audioTourUrl);
    }

    private void onAudioTourToggleSelectedChanged() {
        if (audioTourToggle.isSelected()) {
            // Playing. Show Pause button
            audioTourToggle.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_pause_circle_outline_light_24dp), null, null, null);
            audioTourToggle.setText(R.string.pause_audio_tour);

        } else {
            // Paused. Show Play button
            audioTourToggle.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_play_circle_outline_light_24dp), null, null, null);
            audioTourToggle.setText(R.string.play_audio_tour);
        }
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
            i.putExtra(EXTRA_MODEL_ID, modelId);
            i.putExtra(EXTRA_MODEL_TYPE, modeltype);
            startActivity(i);
        }
    }

    View.OnClickListener favoriteButtonOnClickListener = (View v) -> {
        setFavorite(!isFavorite, true);
    };

    private void setFavorite(boolean isFavorite, boolean save) {
        if (modelTable == null || modelId == 0) {
            Timber.w("setFavorite called before model data ready. Ignoring");
            return;
        }

        int newMenuDrawableResId = isFavorite ? R.drawable.ic_heart_full : R.drawable.ic_heart_empty;

        favoriteButton.setSelectedState(isFavorite, save);
        if (favoriteMenuItem != null) favoriteMenuItem.setIcon(newMenuDrawableResId);

        if (save) {
            // TODO
//            DataProvider.getInstance(PlayaItemViewActivity.this.getApplicationContext())
//                    .subscribe(dataProvider -> dataProvider.toggleFavorite(item),
//                            throwable -> Timber.e(throwable, "Failed to save"));
            this.isFavorite = isFavorite;
        }
    }
}