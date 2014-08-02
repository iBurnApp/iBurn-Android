package com.gaiagps.iburn.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
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

import java.util.ArrayList;

/**
 * Created by davidbrodsky on 8/3/13.
 * Base class for iBurn ListViews describing
 * Camps, Art, and Events. A subclass should provide
 * a value for PROJECTION, mAdapter, baseUri, and searchUri
 */
public abstract class PlayaListViewFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor>, Searchable {
    private static final String TAG = "PlayaListViewFragment";

    TextView emptyText;                     // TextView to display when no ListView items present
    String mCurFilter;                      // Search string to filter by

    protected abstract Uri getBaseUri();

    protected abstract SimpleCursorAdapter getAdapter();

    protected abstract String[] getProjection();

    protected abstract String getOrdering();

    protected abstract String getFavoriteSelection();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        inflater.inflate(R.menu.list, menu);
//        super.onCreateOptionsMenu(menu, inflater);
//    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(getAdapter());
        if (PlayaClient.isDbPopulated(getActivity())) {
            mCurFilter = ((SearchQueryProvider) getActivity()).getCurrentQuery();
            initLoader();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        emptyText = (TextView) v.findViewById(android.R.id.empty);
        ((ListView) v.findViewById(android.R.id.list)).setDivider(new ColorDrawable(0x292929));
        ((ListView) v.findViewById(android.R.id.list)).setFastScrollEnabled(true);
        return v;
    }

    protected boolean getShouldLimitSearchToFavorites() {
        // TODO
        return false;
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

        if (isResumed() && emptyText != null) {
            // If searching, show no camps match query
            if (data.getCount() == 0 && mCurFilter != null && mCurFilter != "") {
                emptyText.setVisibility(View.VISIBLE);
                emptyText.setText(getActivity().getString(com.gaiagps.iburn.R.string.no_results));
            } else if (data.getCount() == 0)
                if (getShouldLimitSearchToFavorites())
                    emptyText.setText(getActivity().getString(com.gaiagps.iburn.R.string.mark_some_favorites));
                else
                    emptyText.setText(getActivity().getString(com.gaiagps.iburn.R.string.no_items_found));
            else
                emptyText.setVisibility(View.GONE);

            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
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
}