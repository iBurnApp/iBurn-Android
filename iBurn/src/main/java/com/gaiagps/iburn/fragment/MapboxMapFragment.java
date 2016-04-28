package com.gaiagps.iburn.fragment;

import android.database.Cursor;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.CurrentDateProvider;
import com.gaiagps.iburn.Geo;
import com.gaiagps.iburn.PrefsHelper;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.Embargo;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.database.UserPoiTable;
import com.google.android.gms.maps.model.LatLngBounds;
import com.mapbox.mapboxsdk.MapFragment;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;
import com.squareup.sqlbrite.SqlBrite;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;


/**
 * Created by dbro on 3/11/16.
 */
public class MapboxMapFragment extends MapFragment implements MapView.OnMapChangedListener {

    private enum State {
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

    private State state = State.EXPLORE;

    private final double POI_ZOOM_LEVEL = 16.5;
    float currentZoom = 0;

    private PublishSubject<Integer> cameraUpdateSubject;
    private PrefsHelper prefs;

    @SuppressWarnings("MissingPermission")
    @Override
    public void onStart() {
        super.onStart();

        prefs = new PrefsHelper(getContext().getApplicationContext());

        MapView map = getMap();
        map.setStyleUrl("mapbox://styles/onlyinamerica/cilogpkr20018a7ltf5yin2uk");

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(Geo.MAN_LAT, Geo.MAN_LON)) // Sets the center of the map to Chicago
                .zoom(11)
                .build();

        map.addOnMapChangedListener(this);
        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        map.setMyLocationEnabled(true);

        cameraUpdateSubject = PublishSubject.create();

        Observable.combineLatest(

                cameraUpdateSubject
                        .sample(100, TimeUnit.MILLISECONDS),

                DataProvider.getInstance(getContext().getApplicationContext()),
                (change, dataProvider) -> dataProvider)
                .flatMap(dataProvider -> performQuery(dataProvider, getCurrentBoundingBox()))
                .map(SqlBrite.Query::run)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(cursor -> {
                    currentZoom = getMap().getCameraPosition().zoom;
                    Timber.d("Setting current zoom %f", currentZoom);
                })
                .subscribe(this::processMapItemResult, throwable -> Timber.e(throwable, "Error querying"));
    }

    @Override
    public void onStop() {
        super.onStop();

        cameraUpdateSubject.onCompleted();
        cameraUpdateSubject = null;
    }

    @Override
    public void onMapChanged(int change) {
        if (cameraUpdateSubject == null) return;

        if (change == MapView.REGION_DID_CHANGE)
            cameraUpdateSubject.onNext(change);
    }

    private BoundingBox getCurrentBoundingBox() {
        MapView map = getMap();

        int viewportWidth = map.getWidth();
        int viewportHeight = map.getHeight();

        LatLng topRight = map.fromScreenLocation(new PointF(viewportWidth, 0));
        LatLng bottomLeft = map.fromScreenLocation(new PointF(0, viewportHeight));

        return new BoundingBox(bottomLeft, topRight);
    }

    private void resetMapView() {
        getMap().animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(Geo.MAN_LAT, Geo.MAN_LON), 14, 0, 0)));
    }

    //<editor-fold desc="POI Mapping">

    /**
     * Map of user added pins. Mapbox Marker Id -> Database Id
     */
    HashMap<Long, String> mMappedCustomMarkerIds = new HashMap<>();
    /**
     * Map of pins shown in response to explore or search
     */
    private static final int MAX_POIS = 100;

    /**
     * Keep track of the bounds describing a batch of results across Loaders
     */
    private LatLngBounds.Builder mResultBounds;

    // Markers that should only be cleared on new query arrival
    HashSet<Marker> mappedTransientMarkers = new HashSet<>();
    HashSet<Marker> permanentMarkers = new HashSet<>();
    // Markers that should be cleared on camera events
    HashMap<Long, String> markerIdToMeta = new HashMap<>();

    private void processMapItemResult(Cursor cursor) {

        clearPermanentMarkers();
        //mResultBounds = new LatLngBounds.Builder();

        Timber.d("Got cursor result with %d items", cursor.getCount());

        MapView map = getMap();
        String markerMapId;
        // Sorry, but Java has no immutable primitives and LatLngBounds has no indicator
        // of when calling .build() will throw IllegalStateException due to including no points
        boolean[] areBoundsValid = new boolean[1];
        while (cursor.moveToNext()) {
            if (cursor.getDouble(cursor.getColumnIndex(PlayaItemTable.latitude)) == 0) continue;

            int typeInt = cursor.getInt(cursor.getColumnIndex(DataProvider.VirtualType));
            Constants.PlayaItemType type = DataProvider.getTypeValue(typeInt);

            markerMapId = generateDataIdForItem(type, cursor.getInt(cursor.getColumnIndex(PlayaItemTable.id)));

            if (type == Constants.PlayaItemType.POI) {
                // POIs are permanent markers that are editable when their info window is clicked
                if (!mMappedCustomMarkerIds.containsValue(markerMapId)) {
                    Marker marker = addNewMarkerForCursorItem(typeInt, cursor);
                    mMappedCustomMarkerIds.put(marker.getId(), markerMapId);
                }
            } else if (cursor.getInt(cursor.getColumnIndex(PlayaItemTable.favorite)) == 1) {
                // Favorites are permanent markers, but are not editable
                if (!markerIdToMeta.containsValue(markerMapId)) {
                    Marker marker = addNewMarkerForCursorItem(typeInt, cursor);
                    markerIdToMeta.put(marker.getId(), markerMapId);
                    permanentMarkers.add(marker);
                }
            } else if (currentZoom > POI_ZOOM_LEVEL) {
                // Other markers are recyclable, and may be cleared on camera events
                mapRecyclableMarker(typeInt, markerMapId, cursor);//, mResultBounds, areBoundsValid);
            }
        }
        cursor.close();
        if (areBoundsValid[0] && state == State.SEARCH) {
            // TODO : Animate map to enclose all search results
            //googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(mResultBounds.build(), 80));
        } else if (!areBoundsValid[0] && state == State.SEARCH) {
            // No results
            Timber.d("Resetting map view");
            resetMapView();
        }
    }

    /**
     * Map a marker as part of a finite set of markers, limiting the total markers
     * displayed and recycling markers if this limit is exceeded.
     *
     * @param areBoundsValid a hack one-dimensional boolean array used to report whether boundsBuilder
     *                       includes at least one point and will not throw an exception on its build()
     */
    private void mapRecyclableMarker(int itemType, String markerMapId, Cursor cursor) { //, LatLngBounds.Builder boundsBuilder, boolean[] areBoundsValid) {
        if (!markerIdToMeta.containsValue(markerMapId)) {
            // This POI is not yet mapped
//            com.google.android.gms.maps.model.LatLng pos = new com.google.android.gms.maps.model.LatLng(cursor.getDouble(cursor.getColumnIndex(PlayaItemTable.latitude)), cursor.getDouble(cursor.getColumnIndex(PlayaItemTable.longitude)));
//            if (itemType != DataProvider.getTypeValue(Constants.PlayaItemType.POI) && boundsBuilder != null && state == State.SEARCH) {
//                if (BRC_BOUNDS.contains(pos)) {
//                    boundsBuilder.include(pos);
//                    areBoundsValid[0] = true;
//                }
//            }
            // We shall create a new Marker
            Marker marker = addNewMarkerForCursorItem(itemType, cursor);
            markerIdToMeta.put(marker.getId(), markerMapId);
            mappedTransientMarkers.add(marker);
        }
    }

    /**
     * Return a key used internally to keep track of data items currently mapped,
     * helping us avoid mapping duplicate points.
     *
     * @param itemType The type of the item
     * @param itemId   The database id of the item
     */
    private String generateDataIdForItem(Constants.PlayaItemType itemType, long itemId) {
        return String.format("%d-%d", DataProvider.getTypeValue(itemType), itemId);
    }

    /**
     * Return the internal database id for an item given the string id
     * generated by {@link #generateDataIdForItem(Constants.PlayaItemType, long)}
     */
    private int getDatabaseIdFromGeneratedDataId(String dataId) {
        return Integer.parseInt(dataId.split("-")[1]);
    }


    /**
     * Clear markers marked permanent. These are not removed due to camera change events.
     * Currently used for user-selected favorite items.
     */
    public void clearPermanentMarkers() {
        for (Marker marker : permanentMarkers) {
            marker.remove();
            markerIdToMeta.remove(marker.getId());
        }
        permanentMarkers.clear();
    }

    public void clearMap(boolean clearAll) {
        if (clearAll) {
            clearPermanentMarkers();
        }

        for (Marker marker : mappedTransientMarkers) {
            marker.remove();
            markerIdToMeta.remove(marker.getId());
        }
        mappedTransientMarkers.clear();
    }

    private Marker addNewMarkerForCursorItem(int itemType, Cursor cursor) {
        LatLng pos = new LatLng(cursor.getDouble(cursor.getColumnIndex(PlayaItemTable.latitude)),
                cursor.getDouble(cursor.getColumnIndex(PlayaItemTable.longitude)));
        MarkerOptions markerOptions;
        markerOptions = new MarkerOptions().position(pos)
                .title(cursor.getString(cursor.getColumnIndex(PlayaItemTable.name)));

        Constants.PlayaItemType modelType = DataProvider.getTypeValue(itemType);
        IconFactory iconFactory = IconFactory.getInstance(getContext());
        switch (modelType) {
            case POI:
                // Favorite column is mapped to user poi icon type: A hack to make the union query work
                styleCustomMarkerOption(markerOptions, cursor.getInt(cursor.getColumnIndex(PlayaItemTable.favorite)));
                break;
            case ART:
                Drawable iconDrawable = ContextCompat.getDrawable(getContext(), R.drawable.art_pin);
                markerOptions.icon(iconFactory.fromDrawable(iconDrawable));
                //markerOptions.icon(iconFactory.fromResource(R.drawable.art_pin));
                break;
            case CAMP:
                markerOptions.icon(iconFactory.fromResource(R.drawable.camp_pin));
                break;
            case EVENT:
                markerOptions.icon(iconFactory.fromResource(R.drawable.event_pin));
                break;
        }

        // With GoogleMaps, we'd use this so that the marker shadow wasn't used as the "base"
        //markerOptions.anchor(0.5f, 0.5f);
        Marker marker = getMap().addMarker(markerOptions);
        Timber.d("Added marker with id %d", marker.getId());
        return marker;
    }

    private void styleCustomMarkerOption(MarkerOptions markerOption, int drawableResId) {
        IconFactory iconFactory = IconFactory.getInstance(getContext());
        switch (drawableResId) {
            case UserPoiTable.HOME:
                markerOption.icon(iconFactory.fromResource(R.drawable.puck_home));
                break;
            case UserPoiTable.STAR:
                markerOption.icon(iconFactory.fromResource(R.drawable.puck_star));
                break;
            case UserPoiTable.BIKE:
                markerOption.icon(iconFactory.fromResource(R.drawable.puck_bicycle));
                break;
            case UserPoiTable.HEART:
                markerOption.icon(iconFactory.fromResource(R.drawable.puck_heart));
                break;
        }
    }

    private void removeCustomPin(Marker marker) {
        marker.remove();
        if (mMappedCustomMarkerIds.containsKey(marker.getId())) {
            int itemId = getDatabaseIdFromGeneratedDataId(mMappedCustomMarkerIds.get(marker.getId()));
            DataProvider.getInstance(getActivity().getApplicationContext())
                    .map(provider -> provider.delete(PlayaDatabase.POIS, PlayaItemTable.id + " = ?", String.valueOf(itemId)))
                    .subscribe(result -> Timber.d("Deleted marker with result " + result));
        } else Timber.w("Unable to delete marker " + marker.getTitle());
    }


    //</editor-fold desc="POI Mapping">

    //<editor-fold desc="Database Queries">

    static String isFavoriteWhereClause = PlayaItemTable.favorite + " = 1";

    static String geoWhereClause = String.format("(%s < ? AND %s > ?) AND (%s < ? AND %s > ?)",
            PlayaItemTable.latitude, PlayaItemTable.latitude,
            PlayaItemTable.longitude, PlayaItemTable.longitude);

    static String ongoingWhereClause = String.format("(%s < ? AND %s > ?) ",
            EventTable.startTime, EventTable.endTime);

    static String notExpiredWhereClause = String.format("(%s > ?) ",
            EventTable.endTime);

    static String[] sqlParemeters = new String[11/*15*/];

    static final String[] PROJECTION = new String[]{
            PlayaItemTable.name,
            PlayaItemTable.id,
            PlayaItemTable.latitude,
            PlayaItemTable.longitude,
            PlayaItemTable.favorite
    };

    static final String PROJECTION_STRING = DataProvider.makeProjectionString(PROJECTION);

    public Observable<SqlBrite.Query> performQuery(@NonNull DataProvider provider,
                                                   @NonNull BoundingBox boundingBox) {

        // Query all items, not just POIs, if we have a visibleRegion and Embargo is inactive
        // POI table is not affected by Embargo
        boolean queryVisibleRegion = !Embargo.isEmbargoActive(prefs);

        // Don't show non-POI items if we're showcasing a marker to keep the map clear
        boolean queryNonUserItems = state != State.SHOWCASE && !Embargo.isEmbargoActive(prefs);

        StringBuilder sql = new StringBuilder();

        // Select User POIs
        sql.append("SELECT ").append(PROJECTION_STRING.replace(PlayaItemTable.favorite, UserPoiTable.drawableResId + " AS " + PlayaItemTable.favorite)).append(", ").append(4).append(" AS ").append(DataProvider.VirtualType).append(" FROM ").append(PlayaDatabase.POIS);

        if (queryNonUserItems) {
            // Select Events
            sql.append(" UNION ")
                    .append("SELECT ").append(PROJECTION_STRING).append(", ").append(3).append(" AS ").append(DataProvider.VirtualType).append(" FROM ").append(PlayaDatabase.EVENTS)
                    .append(" WHERE (")
                    .append(isFavoriteWhereClause)
                    .append(" AND ")
                    .append(notExpiredWhereClause)
                    .append(")");

            if (queryVisibleRegion) {
                sql.append(" OR (")
                        .append(ongoingWhereClause)
                        .append(" AND ")
                        .append(geoWhereClause)
                        .append(')');
            }

            // Select Art
            sql.append(" UNION ")
                    .append("SELECT ").append(PROJECTION_STRING).append(", ").append(2).append(" AS ").append(DataProvider.VirtualType).append(" FROM ").append(PlayaDatabase.ART)
                    .append(" WHERE ")
                    .append(isFavoriteWhereClause);

            if (queryVisibleRegion) {
                sql.append(" OR ")
                        .append(geoWhereClause);
            }

            // Select Camps
            /*
            sql.append(" UNION ")
                    .append("SELECT ").append(PROJECTION_STRING).append(", ").append(1).append(" AS ").append(DataProvider.VirtualType).append(" FROM ").append(PlayaDatabase.CAMPS)
                    .append(" WHERE ")
                    .append(isFavoriteWhereClause);

            if (queryVisibleRegion) {
                sql.append(" OR ")
                        .append(geoWhereClause);
            }
            */

            // Set visible region query parameters
            if (queryVisibleRegion) {
                // Event time
                sqlParemeters[0] = PlayaDateTypeAdapter.iso8601Format.format(CurrentDateProvider.getCurrentDate());
                sqlParemeters[1] = sqlParemeters[2] = PlayaDateTypeAdapter.iso8601Format.format(CurrentDateProvider.getCurrentDate());

                // Event, Art, Camp Geo
                sqlParemeters[3] = sqlParemeters[7] /*= sqlParemeters[10]*/ = String.valueOf(boundingBox.getLatSouth());
                sqlParemeters[4] = sqlParemeters[8] /*= sqlParemeters[11]*/ = String.valueOf(boundingBox.getLatNorth());
                sqlParemeters[5] = sqlParemeters[9] /*= sqlParemeters[12]*/ = String.valueOf(boundingBox.getLonWest());
                sqlParemeters[6] = sqlParemeters[10] /*= sqlParemeters[13]*/ = String.valueOf(boundingBox.getLonEast());
            }
        }
        if (queryNonUserItems) {
            return provider.createQuery(PlayaDatabase.ALL_TABLES, sql.toString(), queryVisibleRegion ? sqlParemeters : null);
        } else {
            return provider.createQuery(PlayaDatabase.POIS, sql.toString());
        }
    }

    //</editor-fold desc="Database">
}
