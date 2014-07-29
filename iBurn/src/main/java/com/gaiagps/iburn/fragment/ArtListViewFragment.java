package com.gaiagps.iburn.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.widget.SimpleCursorAdapter;

import com.gaiagps.iburn.database.PlayaContentProvider;
import com.gaiagps.iburn.adapters.ArtCursorAdapter;
import com.gaiagps.iburn.database.ArtTable;

/**
 * Created by davidbrodsky on 8/3/13.
 * Base class for iBurn ListViews describing
 * Camps, Art, and Events. A subclass should provide
 * a value for PROJECTION, mAdapter, baseUri, and searchUri
 */
public class ArtListViewFragment extends PlayaListViewFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArtListViewFragment";

    static final String[] PROJECTION = new String[] {
            ArtTable.COLUMN_ID,
            ArtTable.COLUMN_NAME,
            ArtTable.COLUMN_ARTIST,
            ArtTable.COLUMN_FAVORITE
    };

    SimpleCursorAdapter mAdapter;
    protected Uri baseUri = PlayaContentProvider.ART_URI;                    // Uris corresponding to PlayaContentProvider
    protected Uri searchUri = PlayaContentProvider.ART_SEARCH_URI;

    String ordering = ArtTable.COLUMN_NAME + " ASC";               // How is the ListView ordered?
    String favoriteSelection = ArtTable.COLUMN_FAVORITE + " = ?";  // Statement to filter by favorites.

    public static ArtListViewFragment newInstance() {
        return new ArtListViewFragment();
    }

    protected Uri getBaseUri(){
        return baseUri;
    }

    protected Uri getSearchUri(){
        return searchUri;
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