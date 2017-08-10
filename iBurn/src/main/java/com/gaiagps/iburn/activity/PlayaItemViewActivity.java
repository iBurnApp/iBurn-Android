package com.gaiagps.iburn.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gaiagps.iburn.AudioTourManager;
import com.gaiagps.iburn.CurrentDateProvider;
import com.gaiagps.iburn.DateUtil;
import com.gaiagps.iburn.MapboxMapFragment;
import com.gaiagps.iburn.PrefsHelper;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.SchedulersKt;
import com.gaiagps.iburn.adapters.AdapterListener;
import com.gaiagps.iburn.adapters.PlayaItemAdapter;
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter;
import com.gaiagps.iburn.database.Art;
import com.gaiagps.iburn.database.Camp;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.Embargo;
import com.gaiagps.iburn.database.Event;
import com.gaiagps.iburn.database.PlayaItem;
import com.gaiagps.iburn.service.AudioPlayerService;
import com.gaiagps.iburn.view.AnimatedFloatingActionButton;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

import static com.gaiagps.iburn.service.AudioPlayerServiceKt.MediaMetadataKeyArtPlayaId;

/**
 * Show the detail view for a Camp, Art installation, or Event
 * Created by davidbrodsky on 8/11/13.
 */
public class PlayaItemViewActivity extends AppCompatActivity implements AdapterListener {

    public static final String EXTRA_PLAYA_ITEM = "playa-item";

    PlayaItem item;
    LatLng latLng;

    boolean isFavorite;
    boolean showingLocation;
    boolean showingArt;

    MenuItem favoriteMenuItem;
    MenuItem imageMenuItem;

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

    @BindView(R.id.body)
    TextView bodyTextView;

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

    @BindView(R.id.appbar)
    AppBarLayout appbarLayout;

    Disposable autoShowArtDisposable;
    boolean loadedArtImage;
    ImageView artImageView;

    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat.Callback mediaControllerCallback;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playa_item_view);
        ButterKnife.bind(this);
        didPopulateViews = false;

        Intent i = getIntent();
        item = (PlayaItem) i.getSerializableExtra(EXTRA_PLAYA_ITEM);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_on_orange_24dp);
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

        if (item instanceof Art && ((Art) item).hasAudioTour()) {
            onCreateMediaController();
        }

        appbarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {

            int collapsingTriggerHeight = collapsingToolbarLayout.getScrimVisibleHeightTrigger();
            int collapsingOffsetTrigger = -(collapsingToolbarLayout.getHeight() - collapsingTriggerHeight);
            if (verticalOffset <= collapsingOffsetTrigger) {
                // Collapsed
                if (showingLocation && showingArt && imageMenuItem != null && imageMenuItem.isVisible() && loadedArtImage) {
                    Timber.d("Setting imageMenu invisible on collapse");
                    imageMenuItem.setVisible(false);
                }
            } else {
                // Expanded
                if (showingLocation && showingArt && imageMenuItem != null && !imageMenuItem.isVisible() && loadedArtImage) {
                    Timber.d("Setting imageMenu visible on expand");
                    imageMenuItem.setVisible(true);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (item instanceof Art && ((Art) item).hasAudioTour()) {
            onStartMediaController();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (item instanceof Art && ((Art) item).hasAudioTour()) {
            onStopMediaController();
        }

        if (autoShowArtDisposable != null) {
            autoShowArtDisposable.dispose();
        }
    }

    private void onCreateMediaController() {
        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, AudioPlayerService.class),
                new PlayaItemViewMediaConnectionCallback(),
                null); // optional Bundle
    }

    private void onStartMediaController() {
        mediaBrowser.connect();
    }

    private void onStopMediaController() {
        if (MediaControllerCompat.getMediaController(PlayaItemViewActivity.this) != null) {
            MediaControllerCompat.getMediaController(PlayaItemViewActivity.this).unregisterCallback(mediaControllerCallback);
        }
        mediaBrowser.disconnect();
    }

    private class PlayaItemViewMediaConnectionCallback extends MediaBrowserCompat.ConnectionCallback {

        @Override
        public void onConnected() {
            Timber.d("Connected to media service");
            try {
                // Get the token for the MediaSession
                MediaSessionCompat.Token token = mediaBrowser.getSessionToken();

                // Create a MediaControllerCompat
                MediaControllerCompat mediaController =
                        new MediaControllerCompat(
                                PlayaItemViewActivity.this,
                                token);

                // Save the controller
                MediaControllerCompat.setMediaController(PlayaItemViewActivity.this, mediaController);

                // Finish building the UI
                setupMediaTransportControls();

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onConnectionFailed() {
            Timber.d("Connection to media service failed");
            // This means no current media session is active
            // Finish building the UI
//            setupMediaTransportControls();
        }
    }

    private class PlayaItemMediaControllerCallback extends MediaControllerCompat.Callback {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            // Selected corresponds to the playing state
            int playbackState = state.getState();
            Timber.d("onPlaybackStateChanged to %d", playbackState);
            MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(PlayaItemViewActivity.this);
            setAudioTourToggleStateWithPlaybackState(mediaController, item);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            Timber.d("onMetadataChanged to %s", metadata);
        }
    }

    private void setupMediaTransportControls() {
        audioTourUrl = ((Art) item).audioTourUrl;

        // Find or create Audio Tour Playback Toggle View
        ViewGroup contentContainer = findViewById(R.id.content_container);
        TextView audioTourToggle = contentContainer.findViewById(R.id.audio_tour_toggle);

        if (audioTourToggle == null) {
            audioTourToggle = new TextView(this);
            audioTourToggle.setId(R.id.audio_tour_toggle);
            audioTourToggle.setTextColor(getResources().getColor(R.color.regular_text));
            audioTourToggle.setTextSize(22);
            audioTourToggle.setCompoundDrawablePadding(12);  // 8 dp
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = 12; // 8 dp
            audioTourToggle.setLayoutParams(params);

            contentContainer.addView(audioTourToggle, 3);
        }

        this.audioTourToggle = audioTourToggle;

        // Get initial media state
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(PlayaItemViewActivity.this);
        MediaMetadataCompat metadata = mediaController.getMetadata();
        PlaybackStateCompat pbState = mediaController.getPlaybackState();

        audioTourToggle.setOnClickListener(view -> {

            MediaMetadataCompat currentMetadata = mediaController.getMetadata();
            int currentPbState = mediaController.getPlaybackState().getState();
            Timber.d("Audio tour toggle hit in state %d with metadata %s", currentPbState, currentMetadata);

            if (!isCurrentMediaSessionForItem(mediaController, item)) {

                Timber.d("Starting audio tour playback anew for item %s", item.name);
                // Need to start up the media service
                audioTourManager.playAudioTourUrl((Art) item);

            } else if (currentPbState == PlaybackStateCompat.STATE_PLAYING) {

                Timber.d("Resuming audio tour playback for item %s", item.name);
                MediaControllerCompat.getMediaController(PlayaItemViewActivity.this).getTransportControls().pause();

            } else if (currentPbState == PlaybackStateCompat.STATE_PAUSED) {

                Timber.d("Pausing audio tour playback for item %s", item.name);
                MediaControllerCompat.getMediaController(PlayaItemViewActivity.this).getTransportControls().play();

            } else {

                Timber.e("Unable to handle audio tour playback toggle. MediaController in unknown state %d", pbState);

            }
            setAudioTourToggleStateWithPlaybackState(mediaController, item);
        });

        int playbackState = pbState.getState();
        Timber.d("Initial MediaController state %d with metadata %s", playbackState, metadata);

        setAudioTourToggleStateWithPlaybackState(mediaController, item);

        // Register a Callback to stay in sync
        if (mediaControllerCallback == null) {
            mediaControllerCallback = new PlayaItemMediaControllerCallback();
        }
        mediaController.registerCallback(mediaControllerCallback);
    }

    private void setAudioTourToggleStateWithPlaybackState(MediaControllerCompat mediaControllerCompat, PlayaItem item) {
        if (item == null || mediaControllerCompat == null) return;

        int playbackState = mediaControllerCompat.getPlaybackState().getState();
        boolean audioTourToggleSelected = (playbackState == PlaybackStateCompat.STATE_PLAYING) &&
                isCurrentMediaSessionForItem(mediaControllerCompat, item);
        audioTourToggle.setSelected(audioTourToggleSelected);
        onAudioTourToggleSelectedChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        audioTourManager = new AudioTourManager(this);
        if (!didPopulateViews && item != null) {
            populateViews(item);
            didPopulateViews = true;
        }
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
        if (isFavorite) favoriteMenuItem.setIcon(R.drawable.ic_heart_full_white_24dp);
        if (showingLocation || showingArt) favoriteMenuItem.setVisible(false);

        imageMenuItem = menu.findItem(R.id.image_menu);
        if (!loadedArtImage || !showingLocation) {
            imageMenuItem.setVisible(false);
        } else {
            boolean isShowingImage = artImageView.getAlpha() == 1f;
            setImageMenuToggle(isShowingImage);
        }
        Timber.d("onCreateOptionsMenu image visible %b", imageMenuItem.isVisible());
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

            case R.id.image_menu:
                if (artImageView != null) {
                    boolean isVisible = artImageView.getVisibility() == View.VISIBLE && (artImageView.getAlpha() == 1f);
                    boolean willBeVisible = !isVisible;
                    if (willBeVisible) {
                        artImageView.bringToFront();
                    }
                    Timber.d("Fading %s art view", willBeVisible ? "in" : "out");
                    fadeView(artImageView, willBeVisible, null);

                    setImageMenuToggle(willBeVisible);

                    if (autoShowArtDisposable != null) {
                        autoShowArtDisposable.dispose();
                    }
                }
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void populateViews(PlayaItem item) {
        boolean embargoActive = Embargo.isEmbargoActive(new PrefsHelper(getApplicationContext()));
        showingLocation = item.hasLocation() && !embargoActive;

        if (showingLocation) {
            favoriteButton.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (favoriteMenuItem != null)
                    favoriteMenuItem.setVisible(v.getVisibility() == View.GONE);
            });
            latLng = item.getLatLng();
            //TextView locationView = ((TextView) findViewById(R.id.location));
            Timber.d("adding / centering marker on %f, %f", latLng.getLatitude(), latLng.getLongitude());

            MapboxMapFragment mapFragment = new MapboxMapFragment();
            mapFragment.showcaseLatLng(getApplicationContext(), latLng);
            getSupportFragmentManager().beginTransaction().add(R.id.map_container, mapFragment).commit();
            //favoriteMenuItem.setVisible(false);
            //locationView.setText(String.format("%f, %f", latLng.latitude, latLng.longitude));
        } else if (item instanceof Art && ((Art) item).hasImage()) {
            // Art image will be added by populateArtViews
            showingArt = true;
        } else {
            // Adjust the margin / padding show the heart icon doesn't
            // overlap title + descrition
            findViewById(R.id.map_container).setVisibility(View.GONE);
            collapsingToolbarLayout.setBackgroundResource(android.R.color.transparent);
            CollapsingToolbarLayout.LayoutParams parms = new CollapsingToolbarLayout.LayoutParams(CollapsingToolbarLayout.LayoutParams.MATCH_PARENT, 24);
            mapContainer.setLayoutParams(parms);
            favoriteButton.setVisibility(View.GONE);
            //favoriteMenuItem.setVisible(true);
        }

        titleTextView.setText(item.name);
        setFavorite(item.isFavorite, false);
        favoriteButton.setOnClickListener(favoriteButtonOnClickListener);

        setTextOrHideIfEmpty(item.description, bodyTextView);

        if (!embargoActive) {
            setTextOrHideIfEmpty(item.playaAddress, subItem1TextView);
        } else {
            subItem1TextView.setVisibility(View.GONE);
        }

        DataProvider.Companion.getInstance(getApplicationContext())
                .subscribe(provider -> {
                    if (item instanceof Art) {
                        populateArtViews((Art) item, provider);
                    } else if (item instanceof Camp) {
                        populateCampViews((Camp) item, provider);
                    } else if (item instanceof Event) {
                        populateEventViews((Event) item, provider);
                    }
                });
    }

    private void populateArtViews(Art art, DataProvider provider) {

        setTextOrHideIfEmpty(art.artist, subItem2TextView);
        setTextOrHideIfEmpty(art.artistLocation, subItem3TextView);

        if (art.hasImage()) {
            artImageView = new ImageView(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            artImageView.setLayoutParams(params);
            artImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            artImageView.setAlpha(.99f); // Hack - Can't seem to properly add view if it's visibility is not VISIBLE or alpha 0. This lets us know that the view isn't technically visible (it's not at the front)
            mapContainer.addView(artImageView);

            Picasso.with(this).load(art.imageUrl).into(artImageView, new Callback() {
                @Override
                public void onSuccess() {
                    loadedArtImage = true;
                    invalidateOptionsMenu();
                    Timber.d("Loaded image %s", art.imageUrl);

                    // If we're showing location, image will be under map. After a delay
                    // bring it to front and fade-in. Else, image will be visible here
                    if (showingLocation) {
                        autoShowArtDisposable = Observable.timer(7, TimeUnit.SECONDS)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(ignored -> {
                                    if (artImageView.getAlpha() == 1f) return;

                                    artImageView.bringToFront();
                                    fadeView(artImageView, true, new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            invalidateOptionsMenu();
                                        }
                                    });
                                });
                    }
                }

                @Override
                public void onError() {
                    // fuhgeddaboudit. Don't show image
                    Timber.e("Failed to load image");
                }
            });
            // TODO : Add Placeholder and error images
        }

        // Note : Audio Tour views are populated separately when connection to the
        // AudioPlaybackService is complete. See setupMediaTransportControls
    }

    private void populateCampViews(Camp camp, DataProvider provider) {
        setTextOrHideIfEmpty(camp.hometown, subItem2TextView);
        subItem3TextView.setVisibility(View.GONE);

        // Display hosted events
        PlayaItemAdapter adapter = new PlayaItemAdapter(getApplicationContext(), this);

        // This list will be updated if a favorite changes
        provider.observeEventsHostedByCamp(camp)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(events -> {
                    int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());


                    overflowContainer.removeAllViews();

                    if (events.size() > 0) {
                        ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.PlayaTextItem);
                        TextView hostedEventsTitle = new TextView(wrapper);
                        hostedEventsTitle.setText(R.string.hosted_events);
                        hostedEventsTitle.setTextSize(32);
                        hostedEventsTitle.setPadding(pad, pad, pad, pad);
                        overflowContainer.addView(hostedEventsTitle);
                    }

                    adapter.setItems(events);

                    for (int idx = 0; idx < events.size(); idx++) {
                        PlayaItemAdapter.ViewHolder holder = (PlayaItemAdapter.ViewHolder) adapter.createViewHolder(overflowContainer, 0);
                        adapter.bindViewHolder(holder, idx);
                        overflowContainer.addView(holder.itemView);
                    }
                });
    }

    private void populateEventViews(Event event, DataProvider provider) {

        Date nowDate = CurrentDateProvider.getCurrentDate();

        // Describe the event time with some smarts: "[Starts|Ends] [in|at] [20m|4:20p]"
        String dateDescription = DateUtil.getDateString(
                getApplicationContext(),
                nowDate,
                event.startTime,
                event.startTimePretty,
                event.endTime,
                event.endTimePretty);

        subItem2TextView.setText(dateDescription);
        subItem3TextView.setVisibility(View.GONE);

        // Display Hosted-By-Camp
        final ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.PlayaTextItem);
        final Typeface condensed = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());

        if (event.hasCampHost()) {
            final TextView hostedByCamp = new TextView(wrapper);
            hostedByCamp.setTag(event.campPlayaId);
            hostedByCamp.setTypeface(condensed);
            hostedByCamp.setTextSize(32);
            hostedByCamp.setPadding(pad, pad, pad, 0);

            provider.observeCampByPlayaId(event.campPlayaId)
                    .firstElement()
                    .subscribe(camp -> {
                        hostedByCamp.setOnClickListener(new RelatedItemOnClickListener(camp));
                        String campName = camp.name;
                        hostedByCamp.setText("Hosted by " + campName);
                        overflowContainer.addView(hostedByCamp);
                    });

        }

        // Display other event occurrences
        provider.observeOtherOccurrencesOfEvent(event)
                .firstElement()
                .subscribe(eventOccurrences -> {

                    Timber.d("Got %d other event occurrences", eventOccurrences.size());
                    if (eventOccurrences.size() > 0) {
                        TextView occurrencesTitle = new TextView(wrapper);
                        occurrencesTitle.setText(R.string.also_at);
                        occurrencesTitle.setTypeface(condensed);
                        occurrencesTitle.setTextSize(32);
                        occurrencesTitle.setPadding(pad, pad, pad, 0);
                        overflowContainer.addView(occurrencesTitle);
                    }

                    final SimpleDateFormat timeDayFormatter = new SimpleDateFormat("EEEE, M/d 'at' h:mm a", Locale.US);

                    for (Event occurrence : eventOccurrences) {
                        TextView eventTv = new TextView(wrapper);
                        eventTv.setTypeface(condensed);
                        eventTv.setTextSize(20);
                        eventTv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        try {
                            eventTv.setText(timeDayFormatter.format(PlayaDateTypeAdapter.iso8601Format.parse(occurrence.startTime)));
                        } catch (ParseException e) {
                            Timber.w(e, "Unable to parse date, using pre-computed");
                            eventTv.setText(event.startTimePretty.toUpperCase());
                        }
                        eventTv.setOnClickListener(new RelatedItemOnClickListener(occurrence));
                        eventTv.setPadding(pad, pad, pad, pad);

                        TypedValue outValue = new TypedValue();
                        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                        eventTv.setBackgroundResource(outValue.resourceId);
                        overflowContainer.addView(eventTv);
                    }
                });
    }

    private void setTextOrHideIfEmpty(String text, TextView view) {
        if (!TextUtils.isEmpty(text)) {
            view.setText(text);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private boolean isCurrentMediaSessionForItem(MediaControllerCompat mediaControllerCompat,
                                                 PlayaItem item) {
        if (mediaControllerCompat == null) return false;

        MediaMetadataCompat metadata = mediaControllerCompat.getMetadata();
        if (metadata == null) return false;

        String currentlyPlayingArtPlayaId = metadata.getString(MediaMetadataKeyArtPlayaId);

        return (currentlyPlayingArtPlayaId != null && currentlyPlayingArtPlayaId.equals(item.playaId));
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

    @Override
    public void onItemSelected(PlayaItem item) {
        Intent intent = new Intent(PlayaItemViewActivity.this, PlayaItemViewActivity.class);
        intent.putExtra(PlayaItemViewActivity.EXTRA_PLAYA_ITEM, item);
        startActivity(intent);
    }

    @Override
    public void onItemFavoriteButtonSelected(PlayaItem item) {
        DataProvider.Companion.getInstance(getApplicationContext())
                .observeOn(SchedulersKt.getIoScheduler())
                .subscribe(provider -> provider.toggleFavorite(item));
    }

    class RelatedItemOnClickListener implements View.OnClickListener {

        PlayaItem item;

        public RelatedItemOnClickListener(PlayaItem item) {
            this.item = item;
        }

        @Override
        public void onClick(View v) {
            Intent i = new Intent(PlayaItemViewActivity.this, PlayaItemViewActivity.class);
            i.putExtra(EXTRA_PLAYA_ITEM, item);
            startActivity(i);
        }

    }

    View.OnClickListener favoriteButtonOnClickListener = (View v) -> {
        setFavorite(!isFavorite, true);
    };

    private void setFavorite(boolean isFavorite, boolean save) {
        if (item == null) {
            Timber.w("setFavorite called before model data ready. Ignoring");
            return;
        }

        int newMenuDrawableResId = isFavorite ? R.drawable.ic_heart_full_white_24dp : R.drawable.ic_heart_empty_white_24dp;

        favoriteButton.setSelectedState(isFavorite, save);
        if (favoriteMenuItem != null) favoriteMenuItem.setIcon(newMenuDrawableResId);
        if (save) {
            DataProvider.Companion.getInstance(getApplicationContext())
                    .observeOn(SchedulersKt.getIoScheduler())
                    .subscribe(dataProvider -> dataProvider.toggleFavorite(item),
                            throwable -> Timber.e(throwable, "Failed to save"));
            this.isFavorite = isFavorite;
        }
    }

    private void setImageMenuToggle(boolean isShowingImage) {
        if (isShowingImage) {
            imageMenuItem.setIcon(R.drawable.ic_map_white_on_orange_24dp);
        } else {
            imageMenuItem.setIcon(R.drawable.ic_image_white_on_orange_24dp);
        }
    }

    private void fadeView(View view, boolean fadeIn, AnimatorListenerAdapter listener) {
        float startAlpha = fadeIn ? 0 : 1;
        float stopAlpha = 1 - startAlpha;

        ValueAnimator fade = ValueAnimator.ofFloat(startAlpha, stopAlpha);
        fade.addUpdateListener(valueAnimator ->
                view.setAlpha((Float) valueAnimator.getAnimatedValue()));
        fade.setDuration(1000);
        if (listener != null) {
            fade.addListener(listener);
        }
        fade.start();
    }
}