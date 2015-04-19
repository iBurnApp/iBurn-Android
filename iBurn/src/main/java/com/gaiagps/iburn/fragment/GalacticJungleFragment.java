package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liorsaar on 4/18/15.
 */
public class GalacticJungleFragment extends GoogleMapFragment {
    private static final String TAG = "GalacticJungleFragment";

    public static GalacticJungleFragment newInstance() {
        return new GalacticJungleFragment();
    }

    public GalacticJungleFragment() {
        super();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View parent = super.onCreateView(inflater, container, savedInstanceState);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initGJ();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private List<Marker> markers;

    private void initGJ() {
//        40.7888
//        -119.20315
//        lat=40.7843037788468
//        lon=-119.19632155448197
        final LatLng test1 = new LatLng(40.7888, -119.20315);
        final LatLng test2 = new LatLng(40.7843, 119.1963);

        getMap().addMarker(new MarkerOptions()
                .position(test1)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .title("Current Location"));

        markers = new ArrayList<Marker>();

        for (int i = 1; i < 5 ; i++) {
            LatLng ll = new LatLng(40.7888+i*0.003, -119.20315+i*0.003);

            MarkerOptions mops = new MarkerOptions()
                    .position(ll)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    .title("** " + i);
            markers.add( getMap().addMarker(mops) );
        }

        new Handler().postDelayed( new Runnable() {
            @Override
            public void run() {
                for (Marker marker : markers) {
                    marker.setPosition(test1);
                }
            }
        }, 5000);
    }

}
