package com.gaiagps.iburn.fragment;


import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gaiagps.iburn.R;

/**
 * A placeholder Map fragment displayed while acquiring location permission
 */
public class MapPlaceHolderFragment extends Fragment {


    public MapPlaceHolderFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map_place_holder, container, false);
    }

}
