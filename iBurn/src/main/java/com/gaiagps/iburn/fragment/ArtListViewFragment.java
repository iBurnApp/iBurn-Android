package com.gaiagps.iburn.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.widget.SimpleCursorAdapter;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.adapters.CursorRecyclerViewAdapter;
import com.gaiagps.iburn.adapters.PlayaItemCursorAdapter;
import com.gaiagps.iburn.database.PlayaContentProvider;

/**
 * Created by davidbrodsky on 8/3/13.
 * Base class for iBurn ListViews describing
 * Camps, Art, and Events. A subclass should provide
 * a value for PROJECTION, mAdapter, baseUri, and searchUri
 */
public class ArtListViewFragment extends PlayaListViewFragment
        implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String TAG = "ArtListViewFragment";

    PlayaItemCursorAdapter mAdapter;
    protected Uri baseUri   = PlayaContentProvider.Art.ART;                    // Uris corresponding to PlayaContentProvider

    public static ArtListViewFragment newInstance() {
        return new ArtListViewFragment();
    }

    protected Uri getBaseUri(){
        return baseUri;
    }

    protected CursorRecyclerViewAdapter getAdapter(){
        return mAdapter;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override public void onActivityCreated(Bundle savedInstanceState) {
        mAdapter = new PlayaItemCursorAdapter(getActivity(), null, Constants.PLAYA_ITEM_TYPE.ART);
        super.onActivityCreated(savedInstanceState);
    }
}