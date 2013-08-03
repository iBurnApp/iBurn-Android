package com.gaiagps.iburn;

import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
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
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void addMBTileOverlay(String pathToMBTileDatabase){
        GoogleMap map = ((SupportMapFragment) this.getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        TileOverlayOptions opts = new TileOverlayOptions();

        File MBTFile = new File(pathToMBTileDatabase);
        tileProvider = new MapBoxOfflineTileProvider(MBTFile);
        opts.tileProvider(tileProvider);
        overlay = map.addTileOverlay(opts);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        tileProvider.close();
    }
    
}
