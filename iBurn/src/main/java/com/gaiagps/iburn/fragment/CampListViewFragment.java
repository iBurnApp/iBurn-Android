package com.gaiagps.iburn.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.widget.SimpleCursorAdapter;

import com.gaiagps.iburn.adapters.CampCursorAdapter;
import com.gaiagps.iburn.database.CampTable;
import com.gaiagps.iburn.database.PlayaContentProvider;

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
            CampTable.id,
            CampTable.name,
            CampTable.playaStreet,
            CampTable.playaHour,
            CampTable.playaMinute,
            CampTable.favorite
    };

    SimpleCursorAdapter mAdapter;
    protected Uri baseUri = PlayaContentProvider.Camps.CAMPS;                    // Uris corresponding to PlayaContentProvider

    public static CampListViewFragment newInstance() {
        return new CampListViewFragment();
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

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override public void onActivityCreated(Bundle savedInstanceState) {
        mAdapter = new CampCursorAdapter(getActivity(), null);
        super.onActivityCreated(savedInstanceState);
    }

}