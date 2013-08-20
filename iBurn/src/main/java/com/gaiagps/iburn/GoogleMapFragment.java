package com.gaiagps.iburn;

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
    final int ART = 0;
    final int CAMPS = 1;
    final int EVENTS = 2;

    boolean showingMarkers = false;

    MapBoxOfflineTileProvider tileProvider;
    TileOverlay overlay;
    LatLng latLngToCenterOn;

    String mCurFilter;                      // Search string to filter by
    boolean limitListToFavorites = false;   // Limit display to favorites?

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
        inflater.inflate(R.menu.map, menu);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item){
       int id = item.getItemId();
       if(id == R.id.action_home){
           boundCameraByTwoPoints();
       }else if(showingMarkers){
           clearMap();
       }else{
           switch(id){
               case R.id.action_home:
                   boundCameraByTwoPoints();
                   break;
               case R.id.action_map_art:
                   initLoader(ART);
                   break;
               case R.id.action_map_camps:
                   initLoader(CAMPS);
                   break;
               case R.id.action_map_events:
                   initLoader(EVENTS);
                   break;
           }
       }
        return true;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addMBTileOverlay(R.raw.iburn);
        LatLng mStartLocation = new LatLng(Constants.MAN_LAT, Constants.MAN_LON);
        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(mStartLocation, 14));

        if(latLngToCenterOn != null){
            getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(latLngToCenterOn, 14));
            latLngToCenterOn = null;
        }

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

    private void boundCameraByTwoPoints(){
        LatLng start = new LatLng(Constants.MAN_LAT, Constants.MAN_LON);
        LatLng end = new LatLng(Constants.WOMAN_LAT, Constants.WOMAN_LON);
        // Mark start and end
        getMap().addMarker(new MarkerOptions()
                .position(start)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .title("Current Location"));
        getMap().addMarker(new MarkerOptions()
                .position(end)
                .title("Home"));


        // Draw line between them
        PolylineOptions pathOptions = new PolylineOptions()
                .add(start).add(end);
        getMap().addPolyline(pathOptions);

        final LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boundsBuilder.include(new LatLng(Constants.MAN_LAT, Constants.MAN_LON));
        boundsBuilder.include(new LatLng(Constants.WOMAN_LAT, Constants.WOMAN_LON));
        getMap().animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().bearing(getBearing(start, end)).target(getMidPoint(start,end)).tilt(45).zoom(15).build()));

        DecimalFormat twoDForm = new DecimalFormat("#");
        new Toast(getActivity()).makeText(getActivity(), String.format("%s meters from home",twoDForm.format(getDistance(start, end))), Toast.LENGTH_LONG).show();
        // getMap().animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 80));

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
        showingMarkers = false;
    }

    public void mapMarker(MarkerOptions marker){
        getMap().addMarker(marker);
    }

    public void mapAndCenterOnMarker(MarkerOptions marker){
        latLngToCenterOn = marker.getPosition();
        mapMarker(marker);

    }

    static final String[] PROJECTION = new String[] {
            ArtTable.COLUMN_NAME,
            ArtTable.COLUMN_ID,
            ArtTable.COLUMN_LATITUDE,
            ArtTable.COLUMN_LONGITUDE,
            ArtTable.COLUMN_FAVORITE
    };


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri targetUri = null;
        if (mCurFilter != null) {
            switch(i){
                case ART:
                    targetUri = PlayaContentProvider.ART_SEARCH_URI;
                    break;
                case CAMPS:
                    targetUri = PlayaContentProvider.CAMP_SEARCH_URI;
                    break;
                case EVENTS:
                    targetUri = PlayaContentProvider.EVENT_SEARCH_URI;
                    break;
            }
            targetUri = Uri.withAppendedPath(targetUri, Uri.encode(mCurFilter));
        } else {
            switch(i){
                case ART:
                    targetUri = PlayaContentProvider.ART_URI;
                    break;
                case CAMPS:
                    targetUri = PlayaContentProvider.CAMP_URI;
                    break;
                case EVENTS:
                    targetUri = PlayaContentProvider.EVENT_URI;
                    break;
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
                PROJECTION, selection, selectionArgs,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        int id = cursorLoader.getId();
        GoogleMap map = getMap();
        clearMap();
        MarkerOptions markerOptions;
        while(cursor.moveToNext()){
            markerOptions = new MarkerOptions().position(new LatLng(cursor.getDouble(cursor.getColumnIndex(ArtTable.COLUMN_LATITUDE)), cursor.getDouble(cursor.getColumnIndex(ArtTable.COLUMN_LONGITUDE))))
                    .title(cursor.getString(cursor.getColumnIndex(ArtTable.COLUMN_NAME)));

            switch(id){
                case ART:
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    break;
                case CAMPS:
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    break;
                case EVENTS:
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    break;
            }
            map.addMarker(markerOptions);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    public void initLoader(int type){
        showingMarkers = true;
        getLoaderManager().initLoader(type, null, this);
    }
}
