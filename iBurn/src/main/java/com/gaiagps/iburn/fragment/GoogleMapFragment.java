package com.gaiagps.iburn.fragment;

import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.Geo;
import com.gaiagps.iburn.PrefsHelper;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.Searchable;
import com.gaiagps.iburn.activity.PlayaItemViewActivity;
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter;
import com.gaiagps.iburn.database.ArtTable;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.Embargo;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.MapProvider;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.database.UserPoiTable;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.squareup.sqlbrite.SqlBrite;

import java.util.ArrayDeque;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Created by davidbrodsky on 8/3/13.
 * <p>
 * TODO : Instead of clearing, we could set alpha0, never have to re-create markers?
 */
public class GoogleMapFragment extends SupportMapFragment implements Searchable {

    /**
     * Geographic Bounds of Black Rock City
     * Used to determining whether a location lies
     * within the general vicinity
     */
    public static final double MAX_LAT = 40.812161;
    public static final double MAX_LON = -119.170061;
    public static final double MIN_LAT = 40.764702;
    public static final double MIN_LON = -119.247798;

    public static LatLngBounds BRC_BOUNDS = LatLngBounds.builder()
            .include(new LatLng(MAX_LAT, MIN_LON))
            .include(new LatLng(MIN_LAT, MAX_LON))
            .build();

    @Override
    public void onSearchQueryRequested(String query) {
        // Think the new way to do this is simply provide a cursor to
        mCurFilter = query;
        if (TextUtils.isEmpty(query)) {
            if (areMarkersVisible()) clearMap();
            mState = STATE.EXPLORE;
            //if (lastZoomLevel > POI_ZOOM_LEVEL) restartLoaders(true);

        } else {
            mState = STATE.SEARCH;
            // TODO : Do we monitor query or just take first result?
            // TODO : Do we want to merge search queries into the cameraUpdate subscription in initMap?
            DataProvider.getInstance(getActivity())
                    .flatMap(dataProvider -> dataProvider.observeNameQuery(query, PROJECTION))
                    .map(SqlBrite.Query::run)
                    .subscribe(this::processMapItemResult);
        }
    }

    private enum STATE {
        /**
         * Default. Constantly search and show POIs within the viewable map region
         */
        EXPLORE,
        /**
         * Showcase a particular POI and its relation to the user home camp / location
         */
        SHOWCASE,
        /**
         * Show search results
         **/
        SEARCH
    }

    private STATE mState = STATE.EXPLORE;

    // Loader ids
    final int ART = 1;
    final int CAMPS = 2;
    final int EVENTS = 3;
    final int POIS = 4;
    final int ALL = 5;

    private final int POI_ZOOM_LEVEL = 18;
    float lastZoomLevel = 0;

    private final PublishSubject<VisibleRegion> cameraUpdate = PublishSubject.create();

    /**
     * Map of user added pins. Google Marker Id -> Database Id
     */
    HashMap<String, String> mMappedCustomMarkerIds = new HashMap<>();
    /**
     * Map of pins shown in response to explore or search
     */
    private static final int MAX_POIS = 200;
    ArrayDeque<Marker> mMappedMarkers = new ArrayDeque<>(MAX_POIS);
    HashMap<String, String> markerIdToMeta = new HashMap<>();
    public static MapBoxOfflineTileProvider tileProvider; // Re-use tileProvider
    private static AtomicInteger tileProviderHolds = new AtomicInteger();
    TileOverlay overlay;
    LatLng latLngToCenterOn;

    VisibleRegion visibleRegion;
    String mCurFilter;                      // Search string to filter by
    boolean limitListToFavorites = false;   // Limit display to favorites?
    PrefsHelper prefs;

    private View.OnClickListener mOnAddPinBtnListener = v -> {
        Marker marker = addCustomPin(null, null, UserPoiTable.STAR);
        dropPinAndShowEditDialog(marker);
    };

    private void dropPinAndShowEditDialog(final Marker marker) {
        ValueAnimator animator = new ValueAnimator();
        animator.addUpdateListener(animation -> {
            marker.setAlpha((Float) animation.getAnimatedValue());
            if (animation.getAnimatedFraction() == 1)
                showEditPinDialog(marker);
        });
        animator.setFloatValues(0f, 1f);
        animator.setDuration(500);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.start();
    }

    private void showEditPinDialog(final Marker marker) {
        View dialogBody = getActivity().getLayoutInflater().inflate(R.layout.dialog_poi, null);
        final RadioGroup iconGroup = ((RadioGroup) dialogBody.findViewById(R.id.iconGroup));
        // Fetch current Marker icon
        DataProvider.getInstance(getActivity())
                .flatMap(dataProvider ->
                        dataProvider.observeAllTables(PlayaItemTable.id + " = " + String.valueOf(getDatabaseIdFromGeneratedDataId(mMappedCustomMarkerIds.get(marker.getId()))), new String[]{PlayaItemTable.id, UserPoiTable.drawableResId}))
                .map(SqlBrite.Query::run)
                .subscribe(poi -> {
                    if (poi != null && poi.moveToFirst()) {
                        int drawableResId = poi.getInt(poi.getColumnIndex(UserPoiTable.drawableResId));
                        switch (drawableResId) {
                            case UserPoiTable.STAR:
                                ((RadioButton) iconGroup.findViewById(R.id.btn_star)).setChecked(true);
                                break;
                            case UserPoiTable.HEART:
                                ((RadioButton) iconGroup.findViewById(R.id.btn_heart)).setChecked(true);
                                break;
                            case UserPoiTable.HOME:
                                ((RadioButton) iconGroup.findViewById(R.id.btn_home)).setChecked(true);
                                break;
                            case UserPoiTable.BIKE:
                                ((RadioButton) iconGroup.findViewById(R.id.btn_bike)).setChecked(true);
                                break;
                            default:
                                Timber.e("Unknown custom marker type");
                        }
                        poi.close();
                    }
                    final EditText markerTitle = (EditText) dialogBody.findViewById(R.id.markerTitle);
                    markerTitle.setText(marker.getTitle());
                    markerTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {

                        String lastEntry;

                        @DebugLog
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            if (hasFocus) {
                                lastEntry = ((EditText) v).getText().toString();
                                ((EditText) v).setText("");
                            } else if (((EditText) v).getText().length() == 0) {
                                ((EditText) v).setText(lastEntry);
                            }
                        }
                    });
                    new AlertDialog.Builder(getActivity(), R.style.Theme_Iburn_Dialog)
                            .setView(dialogBody)
                            .setPositiveButton("Done", (dialog, which) -> {
                                // Save the title
                                if (markerTitle.getText().length() > 0)
                                    marker.setTitle(markerTitle.getText().toString());
                                marker.hideInfoWindow();

                                int drawableId = 0;
                                switch (iconGroup.getCheckedRadioButtonId()) {
                                    case R.id.btn_star:
                                        drawableId = UserPoiTable.STAR;
                                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.puck_star));
                                        break;
                                    case R.id.btn_heart:
                                        drawableId = UserPoiTable.HEART;
                                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.puck_heart));
                                        break;
                                    case R.id.btn_home:
                                        drawableId = UserPoiTable.HOME;
                                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.puck_home));
                                        break;
                                    case R.id.btn_bike:
                                        drawableId = UserPoiTable.BIKE;
                                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.puck_bicycle));
                                        break;
                                }
                                updateCustomPinWithMarker(marker, drawableId);
                            })
                            .setNegativeButton("Delete", (dialog, which) -> {
                                // Delete Pin
                                removeCustomPin(marker);
                            }).show();
                });
    }

    public static GoogleMapFragment newInstance() {
        return new GoogleMapFragment();
    }

    public GoogleMapFragment() {
        super();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (tileProvider != null && tileProviderHolds.decrementAndGet() == 0) {
            tileProvider.close();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View parent = super.onCreateView(inflater, container, savedInstanceState);
        ImageButton addPoiBtn = (ImageButton) inflater.inflate(R.layout.add_poi_map_btn, container, false);
        addPoiBtn.setOnClickListener(mOnAddPinBtnListener);
        ((ViewGroup) parent).addView(addPoiBtn);
        int dpValue = 10; // margin in dips
        float d = getActivity().getResources().getDisplayMetrics().density;
        int margin = (int) (dpValue * d); // margin in pixels
        setMargins(addPoiBtn, 0, margin * 6, margin, 0);
        return parent;
    }

    /**
     * Thanks to SO:
     * http://stackoverflow.com/questions/4472429/change-the-right-margin-of-a-view-programmatically
     */
    public static void setMargins(View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(l, t, r, b);
            if (p instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) p).gravity = Gravity.TOP | Gravity.RIGHT;
            }
            v.requestLayout();
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initMap();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        latLngToCenterOn = null;
    }

    private void initMap() {

        prefs = new PrefsHelper(getActivity());

        MapProvider.getInstance(getActivity())
                .getMapDatabase()
                .doOnNext(databaseFile -> {
                    Timber.d("Got database file %s", databaseFile.getAbsolutePath());
                    if (tileProvider == null)
                        tileProvider = new MapBoxOfflineTileProvider(databaseFile);
                    else
                        tileProvider.swapDatabase(databaseFile);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(databaseFile -> _addMBTilesOverlay());

        // TODO : Do full query. Don't run separate POIS, results queries
        Observable.combineLatest(cameraUpdate.debounce(250, TimeUnit.MILLISECONDS),
                DataProvider.getInstance(getActivity()), (newVisibleRegion, dataProvider) -> {
                        GoogleMapFragment.this.visibleRegion = newVisibleRegion;
                        return dataProvider;
                })
                // Don't bother running query during embargo because ours has no meaning without location data
                .skipWhile(dataProvider -> Embargo.isEmbargoActive(prefs))
                .flatMap(this::performQuery)
                .map(SqlBrite.Query::run)
                        // Can we do this on bg thread? prolly not
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::processMapItemResult, throwable -> Timber.e(throwable, "Error querying"));

        UiSettings settings = getMap().getUiSettings();
        settings.setZoomControlsEnabled(false);
        settings.setMapToolbarEnabled(false);
        settings.setScrollGesturesEnabled(mState != STATE.SHOWCASE);

        getMapAsync(googleMap -> {
            // TODO: If user location present, start there
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Geo.MAN_LAT, Geo.MAN_LON), 14));
            googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {
                    // do nothing
                }

                @Override
                public void onMarkerDrag(Marker marker) {
                    // do nothing
                }

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    if (mMappedCustomMarkerIds.containsKey(marker.getId())) {
                        updateCustomPinWithMarker(marker, 0);
                    }
                }
            });

            googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                /** Lat, Lon tolerance used to determine if location within BRC boundaries */
                private final double BUFFER = .00005;

                /** Map zoom limits */
                private final double MAX_ZOOM = 19.5;
                private final double MIN_ZOOM = 12;

                /** POI Search throttling */
                private final int CAMERA_MOVE_REACT_THRESHOLD_MS = 500;
                private long lastCallMs = Long.MIN_VALUE;

                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    if (!BRC_BOUNDS.contains(cameraPosition.target) ||
                            cameraPosition.zoom > MAX_ZOOM || cameraPosition.zoom < MIN_ZOOM) {
                        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(
                                        Math.min(MAX_LAT - BUFFER, Math.max(cameraPosition.target.latitude, MIN_LAT + BUFFER)),
                                        Math.min(MAX_LON - BUFFER, Math.max(cameraPosition.target.longitude, MIN_LON + BUFFER))),
                                (float) Math.min(Math.max(cameraPosition.zoom, MIN_ZOOM), MAX_ZOOM)));
                    } else {
                        // Map view bounds valid. Load POIs if necessary
                        if (cameraPosition.zoom > POI_ZOOM_LEVEL) {
                            if (mState == STATE.EXPLORE) {
                                visibleRegion = getMap().getProjection().getVisibleRegion();
                                // Don't bother restartingLoader more than THRESHOLD_MS
                                cameraUpdate.onNext(visibleRegion);
                            }
                        } else if (cameraPosition.zoom < POI_ZOOM_LEVEL && areMarkersVisible()) {
                            if (mState == STATE.EXPLORE) {
                                markerIdToMeta = new HashMap<>();
                                clearMap();
                            }
                        }
                    }
                }
            });

            googleMap.setOnInfoWindowClickListener(marker -> {
                if (markerIdToMeta.containsKey(marker.getId())) {
                    String markerMeta = markerIdToMeta.get(marker.getId());
                    int model_id = Integer.parseInt(markerMeta.split("-")[1]);
                    int model_type = Integer.parseInt(markerMeta.split("-")[0]);
                    Constants.PlayaItemType modelType = null;
                    switch (model_type) {
                        case ART:
                            modelType = Constants.PlayaItemType.ART;
                            break;
                        case EVENTS:
                            modelType = Constants.PlayaItemType.EVENT;
                            break;
                        case CAMPS:
                            modelType = Constants.PlayaItemType.CAMP;
                            break;
                    }
                    Intent i = new Intent(getActivity(), PlayaItemViewActivity.class);
                    i.putExtra("model_id", model_id);
                    i.putExtra("model_type", modelType);
                    getActivity().startActivity(i);
                } else if (mMappedCustomMarkerIds.containsKey(marker.getId())) {
                    showEditPinDialog(marker);
                }
            });
        });
    }

    /**
     * Add {@link #tileProvider} to the current Map and increment the num of tileProvider holds
     * Must be called from Main thread
     */
    private void _addMBTilesOverlay() {
        getMapAsync(map -> {
            tileProviderHolds.incrementAndGet();
            map.setMapType(GoogleMap.MAP_TYPE_NONE);
            map.setMyLocationEnabled(true);
            TileOverlayOptions opts = new TileOverlayOptions();
            opts.tileProvider(tileProvider);
            overlay = map.addTileOverlay(opts);
        });
    }

    public boolean areMarkersVisible() {
        return mMappedMarkers.size() > 0;
    }

    public void clearMap() {
        for (Marker marker : mMappedMarkers) {
            marker.remove();
        }
        mMappedMarkers.clear();
        markerIdToMeta.clear();
    }

    public void mapAndCenterOnMarker(final MarkerOptions marker) {
        latLngToCenterOn = marker.getPosition();
        getMapAsync(googleMap -> {
            googleMap.addMarker(marker);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngToCenterOn, 14));
        });
    }

    public void showcaseMarker(MarkerOptions marker) {
        mState = STATE.SHOWCASE;
        mapAndCenterOnMarker(marker);
        ImageButton poiBtn = (ImageButton) getActivity().findViewById(R.id.mapPoiBtn);
        if (poiBtn != null) {
            poiBtn.setVisibility(View.GONE);
        }

    }

    public void enableExploreState() {
        mState = STATE.EXPLORE;
    }

    static final String[] PROJECTION = new String[]{
            PlayaItemTable.name,
            PlayaItemTable.id,
            PlayaItemTable.latitude,
            PlayaItemTable.longitude,
            PlayaItemTable.favorite
    };

    static final String PROJECTION_STRING = DataProvider.makeProjectionString(PROJECTION);

    static String geoWhereClause = String.format(" WHERE (%s < ? AND %s > ?) AND (%s < ? AND %s > ?)",
            PlayaItemTable.latitude, PlayaItemTable.latitude,
            PlayaItemTable.longitude, PlayaItemTable.longitude);

    static String ongoingWhereClause = String.format("(%s < ? AND %s > ?) ",
            EventTable.startTime, EventTable.endTime);

    static String[] sqlParemeters = new String[14];

    public Observable<SqlBrite.Query> performQuery(DataProvider provider) {

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT ").append(PROJECTION_STRING).append(" FROM ").append(PlayaDatabase.POIS)
                .append(" UNION ")
                .append("SELECT ").append(PROJECTION_STRING).append(" FROM ").append(PlayaDatabase.EVENTS)
                .append(ongoingWhereClause)
                .append(" AND ")
                .append(geoWhereClause)
                .append(" UNION ")
                .append("SELECT ").append(PROJECTION_STRING).append(" FROM ").append(PlayaDatabase.ART)
                .append(geoWhereClause)
                .append(" UNION ")
                .append("SELECT ").append(PROJECTION_STRING).append(" FROM ").append(PlayaDatabase.CAMPS)
                .append(geoWhereClause);

        sqlParemeters[0] = sqlParemeters[1] = PlayaDateTypeAdapter.iso8601Format.format(new Date());

        sqlParemeters[2] = sqlParemeters[6] = sqlParemeters[10] = String.valueOf(visibleRegion.farLeft.latitude);
        sqlParemeters[3] = sqlParemeters[7] = sqlParemeters[11] = String.valueOf(visibleRegion.nearRight.latitude);
        sqlParemeters[4] = sqlParemeters[8] = sqlParemeters[12] = String.valueOf(visibleRegion.nearRight.longitude);
        sqlParemeters[5] = sqlParemeters[9] = sqlParemeters[13] = String.valueOf(visibleRegion.farLeft.longitude);

        return provider.createQuery(PlayaDatabase.ALL_TABLES, sql.toString(), sqlParemeters);
    }

    /**
     * Keep track of the bounds describing a batch of results across Loaders
     */
    private LatLngBounds.Builder mResultBounds;

    private void processMapItemResult(Cursor cursor) {

        mResultBounds = new LatLngBounds.Builder();

        Timber.d("Loader finished for id with items " + cursor.getCount());
        GoogleMap map = getMap();
        if (map == null) return;
        String markerMapId;
        // Sorry, but Java has no immutable primitives and LatLngBounds has no indicator
        // of when calling .build() will throw IllegalStateException due to including no points
        boolean[] areBoundsValid = new boolean[1];
        while (cursor.moveToNext()) {
            int typeInt = cursor.getInt(cursor.getColumnIndex(DataProvider.VirtualType));
            Constants.PlayaItemType type = DataProvider.getTypeValue(typeInt);

            markerMapId = generateDataIdForItem(typeInt, cursor.getInt(cursor.getColumnIndex(PlayaItemTable.id)));

            switch (type) {
                case POI:
                    if (!mMappedCustomMarkerIds.containsValue(markerMapId)) {
                        Marker marker = addNewMarkerForCursorItem(typeInt, cursor);
                        mMappedCustomMarkerIds.put(marker.getId(), markerMapId);
                    }
                    break;

                default:
                    mapRecyclableMarker(typeInt, markerMapId, cursor, mResultBounds, areBoundsValid);
                    break;
            }
        }
        if (areBoundsValid[0] && mState == STATE.SEARCH) {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(mResultBounds.build(), 80));
        } else if (!areBoundsValid[0] && mState == STATE.SEARCH) {
            // No results
            Timber.d("Resetting map view");
            resetMapView();
        }
    }

    /**
     * Return a key used internally to keep track of data items currently mapped,
     * helping us avoid mapping duplicate points.
     *
     * @param loaderId The id of the loader
     * @param itemId   The database id of the item
     */
    private String generateDataIdForItem(int loaderId, long itemId) {
        return String.format("%d-%d", loaderId, itemId);
    }

    /**
     * Return the internal database id for an item given the string id
     * generated by {@link #generateDataIdForItem(int, long)}
     */
    private int getDatabaseIdFromGeneratedDataId(String dataId) {
        return Integer.parseInt(dataId.split("-")[1]);
    }

    /**
     * Map a marker as part of a finite set of markers, limiting the total markers
     * displayed and recycling markers if this limit is exceeded.
     *
     * @param areBoundsValid a hack one-dimensional boolean array used to report whether boundsBuilder
     *                       includes at least one point and will not throw an exception on its build()
     */
    private void mapRecyclableMarker(int loaderId, String markerMapId, Cursor cursor, LatLngBounds.Builder boundsBuilder, boolean[] areBoundsValid) {
        if (!markerIdToMeta.containsValue(markerMapId)) {
            // This POI is not yet mapped
            LatLng pos = new LatLng(cursor.getDouble(cursor.getColumnIndex(ArtTable.latitude)), cursor.getDouble(cursor.getColumnIndex(ArtTable.longitude)));
            if (loaderId != POIS && boundsBuilder != null && mState == STATE.SEARCH) {
                if (BRC_BOUNDS.contains(pos)) {
                    boundsBuilder.include(pos);
                    areBoundsValid[0] = true;
                }
            }
            if (mMappedMarkers.size() == MAX_POIS) {
                // We should re-use the eldest Marker
                Marker marker = mMappedMarkers.remove();
                marker.setPosition(pos);
                marker.setTitle(cursor.getString(cursor.getColumnIndex(ArtTable.name)));

                switch (loaderId) {
                    case ALL:
                        if (cursor.getFloat(cursor.getColumnIndex("art.latitude")) != 0) {
                            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.pin));
                        } else if (cursor.getFloat(cursor.getColumnIndex("camps.latitude")) != 0) {
                            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.pin));
                        } else {
                            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.pin));
                        }
                        break;
                    case ART:
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.pin));
                        break;
                    case CAMPS:
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.pin));
                        break;
                    case EVENTS:
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.pin));
                        break;
                }

                marker.setAnchor(0.5f, 0.5f);
                mMappedMarkers.add(marker);
                markerIdToMeta.put(marker.getId(), String.format("%d-%d", loaderId, cursor.getInt(cursor.getColumnIndex(PlayaItemTable.id))));
            } else {
                // We shall create a new Marker
                Marker marker = addNewMarkerForCursorItem(loaderId, cursor);
                markerIdToMeta.put(marker.getId(), String.format("%d-%d", loaderId, cursor.getInt(cursor.getColumnIndex(PlayaItemTable.id))));
                mMappedMarkers.add(marker);
            }
        }
    }

    private Marker addNewMarkerForCursorItem(int loaderId, Cursor cursor) {
        LatLng pos = new LatLng(cursor.getDouble(cursor.getColumnIndex(PlayaItemTable.latitude)),
                cursor.getDouble(cursor.getColumnIndex(PlayaItemTable.longitude)));
        MarkerOptions markerOptions;
        markerOptions = new MarkerOptions().position(pos)
                .title(cursor.getString(cursor.getColumnIndex(PlayaItemTable.name)));

        switch (loaderId) {
            case POIS:
                styleCustomMarkerOption(markerOptions, cursor.getInt(cursor.getColumnIndex(UserPoiTable.drawableResId)));
//                Log.i(TAG, "Loading POI pin with drawable: " + cursor.getInt(cursor.getColumnIndex(UserPoiTable.drawableResId)));
                break;
            case ALL:
                if (cursor.getFloat(cursor.getColumnIndex("art.latitude")) != 0) {
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.art_pin));
                } else if (cursor.getFloat(cursor.getColumnIndex("camps.latitude")) != 0) {
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.camp_pin));
                } else {
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.event_pin));
                }
                break;
            case ART:
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.art_pin));
                break;
            case CAMPS:
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.camp_pin));
                break;
            case EVENTS:
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.event_pin));
                break;
        }

        markerOptions.anchor(0.5f, 0.5f);
        Marker marker = getMap().addMarker(markerOptions);
        return marker;
    }

    private void removeCustomPin(Marker marker) {
        marker.remove();
        if (mMappedCustomMarkerIds.containsKey(marker.getId())) {
            int itemId = getDatabaseIdFromGeneratedDataId(mMappedCustomMarkerIds.get(marker.getId()));
            DataProvider.getInstance(getActivity())
                    .map(provider -> provider.delete(PlayaDatabase.POIS, PlayaItemTable.id + " = ?", String.valueOf(itemId)))
                    .subscribe(result -> Timber.d("Deleted marker with result " + result));
        } else Timber.w("Unable to delete marker " + marker.getTitle());
    }

    /**
     * Adds a custom pin to the current map and database
     */
    private Marker addCustomPin(LatLng latLng, String title, int drawableResId) {
        if (latLng == null) {
            LatLng mapCenter = getMap().getCameraPosition().target;
            latLng = new LatLng(mapCenter.latitude, mapCenter.longitude);
        }
        if (title == null)
            title = getActivity().getString(R.string.default_custom_pin_title);

        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title)
                .anchor(0.5f, 0.5f);

        styleCustomMarkerOption(markerOptions, drawableResId);

        Marker marker = getMap().addMarker(markerOptions);
        ContentValues poiValues = new ContentValues();
        poiValues.put(UserPoiTable.name, title);
        poiValues.put(UserPoiTable.latitude, latLng.latitude);
        poiValues.put(UserPoiTable.longitude, latLng.longitude);
        poiValues.put(UserPoiTable.drawableResId, drawableResId);
        try {
            DataProvider.getInstance(getActivity())
                    .map(dataProvider -> dataProvider.insert(PlayaDatabase.POIS, poiValues))
                    .subscribe(newId -> mMappedCustomMarkerIds.put(marker.getId(), generateDataIdForItem(POIS, newId)));
        } catch (NumberFormatException e) {
            Timber.w("Unable to get id for new custom marker");
        }

        return marker;
    }

    /**
     * Apply style to a custom MarkerOptions before
     * adding to Map
     * <p>
     * Note: drawableResId is an int constant from {@link com.gaiagps.iburn.database.UserPoiTable}
     */
    private void styleCustomMarkerOption(MarkerOptions markerOption, int drawableResId) {
        markerOption
                .draggable(true)
                .flat(true);
        switch (drawableResId) {
            case UserPoiTable.HOME:
                markerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.puck_home));
                break;
            case UserPoiTable.STAR:
                markerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.puck_star));
                break;
            case UserPoiTable.BIKE:
                markerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.puck_bicycle));
                break;
            case UserPoiTable.HEART:
                markerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.puck_heart));
                break;
        }
    }

    /**
     * Update a Custom pin placed by a user with state of a map marker.
     * <p>
     * Note: If drawableResId is 0, it is ignored
     */
    private void updateCustomPinWithMarker(Marker marker, int drawableResId) {
        if (mMappedCustomMarkerIds.containsKey(marker.getId())) {
            ContentValues poiValues = new ContentValues();
            poiValues.put(UserPoiTable.name, marker.getTitle());
            poiValues.put(UserPoiTable.latitude, marker.getPosition().latitude);
            poiValues.put(UserPoiTable.longitude, marker.getPosition().longitude);
            if (drawableResId != 0)
                poiValues.put(UserPoiTable.drawableResId, drawableResId);
            int itemId = getDatabaseIdFromGeneratedDataId(mMappedCustomMarkerIds.get(marker.getId()));
            DataProvider.getInstance(getActivity())
                    .map(dataProvider -> dataProvider.update(PlayaDatabase.POIS, poiValues, PlayaItemTable.id + " = ?", String.valueOf(itemId)))
                    .subscribe(numUpdated -> Timber.d("Updated marker with status " + numUpdated));
        } else
            Timber.w("Unable to find custom marker in map for updating");
    }

    private void resetMapView() {
        getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Geo.MAN_LAT, Geo.MAN_LON), 14));
    }
}
