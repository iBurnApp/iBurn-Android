package com.gaiagps.iburn.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.activity.PlayaItemViewActivity;
import com.gaiagps.iburn.adapters.AdapterItemSelectedListener;
import com.gaiagps.iburn.adapters.CursorRecyclerViewAdapter;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.database.PlayaItemTable;

import java.util.ArrayList;

import rx.Subscription;
import timber.log.Timber;

/**
 * Base class for iBurn list views describing Camps, Art, and Events.
 *
 * Created by davidbrodsky on 8/3/13.
 */
public abstract class PlayaListViewFragment extends Fragment implements AdapterItemSelectedListener {

    // Location should be shared between all fragments
    protected static Location mLastLocation;

    static final String[] PROJECTION = new String[]{
            PlayaItemTable.id,
            PlayaItemTable.name,
            PlayaItemTable.favorite,
            PlayaItemTable.latitude,
            PlayaItemTable.longitude
    };

    protected CursorRecyclerViewAdapter adapter;

    protected RecyclerView mRecyclerView;
    protected TextView mEmptyText;

    protected Subscription subscription;

//    protected String getOrdering() {
//        switch (mCurrentSelection) {
//            case FAVORITE:
//                return PlayaItemTable.name + " ASC";
//            case NAME:  // Technically this should never be invoked
//            case DISTANCE:
//                // TODO: Dispatch a fresh location request and re-sort list?
//                if (mLastLocation != null) {
//                    String dateSearch = String.format(Locale.US, "(%1$s - %2$,.2f) * (%1$s - %2$,.2f) + (%3$s - %4$,.2f) * (%3$s - %4$,.2f) ASC",
//                            PlayaItemTable.latitude, mLastLocation.getLatitude(),
//                            PlayaItemTable.longitude, mLastLocation.getLongitude());
//                    Timber.d("returning location " + dateSearch);
//                    return dateSearch;
//                }
//                return null;
//        }
//        throw new IllegalStateException("Unknown sort requested");
//    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = getAdapter();
        mRecyclerView.setAdapter(adapter);
        subscription = subscribeToData();
//            mCurFilter = ((SearchQueryProvider) getActivity()).getCurrentQuery();
//            initLoader();
//            if (mLastLocation == null) getLastDeviceLocation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unsubscribeFromData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_playa_list_view, container, false);
        //super.onCreateView(inflater, container, savedInstanceState);
        mEmptyText = (TextView) v.findViewById(android.R.id.empty);
        mRecyclerView = ((RecyclerView) v.findViewById(android.R.id.list));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        return v;
    }

    ArrayList<String> selectionArgs = new ArrayList<>();

//    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//        // This is called when a new Loader needs to be created.  This
//        // sample only has one Loader, so we don't care about the ID.
//        // First, pick the base URI to use depending on whether we are
//        // currently filtering.
//
//        Loader<Cursor> loader = getLoader();
//
//        if (loader != null) return loader;
//
//        // If no loader provided, use a CursorLoader:
//
//        StringBuilder selection = new StringBuilder();
//        selectionArgs.clear();
//
//        if (mCurFilter != null && mCurFilter.length() > 0) {
//            appendSelection(selection, PlayaItemTable.name + " LIKE ?", "%" + mCurFilter + "%");
//        }

//        if (mCurrentSelection == Selection.DISTANCE) {
//            appendSelection(selection, PlayaItemTable.latitude + " != ?", "0");
//            appendSelection(selection, PlayaItemTable.longitude + " != ?", "0");
//        }
//
//        addCursorLoaderSelectionArgs(selection, selectionArgs);
//
//        // Now create and return a CursorLoader that will take care of
//        // creating a Cursor for the data being displayed.
//        Timber.d("Creating loader with uri: " + getBaseUri().toString() + " " + selection.toString() + selectionArgs);
//        return new CursorLoader(getActivity(),
//                getBaseUri(),
//                getProjection(),
//                selection.toString(),
//                selectionArgs.toArray(new String[selectionArgs.size()]),
//                getOrdering());
//    }

    protected void addCursorLoaderSelectionArgs(StringBuilder selection, ArrayList<String> selectionArgs) {
        // childclasses can add selections here
    }

    /**
     * Handle notifications that the data corresponding to our query changed,
     * and we should update UI
     */
    protected void onDataChanged(Cursor newData) {
        if (newData == null) {
            Timber.w("Got null data onDataChanged");
            return;
        }

        Timber.d("%s, onDataChanged with %d items", getClass().getSimpleName(), newData.getCount());
        adapter.changeCursor(newData);
    }

    /**
     * Provide a {@link CursorRecyclerViewAdapter} to bind data to Recyclerview items.
     * This will be called during {@link #onActivityCreated(Bundle)}, so {@link #getActivity()}
     * is guaranteed to be non-null.
     */
    protected abstract CursorRecyclerViewAdapter getAdapter();

    /**
     * Create a subscription to the query describing this fragment's data view.
     */
    protected abstract Subscription subscribeToData();

    /**
     * Unsubscribe from the query describing this fragment's data view
     */
    protected void unsubscribeFromData() {
        if (subscription != null && !subscription.isUnsubscribed())
            subscription.unsubscribe();
    }

//    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//        // Swap the new cursor in.  (The framework will take care of closing the
//        // old cursor once we return.)
//        getAdapter().changeCursor(data);
//        if (data == null) {
//            Timber.e("cursor is null onLoadFinished");
//            return;
//        }
//
//        if (data.getCount() == 0) {
//            // No items to display
//            if (!TextUtils.isEmpty(mCurFilter)) {
//                // This should only happen if the db isn't populated
//                setEmptyText(getActivity().getString(R.string.no_results));
//            } else {
//                if (getShouldLimitSearchToFavorites())
//                    setEmptyText(getActivity().getString(R.string.mark_some_favorites));
//                else
//                    setEmptyText(getActivity().getString(R.string.no_items_found));
//            }
//        } else {
//            // We have some results to show
//            if (isResumed()) setListShown(true);
//            else setListShownNoAnimation(true);
//        }
//    }

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

    public void onItemSelected(int modelId, Constants.PlayaItemType type) {

        Intent i = new Intent(getActivity(), PlayaItemViewActivity.class);
        i.putExtra("model_id", modelId);
        i.putExtra("model_type", type);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            View statusBar = getActivity().findViewById(android.R.id.statusBarBackground);
//            View navigationBar = getActivity().findViewById(android.R.id.navigationBarBackground);
//
//            List<Pair<View, String>> pairs = new ArrayList<>();
//            pairs.add(Pair.create(statusBar, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME));
//            pairs.add(Pair.create(navigationBar, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME));
//
//            Bundle options = ActivityOptions.makeSceneTransitionAnimation(getActivity(),
////                    null).toBundle();
//                    pairs.toArray(new Pair[pairs.size()])).toBundle();
//
//            Fade transition = new Fade();
//            transition.setDuration(250);
//            // with elements excluded, they flicker
////            transition.excludeTarget(android.R.id.statusBarBackground, true);
////            transition.excludeTarget(android.R.id.navigationBarBackground, true);
//            getActivity().getWindow().setAllowEnterTransitionOverlap(false);
//            getActivity().getWindow().setAllowReturnTransitionOverlap(false);
//            getActivity().getWindow().setExitTransition(transition);
//            getActivity().getWindow().setEnterTransition(transition);
//            getActivity().startActivity(i, options);
//
//        } else {
            getActivity().startActivity(i);
//        }
    }

    protected void appendSelection(StringBuilder builder, String selection, String value) {
        if (builder.length() > 0)
            builder.append(" AND ");
        builder.append(selection);
        selectionArgs.add(value);
    }

}