package com.gaiagps.iburn.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.widget.SimpleCursorAdapter;

import com.gaiagps.iburn.adapters.ArtCursorAdapter;
import com.gaiagps.iburn.database.ArtTable;
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

    static final String[] PROJECTION = new String[] {
            ArtTable.id,
            ArtTable.name,
            ArtTable.artist,
            ArtTable.favorite
    };

    SimpleCursorAdapter mAdapter;
    protected Uri baseUri   = PlayaContentProvider.Art.ART;                    // Uris corresponding to PlayaContentProvider

    String ordering = ArtTable.name + " ASC";               // How is the ListView ordered?
    String favoriteSelection = ArtTable.favorite + " = ?";  // Statement to filter by favorites.

    public static ArtListViewFragment newInstance() {
        return new ArtListViewFragment();
    }

    protected Uri getBaseUri(){
        return baseUri;
    }

    protected SimpleCursorAdapter getAdapter(){
        return mAdapter;
    }

    protected String[] getProjection(){
        return PROJECTION;
    }

    protected String getOrdering(){
        return ordering;
    }

    protected String getFavoriteSelection(){
        return favoriteSelection;
    }


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override public void onActivityCreated(Bundle savedInstanceState) {
        mAdapter = new ArtCursorAdapter(getActivity(), null);
        super.onActivityCreated(savedInstanceState);
    }
}