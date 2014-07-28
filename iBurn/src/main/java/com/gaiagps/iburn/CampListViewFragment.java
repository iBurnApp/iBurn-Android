package com.gaiagps.iburn;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.gaiagps.iburn.adapters.CampCursorAdapter;
import com.gaiagps.iburn.database.CampTable;

/**
 * Created by davidbrodsky on 8/3/13.
 * Base class for iBurn ListViews describing
 * Camps, Art, and Events. A subclass should provide
 * a value for PROJECTION, mAdapter, baseUri, and searchUri
 */
public class CampListViewFragment extends PlayaListViewFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "CampListViewFragment";

    static final String[] PROJECTION = new String[] {
            CampTable.COLUMN_ID,
            CampTable.COLUMN_NAME,
            CampTable.COLUMN_LOCATION,
            CampTable.COLUMN_FAVORITE
    };

    SimpleCursorAdapter mAdapter;
    protected Uri baseUri = PlayaContentProvider.CAMP_URI;                    // Uris corresponding to PlayaContentProvider
    protected Uri searchUri = PlayaContentProvider.CAMP_SEARCH_URI;

    String ordering = CampTable.COLUMN_NAME + " ASC";               // How is the ListView ordered?
    String favoriteSelection = CampTable.COLUMN_FAVORITE + " = ?";  // Statement to filter by favorites.

    public static CampListViewFragment newInstance() {
        return new CampListViewFragment();
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
        mAdapter = new CampCursorAdapter(getActivity(), null);
        super.onActivityCreated(savedInstanceState);
    }

}