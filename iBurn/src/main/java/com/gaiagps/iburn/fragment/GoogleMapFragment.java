package com.gaiagps.iburn.fragment;

import android.app.AlertDialog;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
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
            mapCamps = false;
            if (lastZoomLevel > POI_ZOOM_LEVEL) restartLoaders(true);

        } else {
            mState = STATE.SEARCH;
            mapCamps = true;
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
    final int ALL = 4;

    // Limit mapped pois
    boolean mapCamps = false;
    boolean mapArt = true;
    boolean mapEvents = true;

    private final int POI_ZOOM_LEVEL = 16;
    float lastZoomLevel = 0;
    LatLng lastTarget;
    int mLoaderType = 0;

    private static final int MAX_POIS = 200;
    ArrayDeque<Marker> markerQueue = new ArrayDeque<>(MAX_POIS);
    HashMap<String, String> markerIdToMeta = new HashMap<>();
    MapBoxOfflineTileProvider tileProvider;
    TileOverlay overlay;
    LatLng latLngToCenterOn;

    VisibleRegion visibleRegion;
    String mCurFilter;                      // Search string to filter by
    boolean limitListToFavorites = false;   // Limit display to favorites?

    boolean settingHomeLocation = false;

    public static GoogleMapFragment newInstance() {
        return new GoogleMapFragment();
    }

    public GoogleMapFragment() {
        super();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (tileProvider != null)
            tileProvider.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.map, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (PlayaClient.getHomeLatLng(getActivity()) != null && !settingHomeLocation) {
            // GoogleMapFragment may be used without its menu
            if (menu.findItem(R.id.action_home)!= null) {
                // If a home camp is set, "set home camp" -> "navigate home"
                menu.findItem(R.id.action_home).setTitle(R.string.action_nav_home);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_home:
                if (PlayaClient.getHomeLatLng(getActivity()) == null && !settingHomeLocation) {
                    settingHomeLocation = true;
                    Toast.makeText(GoogleMapFragment.this.getActivity(), "Hold then drag the pin to set your home camp", Toast.LENGTH_LONG).show();
                    addHomePin(new LatLng(Constants.MAN_LAT, Constants.MAN_LON));
                } else if (!settingHomeLocation) {
                    // If a home camp is set, "set home camp" -> "navigate home"
                    navigateHome();
                }
                break;
        }

        return true;
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
        addMBTileOverlay(R.raw.iburn);
        getMap().getUiSettings().setZoomControlsEnabled(false);
        addHomePin(PlayaClient.getHomeLatLng(getActivity()));
        // TODO: If user location present, start there
        LatLng mStartLocation = new LatLng(Constants.MAN_LAT, Constants.MAN_LON);
        visibleRegion = getMap().getProjection().getVisibleRegion();
        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(mStartLocation, 14));

        if (latLngToCenterOn != null) {
            getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(latLngToCenterOn, 14));
            latLngToCenterOn = null;
        }

        getMap().setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {

            private final double MAX_LAT = 40.810716;
            private final double MAX_LON = -119.176357;
            private final double MIN_LAT = 40.765293;
            private final double MIN_LON = -119.232981;
            private final double BUFFER = .00005;

            private final double MAX_ZOOM = 19.5;
            private final double MIN_ZOOM = 12;

            private final int CAMERA_MOVE_REACT_THRESHOLD_MS = 500;
            private long lastCallMs = Long.MIN_VALUE;

            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                final long snap = System.currentTimeMillis();
                if (cameraPosition.target.longitude > MAX_LON || cameraPosition.target.longitude < MIN_LON ||
                        cameraPosition.target.latitude > MAX_LAT || cameraPosition.target.latitude < MIN_LAT ||
                        cameraPosition.zoom > MAX_ZOOM || cameraPosition.zoom < MIN_ZOOM) {
                    // Ensure map view is within valid bounds
                    getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(Math.min(MAX_LAT - BUFFER, Math.max(cameraPosition.target.latitude, MIN_LAT + BUFFER)),
                                       Math.min(MAX_LON - BUFFER, Math.max(cameraPosition.target.longitude, MIN_LON + BUFFER))),
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
                    Constants.PLAYA_ITEM playaItem = null;
                    switch (model_type) {
                        case ART:
                            playaItem = Constants.PLAYA_ITEM.ART;
                            break;
                        case EVENTS:
                            playaItem = Constants.PLAYA_ITEM.EVENT;
                            break;
                        case CAMPS:
                            playaItem = Constants.PLAYA_ITEM.CAMP;
                            break;
                    }
                    Intent i = new Intent(getActivity(), PlayaItemViewActivity.class);
                    i.putExtra("model_id", model_id);
                    i.putExtra("playa_item", playaItem);
                    getActivity().startActivity(i);
                }
            }
        });

    }

    private void addMBTileOverlay(int MBTileAssetId) {
        new AsyncTask<Integer, Void, Void>() {

            @Override
            protected Void doInBackground(Integer... params) {
                int MBTileAssetId = params[0];
                if (getActivity() != null)
                    FileUtils.copyMBTilesToSD(getActivity().getApplicationContext(), MBTileAssetId, Constants.MBTILE_DESTINATION);
                else {
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
                GoogleMap map = getMap();
                map.setMapType(GoogleMap.MAP_TYPE_NONE);
                map.setMyLocationEnabled(true);
                TileOverlayOptions opts = new TileOverlayOptions();

                tileProvider = new MapBoxOfflineTileProvider(MBTFile);
                opts.tileProvider(tileProvider);
                overlay = map.addTileOverlay(opts);

            }
        }.execute(MBTileAssetId);

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
        return markerQueue.size() > 0;
    }

    public void clearMap() {
        for (Marker marker : markerQueue) {
            marker.remove();
        }
        markerQueue.clear();
        markerIdToMeta.clear();
        mLoaderType = 0;
    }

    public void mapMarker(MarkerOptions marker) {
        getMap().addMarker(marker);
    }

    public void mapAndCenterOnMarker(MarkerOptions marker) {
        latLngToCenterOn = marker.getPosition();
        mapMarker(marker);
    }

    public void showcaseMarker(MarkerOptions marker) {
        mState = STATE.SHOWCASE;
        mapAndCenterOnMarker(marker);

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

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = PROJECTION;
        Uri targetUri = null;
        String selection = "";
        ArrayList<String> selectionArgs = new ArrayList<>();

        switch (i) {
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
        if (mState == STATE.EXPLORE) {
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

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        Log.i(TAG, "Creating loader with uri: " + targetUri.toString());
        return new CursorLoader(getActivity(), targetUri, projection, selection, selectionArgs.toArray(new String[selectionArgs.size()]),
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        int id = cursorLoader.getId();
        GoogleMap map = getMap();
        if (map == null) return;
        String markerMapId;
        while (cursor.moveToNext()) {
            markerMapId = String.format("%d-%d", id, cursor.getInt(cursor.getColumnIndex(ArtTable.id)));
            if (!markerIdToMeta.containsValue(markerMapId)) {
                // This POI is not yet mapped
                LatLng pos = new LatLng(cursor.getDouble(cursor.getColumnIndex(ArtTable.latitude)), cursor.getDouble(cursor.getColumnIndex(ArtTable.longitude)));
                if (markerQueue.size() == MAX_POIS) {
                    // We should re-use the eldest Marker
                    Marker marker = markerQueue.remove();
                    marker.setPosition(pos);
                    marker.setTitle(cursor.getString(cursor.getColumnIndex(ArtTable.name)));

                    switch (id) {
                        case ALL:
                            if (cursor.getFloat(cursor.getColumnIndex("art.latitude")) != 0) {
                                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.art_marker));
                            } else if (cursor.getFloat(cursor.getColumnIndex("camps.latitude")) != 0) {
                                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.camp_marker));
                            } else {
                                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.event_marker));
                            }
                            break;
                        case ART:
                            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.art_marker));
                            break;
                        case CAMPS:
                            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.camp_marker));
                            break;
                        case EVENTS:
                            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.event_marker));
                            break;
                    }

                    markerQueue.add(marker);
                    markerIdToMeta.put(marker.getId(), String.format("%d-%d", id, cursor.getInt(cursor.getColumnIndex(ArtTable.id))));
                } else {
                    // We shall create a new Marker
                    MarkerOptions markerOptions;
                    markerOptions = new MarkerOptions().position(pos)
                            .title(cursor.getString(cursor.getColumnIndex(ArtTable.name)));

                    switch (id) {
                        case ALL:
                            if (cursor.getFloat(cursor.getColumnIndex("art.latitude")) != 0) {
                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.art_marker));
                            } else if (cursor.getFloat(cursor.getColumnIndex("camps.latitude")) != 0) {
                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.camp_marker));
                            } else {
                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.event_marker));
                            }
                            break;
                        case ART:
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.art_marker));
                            break;
                        case CAMPS:
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.camp_marker));
                            break;
                        case EVENTS:
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.event_marker));
                            break;
                    }

                    Marker marker = map.addMarker(markerOptions);
                    markerIdToMeta.put(marker.getId(), String.format("%d-%d", id, cursor.getInt(cursor.getColumnIndex(ArtTable.id))));
                    markerQueue.add(marker);
                }
            }
        }
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

    private void addHomePin(LatLng latLng) {
        if (latLng == null)
            return;
        Marker marker = getMap().addMarker(new MarkerOptions()
                .position(latLng)
                .draggable(true)
                .title(getActivity().getString(R.string.home))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.home_marker)));
        final String homeMarkerId = marker.getId();
        getMap().setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                if (marker.getId().compareTo(homeMarkerId) == 0) {
                    PlayaClient.setHomeLatLng(GoogleMapFragment.this.getActivity().getApplicationContext(), marker.getPosition());
                    getActivity().invalidateOptionsMenu();
                }
            }
        });
    }

    private void restartLoaders(boolean clearMap) {
        if (clearMap)
            clearMap();
        if (mapCamps)
            restartLoader(CAMPS);
        if (mapArt)
            restartLoader(ART);
        if (mapEvents)
            restartLoader(EVENTS);
    }
}
