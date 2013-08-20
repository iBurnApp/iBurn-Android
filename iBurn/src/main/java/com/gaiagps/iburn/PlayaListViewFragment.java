package com.gaiagps.iburn;

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

/**
 * Created by davidbrodsky on 8/3/13.
 * Base class for iBurn ListViews describing
 * Camps, Art, and Events. A subclass should provide
 * a value for PROJECTION, mAdapter, baseUri, and searchUri
 */
public abstract class PlayaListViewFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "PlayaListViewFragment";

    protected View popupView;               // Popup showing item detail
    TextView emptyText;                     // TextView to display when no ListView items present
    String mCurFilter;                      // Search string to filter by
    boolean limitListToFavorites = false;   // Limit display to favorites?

    // ****************************
    // Provided by subclass:

    // An array of the column names to fetch for populating the ListView via the appropriate adapter
    protected static final String[] PROJECTION = null;

    // Should be CampCursorAdapter, EventCursorAdapter, or ArtCursorAdapter
    // Set onActivityCreated(...)
    protected SimpleCursorAdapter mAdapter;

    // Uri's corresponding to PlayaContentProvider
    protected Uri baseUri;
    protected Uri searchUri;

    // How is the ListView ordered?
    protected String ordering;
    // Statement to filter by favorites.
    protected String favoriteSelection;

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
        super.onActivityCreated(savedInstanceState);
        Constants.TAB_TYPE tabType = (Constants.TAB_TYPE) this.getArguments().getSerializable("type");
        setListAdapter(getAdapter());
        initLoader();
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v =  super.onCreateView(inflater, container, savedInstanceState);
        emptyText = (TextView) v.findViewById(android.R.id.empty);
        ((ListView) v.findViewById(android.R.id.list)).setDivider(new ColorDrawable(0x292929));

        return v;
    }

    public void initLoader(){
        getLoaderManager().initLoader(0, null, this);
    }

    public void restartLoader(){
        getLoaderManager().restartLoader(0, null, PlayaListViewFragment.this);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        getAdapter().swapCursor(null);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader, so we don't care about the ID.
        // First, pick the base URI to use depending on whether we are
        // currently filtering.

        Uri targetUri;
        if (mCurFilter != null) {
            targetUri = Uri.withAppendedPath(getSearchUri(), Uri.encode(mCurFilter));
        } else {
            targetUri = getBaseUri();
            ordering = getOrdering();
            //ordering = CampTable.COLUMN_NAME + " ASC";
        }

        String selection = null;
        String[] selectionArgs = null;

        if(limitListToFavorites){
            selection = getFavoriteSelection();
            selectionArgs = new String[]{"1"};
        }

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        Log.i(TAG, "Creating loader with uri: " + targetUri.toString());
        return new CursorLoader(getActivity(), targetUri,
                getProjection(), selection, selectionArgs,
                ordering);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        getAdapter().swapCursor(data);
        if(data == null){
            Log.e(TAG, "cursor is null onLoadFinished");
            return;
        }

        if (isResumed() && emptyText != null) {
            // If searching, show no camps match query
            if (data.getCount() == 0 && mCurFilter != null && mCurFilter != "" ){
                emptyText.setVisibility(View.VISIBLE);
                emptyText.setText(getActivity().getString(com.gaiagps.iburn.R.string.no_results));
            }
            else if(data.getCount() == 0)
                if(limitListToFavorites)
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
    public void onListItemClick (ListView l, View v, int position, long id){
        int model_id = (Integer) v.getTag(R.id.list_item_related_model);
        Constants.PLAYA_ITEM playa_item = (Constants.PLAYA_ITEM) v.getTag(R.id.list_item_related_model_type);
        Intent i = new Intent(getActivity(), PlayaItemViewActivity.class);
        i.putExtra("model_id", model_id);
        i.putExtra("playa_item", playa_item);
        getActivity().startActivity(i);

    }
}