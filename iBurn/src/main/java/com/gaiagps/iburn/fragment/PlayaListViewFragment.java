package com.gaiagps.iburn.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.PlayaClient;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.SearchQueryProvider;
import com.gaiagps.iburn.Searchable;
import com.gaiagps.iburn.activity.PlayaItemViewActivity;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.location.DeviceLocation;
import com.gaiagps.iburn.view.PlayaListViewHeader;

import java.util.ArrayList;

/**
 * Created by davidbrodsky on 8/3/13.
 * Base class for iBurn ListViews describing
 * Camps, Art, and Events. A subclass should provide
 * a value for PROJECTION, mAdapter, baseUri, and searchUri
 */
public abstract class PlayaListViewFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor>, Searchable, PlayaListViewHeader.PlayaListViewHeaderReceiver {
    private static final String TAG = "PlayaListViewFragment";

    private static Location mLastLocation;
    private SORT mCurrentSort = SORT.NAME;
    String mCurFilter;                      // Search string to filter by

    static final String[] PROJECTION = new String[] {
            PlayaItemTable.id,
            PlayaItemTable.name,
            PlayaItemTable.favorite,
            PlayaItemTable.latitude,
            PlayaItemTable.longitude
    };

    private ListView mListView;
    private TextView mEmptyText;

    protected abstract Uri getBaseUri();

    protected abstract SimpleCursorAdapter getAdapter();

    protected String[] getProjection() {
        return PROJECTION;
    }

    protected String getOrdering() {
        switch (mCurrentSort) {
            case FAVORITE:
            case NAME:
                return PlayaItemTable.name + " ASC";
            case DISTANCE:
                // TODO: Dispatch a fresh location request and re-sort list?
                if (mLastLocation != null) {
                    String dateSearch = String.format("(%1$s - %2$,.2f) * (%1$s - %2$,.2f) + (%3$s - %4$,.2f) * (%3$s - %4$,.2f) ASC",
                            PlayaItemTable.latitude, mLastLocation.getLatitude(),
                            PlayaItemTable.longitude, mLastLocation.getLongitude());
                    Log.i(TAG, "returning location " + dateSearch);
                    return dateSearch;
                }
                return null;
        }
        throw new IllegalStateException("Unknown sort requested");
    }

    protected String getFavoriteSelection() {
        return PlayaItemTable.favorite + " = ?";
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(getAdapter());
        if (PlayaClient.isDbPopulated(getActivity())) {
            mCurFilter = ((SearchQueryProvider) getActivity()).getCurrentQuery();
            initLoader();
            if (mLastLocation == null) getLastDeviceLocation();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_playa_list_view, container, false);
        //super.onCreateView(inflater, container, savedInstanceState);
        mEmptyText = (TextView) v.findViewById(android.R.id.empty);
        mListView = ((ListView) v.findViewById(android.R.id.list));
        mListView.setEmptyView(mEmptyText);
        mListView.setDivider(new ColorDrawable(0x292929));
        mListView.setFastScrollEnabled(true);
        ((PlayaListViewHeader) v.findViewById(R.id.header)).setReceiver(this);
        return v;
    }

    protected boolean getShouldLimitSearchToFavorites() {
        return mCurrentSort == SORT.FAVORITE;
    }


    public void restartLoader() {
        Log.i(TAG, "restarting loader");
        getLoaderManager().restartLoader(0, null, PlayaListViewFragment.this);
    }

    public void initLoader() {
        Log.i(TAG, "init loader");
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        getAdapter().swapCursor(null);
    }

    ArrayList<String> selectionArgs = new ArrayList<>();

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader, so we don't care about the ID.
        // First, pick the base URI to use depending on whether we are
        // currently filtering.

        StringBuilder selection = new StringBuilder();
        selectionArgs.clear();

        if (getShouldLimitSearchToFavorites()) {
            appendSelection(selection, getFavoriteSelection(), "1");
        }

        if (mCurFilter != null && mCurFilter.length() > 0) {
            appendSelection(selection, PlayaItemTable.name + " LIKE ?", "%" + mCurFilter + "%");
        }

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        Log.i(TAG, "Creating loader with uri: " + getBaseUri().toString());
        return new CursorLoader(getActivity(),
                getBaseUri(),
                getProjection(),
                selection.toString(),
                selectionArgs.toArray(new String[selectionArgs.size()]),
                getOrdering());
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        getAdapter().swapCursor(data);
        if (data == null) {
            Log.e(TAG, "cursor is null onLoadFinished");
            return;
        }

        if (isResumed()) {
            // If searching, show no camps match query
            if (data.getCount() == 0 && !TextUtils.isEmpty(mCurFilter)) {
                setEmptyText(getActivity().getString(com.gaiagps.iburn.R.string.no_results));
            } else if (data.getCount() == 0) {
                if (getShouldLimitSearchToFavorites())
                    setEmptyText(getActivity().getString(com.gaiagps.iburn.R.string.mark_some_favorites));
                else
                    setEmptyText(getActivity().getString(com.gaiagps.iburn.R.string.no_items_found));
            }
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    /**
     * Override per AOSP bug:
     * https://code.google.com/p/android/issues/detail?id=21742
     */
    @Override
    public void setEmptyText(CharSequence text) {
        setListShown(false);
        mEmptyText.setText(text);
    }

    /**
     * Override per AOSP bug:
     * https://code.google.com/p/android/issues/detail?id=21742
     */
    @Override
    public void setListShown(boolean doShow) {
        if (doShow) {
            mListView.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        } else {
            mListView.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Override per AOSP bug:
     * https://code.google.com/p/android/issues/detail?id=21742
     */
    @Override
    public void setListShownNoAnimation(boolean doShow) {
        setListShown(doShow);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        int model_id = (Integer) v.getTag(R.id.list_item_related_model);
        Constants.PLAYA_ITEM playa_item = (Constants.PLAYA_ITEM) v.getTag(R.id.list_item_related_model_type);
        Intent i = new Intent(getActivity(), PlayaItemViewActivity.class);
        i.putExtra("model_id", model_id);
        i.putExtra("playa_item", playa_item);
        getActivity().startActivity(i);
    }

    @Override
    public void onSearchQueryRequested(String query) {
        Log.i(TAG, "got search query: " + query);
        mCurFilter = query;
        restartLoader();
    }

    private void appendSelection(StringBuilder builder, String selection, String value) {
        if (builder.length() > 0)
            builder.append(" AND ");
        builder.append(selection);
        selectionArgs.add(value);
    }

    @Override
    public void onSelectionChanged(SORT sort) {
        if (mCurrentSort == sort) return;
        mCurrentSort = sort;
        if (mCurrentSort == SORT.DISTANCE) {
            Log.i(TAG, "Got LOCATION sort request");
            getLastDeviceLocation();
        }
        restartLoader();
    }

    private void getLastDeviceLocation() {
        Log.i(TAG, "Getting device location");
        DeviceLocation.getLastKnownLocation(getActivity(), false, new DeviceLocation.LocationResult() {
            @Override
            public void gotLocation(Location location) {
                Log.i(TAG, "got device location!");
                mLastLocation = location;
            }
        });
    }
}