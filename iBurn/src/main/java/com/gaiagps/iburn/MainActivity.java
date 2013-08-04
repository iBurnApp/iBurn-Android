package com.gaiagps.iburn;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.io.File;

public class MainActivity extends FragmentActivity {

    MapBoxOfflineTileProvider tileProvider;
    TileOverlay overlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addMBTileOverlay(R.raw.iburn);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void addMBTileOverlay(int MBTileAssetId){
        new AsyncTask<Void, Void, Void>(){

            @Override
            protected Void doInBackground(Void... params) {
                FileUtils.copyMBTilesToSD(getApplicationContext(), R.raw.iburn, Constants.MBTILE_DESTINATION);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                String tilesPath = String.format("%s/%s/%s/%s",Environment.getExternalStorageDirectory().getAbsolutePath().toString(),
                        Constants.IBURN_ROOT, Constants.TILES_DIR, Constants.MBTILE_DESTINATION);
                File MBTFile = new File(tilesPath);
                GoogleMap map = ((SupportMapFragment) MainActivity.this.getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
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

    @Override
    protected void onDestroy(){
        super.onDestroy();
        tileProvider.close();
    }
    
}
