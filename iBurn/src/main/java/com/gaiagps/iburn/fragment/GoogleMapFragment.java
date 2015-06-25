package com.gaiagps.iburn.fragment;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.FileUtils;
import com.gaiagps.iburn.PlayaClient;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.Searchable;
import com.gaiagps.iburn.activity.PlayaItemViewActivity;
import com.gaiagps.iburn.database.ArtTable;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.PlayaContentProvider;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.database.UserPoiTable;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
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

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by davidbrodsky on 8/3/13.
 */
public class GoogleMapFragment extends SupportMapFragment implements LoaderManager.LoaderCallbacks<Cursor>, Searchable {
    private static final String TAG = "GoogleMapFragment";

    @Override
    public void onSearchQueryRequested(String query) {
        mCurFilter = query;
        if (TextUtils.isEmpty(query)) {
            if (areMarkersVisible()) clearMap();
            mState = STATE.EXPLORE;
            if (lastZoomLevel > POI_ZOOM_LEVEL) restartLoaders(true);

        } else {
            mState = STATE.SEARCH;
            // TODO: Better way of counting all loaders in a query
            // Camps, Art, Events. Manually counting on restartLoader etc. causes mismatches
            mLoaderResponsesExpected = 3;
            restartLoaders(true);
        }
    }

    private enum STATE {
        /** Default. Constantly search and show POIs within the viewable map region */
        EXPLORE,
        /** Showcase a particular POI and its relation to the user home camp / location */
        SHOWCASE,
        /** Show search results **/
        SEARCH
    }

    private STATE mState = STATE.EXPLORE;

    // Loader ids
    final int ART = 1;
    final int CAMPS = 2;
    final int EVENTS = 3;
    final int POIS = 4;
    final int ALL = 5;

    // Limit mapped pois
    boolean mapCamps = true;
    boolean mapArt = true;
    boolean mapEvents = true;
    boolean mapUserPois = true;

    private final int POI_ZOOM_LEVEL = 18;
    float lastZoomLevel = 0;
    int mLoaderType = 0;
    /** When restartLoaders is called, how many loaders were simultaneously started.
     *  We use this number to collect a batch of loader results into a single collection
     *  to center the camera on.
     *
     *  Set on each call to {@link #restartLoaders(boolean)}, Read on each call to
     *  {@link #onLoadFinished(android.support.v4.content.Loader, android.database.Cursor)}
     */
    private int mLoaderResponsesExpected;

    /** Map of user added pins. Google Marker Id -> Database Id */
    HashMap<String, String> mMappedCustomMarkerIds = new HashMap<>();
    /** Map of pins shown in response to explore or search */
    private static final int MAX_POIS = 200;
    ArrayDeque<Marker> mMappedMarkers = new ArrayDeque<>(MAX_POIS);
    HashMap<String, String> markerIdToMeta = new HashMap<>();
    private static MapBoxOfflineTileProvider tileProvider; // Re-use tileProvider
    private static AtomicInteger tileProviderHolds = new AtomicInteger();
    TileOverlay overlay;
    LatLng latLngToCenterOn;

    VisibleRegion visibleRegion;
    String mCurFilter;                      // Search string to filter by
    boolean limitListToFavorites = false;   // Limit display to favorites?

    private View.OnClickListener mOnAddPinBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Marker marker = addCustomPin(null, null, UserPoiTable.STAR);
            showEditPinDialog(marker);
        }
    };

    private void showEditPinDialog(final Marker marker) {
        View dialogBody = getActivity().getLayoutInflater().inflate(R.layout.dialog_poi, null);
        final RadioGroup iconGroup = ((RadioGroup) dialogBody.findViewById(R.id.iconGroup));
        // Fetch current Marker icon
        Cursor poi = getActivity().getContentResolver().query(PlayaContentProvider.Pois.POIS,
                new String[] {PlayaItemTable.id, UserPoiTable.drawableResId},
                PlayaItemTable.id + " = ?",
                new String[] { String.valueOf(getDatabaseIdFromGeneratedDataId(mMappedCustomMarkerIds.get(marker.getId())))}, null);
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
                    Log.e(TAG, "Unknown custom marker type");
            }
        }
        final EditText markerTitle = (EditText) dialogBody.findViewById(R.id.markerTitle);
        markerTitle.setText(marker.getTitle());
        new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.Theme_Iburn))
                .setView(dialogBody)
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Save the title
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
                    }
                })
                .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Delete Pin
                        removeCustomPin(marker);
                    }
                })
                .show();
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
        int margin = (int)(dpValue * d); // margin in pixels
        setMargins(addPoiBtn, 0, margin * 6, margin, 0);
        return parent;
    }

    /**
     * Thanks to SO:
     * http://stackoverflow.com/questions/4472429/change-the-right-margin-of-a-view-programmatically
     */
    public static void setMargins (View v, int l, int t, int r, int b) {
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

    /**
     * Load all user placeable POIs and attach
     * a {@link com.google.android.gms.maps.GoogleMap.OnMarkerDragListener}
     * on pins of that type
     */
    private void loadCustomPins() {
        restartLoader(POIS);
        getMap().setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                if ( mMappedCustomMarkerIds.containsKey(marker.getId()) ) {
                    updateCustomPinWithMarker(marker, 0);
                }
            }
        });
    }

    private void initMap() {

        addMBTileOverlay(R.raw.iburn);
        UiSettings settings = getMap().getUiSettings();
        settings.setZoomControlsEnabled(false);
        settings.setMapToolbarEnabled(false);
        settings.setScrollGesturesEnabled(mState != STATE.SHOWCASE);
        loadCustomPins();
        //addCustomPin(PlayaClient.getHomeLatLng(getActivity()));
        // TODO: If user location present, start there
        LatLng mStartLocation = new LatLng(Constants.MAN_LAT, Constants.MAN_LON);
        visibleRegion = getMap().getProjection().getVisibleRegion();
        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(mStartLocation, 14));

        getMap().setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
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
                final long snap = System.currentTimeMillis();
                if (!PlayaClient.BRC_BOUNDS.contains(cameraPosition.target) ||
                        cameraPosition.zoom > MAX_ZOOM || cameraPosition.zoom < MIN_ZOOM) {
                    getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(
                                    Math.min(PlayaClient.MAX_LAT - BUFFER, Math.max(cameraPosition.target.latitude, PlayaClient.MIN_LAT + BUFFER)),
                                    Math.min(PlayaClient.MAX_LON - BUFFER, Math.max(cameraPosition.target.longitude, PlayaClient.MIN_LON + BUFFER))),
                            (float) Math.min(Math.max(cameraPosition.zoom, MIN_ZOOM), MAX_ZOOM)));
                } else {
                    // Map view bounds valid. Load POIs if necessary
                    if (cameraPosition.zoom > POI_ZOOM_LEVEL && PlayaClient.isEmbargoClear(getActivity())) {
                        visibleRegion = getMap().getProjection().getVisibleRegion();
                        if (mState == STATE.EXPLORE) {
                            // Don't bother restartingLoader more than THRESHOLD_MS
                            if (lastCallMs + CAMERA_MOVE_REACT_THRESHOLD_MS > snap) {
                                lastCallMs = snap;
                                return;
                            }
                            restartLoaders(false);
                        }
                    } else if (cameraPosition.zoom < POI_ZOOM_LEVEL && areMarkersVisible()) {
                        if (mState == STATE.EXPLORE) {
                            markerIdToMeta = new HashMap<>();
                            clearMap();
                        }
                    }
                }
                lastCallMs = snap;
            }
        });

        getMap().setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
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
            }
        });
    }

    private void addMBTileOverlay(int MBTileAssetId) {
        if (tileProvider != null) {
            Log.d("GoogleMapFragment", "Reusing tileProvider");
            _addMBTilesOverlay();
            return;
        }

        new AsyncTask<Integer, Void, Void>() {

            @Override
            protected Void doInBackground(Integer... params) {
                int MBTileAssetId = params[0];
                if (getActivity() != null) {
                    FileUtils.copyMBTilesToSD(getActivity().getApplicationContext(), MBTileAssetId, Constants.MBTILE_DESTINATION);
                } else {
                    Log.e(TAG, "getActivity() null on addMBTileOverlay");
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (getActivity() == null)
                    return;
                String tilesPath = String.format("%s/%s/%s/%s", Environment.getExternalStorageDirectory().getAbsolutePath(),
                        Constants.IBURN_ROOT, Constants.TILES_DIR, Constants.MBTILE_DESTINATION);

                File MBTFile = new File(tilesPath);
                tileProvider = new MapBoxOfflineTileProvider(MBTFile);

                _addMBTilesOverlay();
            }
        }.execute(MBTileAssetId);

    }

    /** Add {@link #tileProvider} to the current Map and increment the num of tileProvider holds */
    private void _addMBTilesOverlay() {
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                tileProviderHolds.incrementAndGet();
                map.setMapType(GoogleMap.MAP_TYPE_NONE);
                map.setMyLocationEnabled(true);
                TileOverlayOptions opts = new TileOverlayOptions();

                opts.tileProvider(tileProvider);
                overlay = map.addTileOverlay(opts);
            }
        });
    }

    private void navigateHome() {
        if (getMap().getMyLocation() == null) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Where are you?")
                    .setMessage("We're still working on your location. Try again in a few seconds!")
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
            getMap().animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(new LatLng(Constants.MAN_LAT, Constants.MAN_LON)).zoom(15).build()));
            return;
        }
        LatLng start = new LatLng(getMap().getMyLocation().getLatitude(), getMap().getMyLocation().getLongitude());
        LatLng end = PlayaClient.getHomeLatLng(getActivity());
        if (getDistance(start, end) > 8046) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(getActivity().getString(R.string.youre_so_far))
                    .setMessage(String.format("It appears you're %d meters from home. Get closer to the burn before navigating home..", (int) getDistance(start, end)))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();

//            getMap().animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(new LatLng(Constants.MAN_LAT, Constants.MAN_LON)).zoom(15).build()));
            return;
        }

        getMap().addMarker(new MarkerOptions()
                .position(start)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .title("Current Location"));
        getMap().addMarker(new MarkerOptions()
                .position(end)
                .title("Home"));

        getMap().animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().bearing(getBearing(start, end)).target(getMidPoint(start, end)).tilt(45).zoom(15).build()));

        DecimalFormat twoDForm = new DecimalFormat("#");
        Toast.makeText(getActivity(), String.format("%s meters from home", twoDForm.format(getDistance(start, end))), Toast.LENGTH_LONG).show();

    }

    private float getBearing(LatLng start, LatLng end) {
        double longitude1 = start.longitude;
        double longitude2 = end.longitude;
        double latitude1 = Math.toRadians(start.latitude);
        double latitude2 = Math.toRadians(end.latitude);
        double longDiff = Math.toRadians(longitude2 - longitude1);
        double y = Math.sin(longDiff) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);

        return (float) (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    private LatLng getMidPoint(LatLng start, LatLng end) {

        double dLon = Math.toRadians(end.longitude - start.longitude);
        double lat1;
        double lat2;
        double lon1;
        //convert to radians
        lat1 = Math.toRadians(start.latitude);
        lat2 = Math.toRadians(end.latitude);
        lon1 = Math.toRadians(start.longitude);

        double Bx = Math.cos(lat2) * Math.cos(dLon);
        double By = Math.cos(lat2) * Math.sin(dLon);
        double lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
        double lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);

        //print out in degrees
        return new LatLng(Math.toDegrees(lat3), Math.toDegrees(lon3));
    }

    private double getDistance(LatLng start, LatLng end) {
        double theta = start.longitude - end.longitude;
        double dist = Math.sin(Math.toRadians(start.latitude)) * Math.sin(Math.toRadians(end.latitude)) + Math.cos(Math.toRadians(start.latitude)) * Math.cos(Math.toRadians(end.latitude)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515;
        dist = dist * 1609.344; // to km
        return dist;
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
        mLoaderType = 0;
    }

    public void mapAndCenterOnMarker(final MarkerOptions marker) {
        latLngToCenterOn = marker.getPosition();
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                googleMap.addMarker(marker);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngToCenterOn, 14));
            }
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

    static final ArrayList<String> PROJECTION = new ArrayList<String>() {{
        add(PlayaItemTable.name);
        add(PlayaItemTable.id);
        add(PlayaItemTable.latitude);
        add(PlayaItemTable.longitude);
        add(PlayaItemTable.favorite);
    }};

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        Uri targetUri = null;
        String selection = "";
        ArrayList<String> selectionArgs = new ArrayList<>();

        switch (loaderId) {
            case ART:
                targetUri = PlayaContentProvider.Art.ART;
                break;
            case CAMPS:
                targetUri = PlayaContentProvider.Camps.CAMPS;
                break;
            case EVENTS:
                targetUri = PlayaContentProvider.Events.EVENTS;
                // Select by event currently ongoing
                Date now = new Date();
                selection += String.format("(%1$s < '%2$s' AND %3$s > '%2$s') ",
                            EventTable.startTime,   PlayaClient.getISOString(now),
                            EventTable.endTime);
                break;
            case POIS:
                targetUri = PlayaContentProvider.Pois.POIS;
                break;
            case ALL:
            default:
                //targetUri = PlayaContentProvider.Camps.ALL;
                //throw new IllegalArgumentException("ALL endpoint not yet supported");
                return null;
        }

        if (!TextUtils.isEmpty(mCurFilter)) {
            Log.i(TAG, "filtering map by " + mCurFilter);
            // Add to selection, selectionArgs for name filter
            if (selection.length() > 0) selection += " AND ";
            selection += PlayaItemTable.name + " LIKE ?";
            selectionArgs.add("%" + mCurFilter + "%");
        }

        // Select by latitude and longitude within screen-visible region
        if (mState == STATE.EXPLORE && visibleRegion != null) {
            if (selection.length() > 0) selection += " AND ";
            selection += String.format("(%s < ? AND %s > ?) AND (%s < ? AND %s > ?)",
                    PlayaItemTable.latitude, PlayaItemTable.latitude,
                    PlayaItemTable.longitude, PlayaItemTable.longitude);

            selectionArgs.add(String.valueOf(visibleRegion.farLeft.latitude));
            selectionArgs.add(String.valueOf(visibleRegion.nearRight.latitude));
            selectionArgs.add(String.valueOf(visibleRegion.nearRight.longitude));
            selectionArgs.add(String.valueOf(visibleRegion.farLeft.longitude));
        }

        if (limitListToFavorites) {
            selection += " AND " + ArtTable.favorite + " =?";
            selectionArgs.add("1");
        }

        String[] projection = null;
        if (loaderId == POIS) {
            // If we're fetching POIs, add the drawableResourceId column
            projection = PROJECTION.toArray(new String[PROJECTION.size() + 1]);
            projection[projection.length-1] = UserPoiTable.drawableResId;
        } else {
            projection = PROJECTION.toArray(new String[PROJECTION.size()]);
        }

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        Log.i(TAG, "Creating loader with uri: " + targetUri.toString());
        return new CursorLoader(getActivity(), targetUri, projection, selection, selectionArgs.toArray(new String[selectionArgs.size()]),
                null);
    }

    /** Keep track of the bounds describing a batch of results across Loaders */
    private LatLngBounds.Builder mResultBounds = new LatLngBounds.Builder();

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (mState == STATE.SEARCH)
            mLoaderResponsesExpected--;
        int id = cursorLoader.getId();
        Log.i(TAG, "Loader finished for id " + id + " with items " + cursor.getCount());
        GoogleMap map = getMap();
        if (map == null) return;
        String markerMapId;
        // Sorry, but Java has no immutable primitives and LatLngBounds has no indicator
        // of when calling .build() will throw IllegalStateException due to including no points
        boolean[] areBoundsValid = new boolean[1];
        while (cursor.moveToNext()) {
            markerMapId = generateDataIdForItem(id, cursor.getInt(cursor.getColumnIndex(PlayaItemTable.id)));
            if (id == POIS) {
                if (!mMappedCustomMarkerIds.containsValue(markerMapId)) {
                    Marker marker = addNewMarkerForCursorItem(id, cursor);
                    mMappedCustomMarkerIds.put(marker.getId(), markerMapId);
                }
            } else {
                mapRecyclableMarker(id, markerMapId, cursor, mResultBounds, areBoundsValid);
            }
        }
        if (mState == STATE.SEARCH)
            Log.i(TAG, "Loader responses expected " + mLoaderResponsesExpected);
        if (areBoundsValid[0] && mState == STATE.SEARCH && mLoaderResponsesExpected == 0) {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(mResultBounds.build(), 80));
        } else if (!areBoundsValid[0] && mState == STATE.SEARCH && mLoaderResponsesExpected == 0) {
            // No results
            Log.i(TAG, "Resetting map view");
            resetMapView();
        }
        if (mLoaderResponsesExpected == 0) {
            // Reset query bounds
            mResultBounds = new LatLngBounds.Builder();
        }

    }

    /**
     * Return a key used internally to keep track of data items currently mapped,
     * helping us avoid mapping duplicate points.
     * @param loaderId The id of the loader
     * @param itemId The database id of the item
     */
    private String generateDataIdForItem(int loaderId, int itemId) {
        return String.format("%d-%d", loaderId, itemId);
    }

    /**
     * Return the internal database id for an item given the string id
     * generated by {@link #generateDataIdForItem(int, int)}
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
                if (PlayaClient.BRC_BOUNDS.contains(pos)) {
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

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void restartLoader(int type) {
        mLoaderType = type;
        getLoaderManager().restartLoader(type, null, this);

    }

    public void initLoader() {
        getLoaderManager().initLoader(0, null, this);
    }

    private void removeCustomPin(Marker marker) {
        marker.remove();
        if (mMappedCustomMarkerIds.containsKey(marker.getId())) {
            int itemId = getDatabaseIdFromGeneratedDataId(mMappedCustomMarkerIds.get(marker.getId()));
            int numDeleted = getActivity().getContentResolver().delete(PlayaContentProvider.Pois.POIS, PlayaItemTable.id + " = ?", new String[] { String.valueOf(itemId)});
            if (numDeleted != 1) Log.w(TAG, "Unable to delete marker " + marker.getTitle());
        } else Log.w(TAG, "Unable to delete marker " + marker.getTitle());
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
            int newId = Integer.parseInt(getActivity().getContentResolver().insert(PlayaContentProvider.Pois.POIS, poiValues).getLastPathSegment());
            mMappedCustomMarkerIds.put(marker.getId(), generateDataIdForItem(POIS, newId));
        } catch (NumberFormatException e) {
                Log.w(TAG, "Unable to get id for new custom marker");
        }

        return marker;
    }

    /**
     * Apply style to a custom MarkerOptions before
     * adding to Map
     *
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
     *
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
            int numUpdated = getActivity().getContentResolver().update(PlayaContentProvider.Pois.POIS, poiValues, PlayaItemTable.id + " = ?", new String[] { String.valueOf(itemId)});
            if (numUpdated != 1) Log.w(TAG, "Failed to update custom pin with marker");
        } else
            Log.w(TAG, "Unable to find custom marker in map for updating");
    }

    private void restartLoaders(boolean clearMap) {
        if (clearMap)
            clearMap();
        if (mapCamps && PlayaClient.isEmbargoClear(getActivity()))
            restartLoader(CAMPS);
        if (mapArt)
            restartLoader(ART);
        if (mapEvents && PlayaClient.isEmbargoClear(getActivity()))
            restartLoader(EVENTS);
        // User POIs are never cleared
        // and so shouldn't need to be reset
    }

    private void resetMapView() {
        getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Constants.MAN_LAT, Constants.MAN_LON), 14));
    }
}
