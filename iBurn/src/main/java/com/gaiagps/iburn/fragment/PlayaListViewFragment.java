package com.gaiagps.iburn.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.Subscriber;
import com.gaiagps.iburn.activity.PlayaItemViewActivity;
import com.gaiagps.iburn.adapters.AdapterListener;
import com.gaiagps.iburn.adapters.CursorRecyclerViewAdapter;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaDatabase;

import rx.Subscription;
import timber.log.Timber;

/**
 * Base class for iBurn list views describing Camps, Art, and Events.
 * <p>
 * Created by davidbrodsky on 8/3/13.
 */
public abstract class PlayaListViewFragment extends Fragment implements AdapterListener, Subscriber {

    protected CursorRecyclerViewAdapter adapter;

    protected RecyclerView mRecyclerView;
    protected TextView mEmptyText;

    protected Subscription subscription;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = getAdapter();
        mRecyclerView.setAdapter(adapter);
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

    /**
     * Handle notifications that the data corresponding to our query changed,
     * and we should update UI
     */
    protected void onDataChanged(Cursor newData) {
        if (newData == null) {
            Timber.w("Got null data onDataChanged");
            return;
        }

        boolean adapterWasEmpty = adapter.getItemCount() == 0;

        if (adapterWasEmpty && newData.getCount() > 0) {
            // Fade in the initial data, but let updates happen without animation
            AlphaAnimation fadeAnimation = new AlphaAnimation(0, 1);
            fadeAnimation.setDuration(250);
            fadeAnimation.setStartOffset(100);
            fadeAnimation.setFillAfter(true);
            fadeAnimation.setFillEnabled(true);
            mRecyclerView.startAnimation(fadeAnimation);
        }

        if (newData.getCount() == 0) {
            setListShown(false);
        } else {
            setListShown(true);
        }

        Timber.d("%s, onDataChanged with %d items", getClass().getSimpleName(), newData.getCount());
        // Important to use changeCursor bc, unlike swapCursor, it can be executed asynchronously
        adapter.changeCursor(newData);
    }

    /**
     * Provide a {@link CursorRecyclerViewAdapter} to bind data to Recyclerview items.
     * This will be called during {@link #onActivityCreated(Bundle)}, so {@link #getActivity()}
     * is guaranteed to be non-null.
     */
    protected abstract CursorRecyclerViewAdapter getAdapter();

    protected abstract Subscription createSubscription();

    /**
     * Subscribe to the data describing this fragment's view
     */
    @Override
    public void subscribeToData() {
        if (subscription == null || subscription.isUnsubscribed()) {
            subscription = createSubscription();
        }
    }

    /**
     * Unsubscribe from the data describing this fragment's view
     */
    protected void unsubscribeFromData() {
        if (subscription != null && !subscription.isUnsubscribed())
            subscription.unsubscribe();
    }

    public void reSubscribeToData() {
        unsubscribeFromData();
        subscribeToData();
    }

    public String getEmptyText() {
        return getString(R.string.no_items_found);
    }

    public void setListShown(boolean doShow) {
        if (doShow) {
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        } else {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyText.setText(getEmptyText());
            mEmptyText.setVisibility(View.VISIBLE);
        }
    }

    public void setListShownNoAnimation(boolean doShow) {
        setListShown(doShow);
    }

    @Override
    public void onItemSelected(int modelId, Constants.PlayaItemType type) {

        Intent i = new Intent(getActivity(), PlayaItemViewActivity.class);
        i.putExtra(PlayaItemViewActivity.EXTRA_MODEL_ID, modelId);
        i.putExtra(PlayaItemViewActivity.EXTRA_MODEL_TYPE, type);
        getActivity().startActivity(i);
    }

    public void onItemFavoriteButtonSelected(int modelId, Constants.PlayaItemType type) {

        final String modelTable;
        switch (type) {
            case CAMP:
                modelTable = PlayaDatabase.CAMPS;
                break;
            case ART:
                modelTable = PlayaDatabase.ART;
                break;
            case EVENT:
                modelTable = PlayaDatabase.EVENTS;
                break;

            default:
                throw new IllegalArgumentException("Invalid type " + type);
        }

        DataProvider.getInstance(getActivity().getApplicationContext())
                .subscribe(dataProvider -> {
                    dataProvider.toggleFavorite(modelTable, modelId);
                }, throwable -> Timber.e(throwable, "Failed to update favorite"));
    }
}