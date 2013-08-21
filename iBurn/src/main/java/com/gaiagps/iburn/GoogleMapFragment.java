package com.gaiagps.iburn;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.gaiagps.iburn.database.ArtTable;
import com.gaiagps.iburn.database.CampTable;
import com.gaiagps.iburn.database.EventTable;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;

import java.io.File;
import java.text.DecimalFormat;

/**
 * Created by davidbrodsky on 8/3/13.
 */
public class GoogleMapFragment extends SupportMapFragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String TAG = "GoogleMapFragment";

    // Loader ids
    final int ART = 1;
    final int CAMPS = 2;
    final int EVENTS = 3;
    final int ALL = 4;

    float lastZoomLevel = 0;
    int state = 0;

    MapBoxOfflineTileProvider tileProvider;
    TileOverlay overlay;
    LatLng latLngToCenterOn;

    String mCurFilter;                      // Search string to filter by
    boolean limitListToFavorites = false;   // Limit display to favorites?

    boolean settingHomeLocation = false;

    public GoogleMapFragment() {
        super();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(tileProvider != null)
            tileProvider.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(BurnState.isEmbargoClear(getActivity().getApplicationContext()))
            inflater.inflate(R.menu.map, menu);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item){
       int id = item.getItemId();

       if(state != 0){
           clearMap();
       }

       switch(id){
           case R.id.action_home:
               if(BurnState.getHomeLatLng(getActivity()) == null && !settingHomeLocation){
                   settingHomeLocation = true;
                   Toast.makeText(GoogleMapFragment.this.getActivity(), "Hold then drag the pin to set your home camp", Toast.LENGTH_LONG).show();
                   addHomePin(new LatLng(Constants.MAN_LAT, Constants.MAN_LON));
               }else if(!settingHomeLocation){
                   navigateHome();
               }
               break;
       }

        return true;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initMap();
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        latLngToCenterOn = null;
    }

    private void initMap(){
        addMBTileOverlay(R.raw.iburn);
        addHomePin(BurnState.getHomeLatLng(getActivity()));
        LatLng mStartLocation = new LatLng(Constants.MAN_LAT, Constants.MAN_LON);
        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(mStartLocation, 14));

        if(latLngToCenterOn != null){
            getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(latLngToCenterOn, 14));
            latLngToCenterOn = null;
        }
        getMap().setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                Log.i(TAG, "Zoom: " + String.valueOf(cameraPosition.zoom) + " Last zoom: " + String.valueOf(lastZoomLevel));
                if(cameraPosition.zoom > 16 && lastZoomLevel <= 16){
                    clearMap();
                    restartLoader(CAMPS);
                    restartLoader(ART);
                    restartLoader(EVENTS);
                }else if(cameraPosition.zoom < 16 && lastZoomLevel >= 16)
                    clearMap();
                lastZoomLevel = cameraPosition.zoom;
            }
        });

    }

    private void addMBTileOverlay(int MBTileAssetId){
        new AsyncTask<Integer, Void, Void>(){

            @Override
            protected Void doInBackground(Integer... params) {
                int MBTileAssetId = params[0];
                if(getActivity() != null)
                    FileUtils.copyMBTilesToSD(getActivity().getApplicationContext(), MBTileAssetId, Constants.MBTILE_DESTINATION);
                else{
                    Log.e(TAG, "getActivity() null on addMBTileOverlay");
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if(getActivity() == null)
                    return;
                String tilesPath = String.format("%s/%s/%s/%s", Environment.getExternalStorageDirectory().getAbsolutePath().toString(),
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

    private void navigateHome(){
        if(getMap().getMyLocation() == null){
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
        LatLng end = BurnState.getHomeLatLng(getActivity());
        if(getDistance(start, end) > 8046){
            new AlertDialog.Builder(getActivity())
                    .setTitle(getActivity().getString(R.string.youre_so_far))
                    .setMessage(String.format("It appears you're %d meters from home. Get closer to the burn before navigating home..", (int) getDistance(start, end)))
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            
                        }
                    })
                    .show();

            getMap().animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(new LatLng(Constants.MAN_LAT, Constants.MAN_LON)).zoom(15).build()));
            return;

        }

        getMap().addMarker(new MarkerOptions()
                .position(start)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .title("Current Location"));
        getMap().addMarker(new MarkerOptions()
                .position(end)
                .title("Home"));

        getMap().animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().bearing(getBearing(start, end)).target(getMidPoint(start,end)).tilt(45).zoom(15).build()));

        DecimalFormat twoDForm = new DecimalFormat("#");
        new Toast(getActivity()).makeText(getActivity(), String.format("%s meters from home",twoDForm.format(getDistance(start, end))), Toast.LENGTH_LONG).show();

    }

    private float getBearing(LatLng start, LatLng end){
        double longitude1 = start.longitude;
        double longitude2 = end.longitude;
        double latitude1 = Math.toRadians(start.latitude);
        double latitude2 = Math.toRadians(end.latitude);
        double longDiff= Math.toRadians(longitude2-longitude1);
        double y= Math.sin(longDiff)*Math.cos(latitude2);
        double x=Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);

        return (float) (Math.toDegrees(Math.atan2(y, x))+360)%360;
    }

    private LatLng getMidPoint(LatLng start, LatLng end){

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

    public void clearMap(){
        getMap().clear();
        addMBTileOverlay(R.raw.iburn);
        if(BurnState.getHomeLatLng(getActivity()) != null)
            addHomePin(BurnState.getHomeLatLng(getActivity()));
        state = 0;
    }

    public void mapMarker(MarkerOptions marker){
        getMap().addMarker(marker);
    }

    public void mapAndCenterOnMarker(MarkerOptions marker){
        latLngToCenterOn = marker.getPosition();
        mapMarker(marker);

    }

    static final String[] ART_PROJECTION = new String[] {
            ArtTable.COLUMN_NAME,
            ArtTable.COLUMN_ID,
            ArtTable.COLUMN_LATITUDE,
            ArtTable.COLUMN_LONGITUDE,
            ArtTable.COLUMN_FAVORITE
    };

    static final String[] EVENTS_PROJECTION = new String[] {
            EventTable.COLUMN_NAME,
            EventTable.COLUMN_ID,
            EventTable.COLUMN_LATITUDE,
            EventTable.COLUMN_LONGITUDE,
            EventTable.COLUMN_FAVORITE
    };

    static final String[] CAMPS_PROJECTION = new String[] {
            CampTable.COLUMN_NAME,
            CampTable.COLUMN_ID,
            CampTable.COLUMN_LATITUDE,
            CampTable.COLUMN_LONGITUDE,
            CampTable.COLUMN_FAVORITE
    };


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = null;
        Uri targetUri = null;
        if (mCurFilter != null) {
            switch(i){
                case ART:
                    projection = ART_PROJECTION;
                    targetUri = PlayaContentProvider.ART_SEARCH_URI;
                    break;
                case CAMPS:
                    projection = CAMPS_PROJECTION;
                    targetUri = PlayaContentProvider.CAMP_SEARCH_URI;
                    break;
                case EVENTS:
                    projection = EVENTS_PROJECTION;
                    targetUri = PlayaContentProvider.EVENT_SEARCH_URI;
                    break;
                case ALL:
                    targetUri = PlayaContentProvider.ALL_URI; // TODO
            }
            targetUri = Uri.withAppendedPath(targetUri, Uri.encode(mCurFilter));
        } else {
            switch(i){
                case ART:
                    projection = ART_PROJECTION;
                    targetUri = PlayaContentProvider.ART_URI;
                    break;
                case CAMPS:
                    projection = CAMPS_PROJECTION;
                    targetUri = PlayaContentProvider.CAMP_URI;
                    break;
                case EVENTS:
                    projection = EVENTS_PROJECTION;
                    targetUri = PlayaContentProvider.EVENT_URI;
                    break;
                case ALL:
                    targetUri = PlayaContentProvider.ALL_URI;
            }

        }

        String selection = null;
        String[] selectionArgs = null;


        if(limitListToFavorites){
            selection = ArtTable.COLUMN_FAVORITE;
            selectionArgs = new String[]{"1"};
        }

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        Log.i(TAG, "Creating loader with uri: " + targetUri.toString());
        return new CursorLoader(getActivity(), targetUri,
                projection, selection, selectionArgs,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Toast.makeText(getActivity(), "Mapping POIs...", Toast.LENGTH_SHORT).show();
        int id = cursorLoader.getId();
        GoogleMap map = getMap();
        //clearMap();
        MarkerOptions markerOptions;
        while(cursor.moveToNext()){
            markerOptions = new MarkerOptions().position(new LatLng(cursor.getDouble(cursor.getColumnIndex(ArtTable.COLUMN_LATITUDE)), cursor.getDouble(cursor.getColumnIndex(ArtTable.COLUMN_LONGITUDE))))
                    .title(cursor.getString(cursor.getColumnIndex(ArtTable.COLUMN_NAME)));

            switch(id){
                case ALL:
                    if(cursor.getFloat(cursor.getColumnIndex("art.latitude")) != 0){
                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.art_marker));
                    }else if(cursor.getFloat(cursor.getColumnIndex("camps.latitude")) != 0){
                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.camp_marker));
                    }else{
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
            map.addMarker(markerOptions);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    public void restartLoader(int type){
        state = type;
        getLoaderManager().restartLoader(type, null, this);
        //getLoaderManager().initLoader(type, null, this);
    }

    private void addHomePin(LatLng latLng){
        if(latLng == null)
            return;
        Marker marker = getMap().addMarker(new MarkerOptions()
                .position(latLng)
                .draggable(true)
                .title("Home")
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
                if(marker.getId().compareTo(homeMarkerId) == 0)
                    BurnState.setHomeLatLng(GoogleMapFragment.this.getActivity().getApplicationContext(), marker.getPosition());
            }
        });
    }
}
