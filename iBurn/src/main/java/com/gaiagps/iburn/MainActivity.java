package com.gaiagps.iburn;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.io.File;
import java.io.IOException;

public class MainActivity extends FragmentActivity {

    MapBoxOfflineTileProvider tileProvider;
    TileOverlay overlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addMBTileOverlay(R.raw.iburn2013_transparent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void addMBTileOverlay(int MBTileAssetId){
        // TODO: Get File reference to bundled .mbtiles
        /*
        Uri path = Uri.parse("android.resource://" + getPackageName() + "/" + MBTileAssetId);
        File MBTFile = new File(path.getPath());

        GoogleMap map = ((SupportMapFragment) this.getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        TileOverlayOptions opts = new TileOverlayOptions();

        tileProvider = new MapBoxOfflineTileProvider(MBTFile);
        opts.tileProvider(tileProvider);
        overlay = map.addTileOverlay(opts);
        */
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        tileProvider.close();
    }
    
}
