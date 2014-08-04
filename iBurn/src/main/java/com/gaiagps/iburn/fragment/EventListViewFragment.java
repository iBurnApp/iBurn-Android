package com.gaiagps.iburn.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;

import com.gaiagps.iburn.adapters.EventCursorAdapter;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.PlayaContentProvider;

/**
 * Created by davidbrodsky on 8/3/13.
 * Base class for iBurn ListViews describing
 * Camps, Art, and Events. A subclass should provide
 * a value for PROJECTION, mAdapter, baseUri, and searchUri
 */
public class EventListViewFragment extends PlayaListViewFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "EventListViewFragment";

    static final String[] PROJECTION = new String[] {
            EventTable.id,
            EventTable.name,
            EventTable.startTime,
            EventTable.startTimePrint,
            EventTable.allDay,
            EventTable.favorite,
            EventTable.latitude,
            EventTable.longitude
    };

    SimpleCursorAdapter mAdapter;
    protected Uri baseUri = PlayaContentProvider.Events.EVENTS;     // Uris corresponding to PlayaContentProvider

    String ordering = EventTable.startTime + " ASC";                // How is the ListView ordered?
    String favoriteSelection = EventTable.favorite + " = ?";        // Statement to filter by favorites.

    public static EventListViewFragment newInstance() {
        return new EventListViewFragment();
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
        mAdapter = new EventCursorAdapter(getActivity(), null);
        super.onActivityCreated(savedInstanceState);
    }

}