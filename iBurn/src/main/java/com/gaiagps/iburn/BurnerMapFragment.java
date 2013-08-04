package com.gaiagps.iburn;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.io.File;

/**
 * Created by davidbrodsky on 8/3/13.
 */
public class BurnerMapFragment extends SupportMapFragment{
    private static final String TAG = "BurnerMapFragment";

    MapBoxOfflineTileProvider tileProvider;
    TileOverlay overlay;

    public BurnerMapFragment() {
        super();
    }

    @Override
    public void onDestroy(){
        tileProvider.close();
    }

    @Override
    public void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        addMBTileOverlay(R.raw.iburn);
    }

    private void addMBTileOverlay(int MBTileAssetId){
        new AsyncTask<Void, Void, Void>(){

            @Override
            protected Void doInBackground(Void... params) {
                FileUtils.copyMBTilesToSD(getActivity().getApplicationContext(), R.raw.iburn, Constants.MBTILE_DESTINATION);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
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

                LatLng mStartLocation = new LatLng(Constants.MAN_LAT, Constants.MAN_LON);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(mStartLocation, 10));

            }
        }.execute();


    }


}
