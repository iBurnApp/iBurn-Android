package com.gaiagps.iburn.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.gaiagps.iburn.adapters.CursorRecyclerViewAdapter;
import com.gaiagps.iburn.adapters.PlayaItemCursorAdapter;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.location.DeviceLocation;
import com.gaiagps.iburn.view.PlayaListViewHeader;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by davidbrodsky on 8/3/13.
 * Base class for iBurn ListViews describing
 * Camps, Art, and Events. A subclass should provide
 * a value for PROJECTION, mAdapter, baseUri, and searchUri
 */
public abstract class PlayaListViewFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, Searchable, PlayaListViewHeader.PlayaListViewHeaderReceiver {
    private static final String TAG = "PlayaListViewFragment";

    protected static Location mLastLocation;
    protected SORT mCurrentSort = SORT.NAME;
    String mCurFilter;                      // Search string to filter by

    static final String[] PROJECTION = new String[] {
            PlayaItemTable.id,
            PlayaItemTable.name,
            PlayaItemTable.favorite,
            PlayaItemTable.latitude,
            PlayaItemTable.longitude
    };

    protected RecyclerView mRecyclerView;
    protected TextView mEmptyText;

    protected abstract Uri getBaseUri();

    protected abstract CursorRecyclerViewAdapter getAdapter();

    protected String[] getProjection() {
        return PROJECTION;
    }

    protected String getOrdering() {
        switch (mCurrentSort) {
            case FAVORITE:
                return PlayaItemTable.name + " ASC";
            case NAME:  // Technically this should never be invoked
            case DISTANCE:
                // TODO: Dispatch a fresh location request and re-sort list?
                if (mLastLocation != null) {
                    String dateSearch = String.format(Locale.US, "(%1$s - %2$,.2f) * (%1$s - %2$,.2f) + (%3$s - %4$,.2f) * (%3$s - %4$,.2f) ASC",
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
//        setListAdapter(getAdapter());
        mRecyclerView.setAdapter(getAdapter());
        if (PlayaClient.isDbPopulated(getActivity())) {
            mCurFilter = ((SearchQueryProvider) getActivity()).getCurrentQuery();
            mCurrentSort = SORT.NAME;
            initLoader();
            if (mLastLocation == null) getLastDeviceLocation();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_playa_list_view, container, false);
        //super.onCreateView(inflater, container, savedInstanceState);
        mEmptyText = (TextView) v.findViewById(android.R.id.empty);
        mRecyclerView = ((RecyclerView) v.findViewById(android.R.id.list));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));


//        mListView.setOnItemLongClickListener(AdapterUtils.mListItemLongClickListener);
//        mListView.setEmptyView(mEmptyText);
//        mListView.setFastScrollEnabled(true);
//        mListView.setDividerHeight(10);
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

        if (mCurrentSort == SORT.DISTANCE) {
            appendSelection(selection, PlayaItemTable.latitude + " != ?", "0");
            appendSelection(selection, PlayaItemTable.longitude + " != ?", "0");
        }

        addCursorLoaderSelectionArgs(selection, selectionArgs);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        Log.i(TAG, "Creating loader with uri: " + getBaseUri().toString() + " " + selection.toString() + selectionArgs);
        return new CursorLoader(getActivity(),
                getBaseUri(),
                getProjection(),
                selection.toString(),
                selectionArgs.toArray(new String[selectionArgs.size()]),
                getOrdering());
    }

    protected void addCursorLoaderSelectionArgs(StringBuilder selection, ArrayList<String> selectionArgs) {
        // childclasses can add selections here
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        getAdapter().swapCursor(data);
        if (data == null) {
            Log.e(TAG, "cursor is null onLoadFinished");
            return;
        }

        if (data.getCount() == 0) {
            // No items to display
            if (!TextUtils.isEmpty(mCurFilter)) {
                // This should only happen if the db isn't populated
                setEmptyText(getActivity().getString(com.gaiagps.iburn.R.string.no_results));
            } else {
                if (getShouldLimitSearchToFavorites())
                    setEmptyText(getActivity().getString(com.gaiagps.iburn.R.string.mark_some_favorites));
                else
                    setEmptyText(getActivity().getString(com.gaiagps.iburn.R.string.no_items_found));
            }
        } else {
            // We have some results to show
            if   (isResumed()) setListShown(true);
            else setListShownNoAnimation(true);
        }
    }

    /**
     * Override per AOSP bug:
     * https://code.google.com/p/android/issues/detail?id=21742
     */
    public void setEmptyText(CharSequence text) {
        setListShown(false);
        mEmptyText.setText(text);
    }

    /**
     * Override per AOSP bug:
     * https://code.google.com/p/android/issues/detail?id=21742
     */
    public void setListShown(boolean doShow) {
        if (doShow) {
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        } else {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Override per AOSP bug:
     * https://code.google.com/p/android/issues/detail?id=21742
     */
    public void setListShownNoAnimation(boolean doShow) {
        setListShown(doShow);
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        int model_id = (Integer) v.getTag(R.id.list_item_related_model);
        Constants.PLAYA_ITEM_TYPE itemType = (Constants.PLAYA_ITEM_TYPE) v.getTag(R.id.list_item_related_model_type);
        Intent i = new Intent(getActivity(), PlayaItemViewActivity.class);
        i.putExtra("model_id", model_id);
        i.putExtra("playa_item", itemType);
        getActivity().startActivity(i);
    }

    @Override
    public void onSearchQueryRequested(String query) {
        Log.i(TAG, "got search query: " + query);
        mCurFilter = query;
        restartLoader();
    }

    protected void appendSelection(StringBuilder builder, String selection, String value) {
        if (builder.length() > 0)
            builder.append(" AND ");
        builder.append(selection);
        selectionArgs.add(value);
    }

    @Override
    public void onSelectionChanged(SORT sort, String day, ArrayList<String> types) {
        if (mCurrentSort == sort) return;
        mCurrentSort = sort;
        if (mCurrentSort == SORT.DISTANCE) {
            Log.i(TAG, "Got LOCATION sort request");
            getLastDeviceLocation();
        }
        restartLoader();
    }

    protected void getLastDeviceLocation() {
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