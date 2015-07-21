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
import android.widget.TextView;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.Subscriber;
import com.gaiagps.iburn.activity.PlayaItemViewActivity;
import com.gaiagps.iburn.adapters.AdapterItemSelectedListener;
import com.gaiagps.iburn.adapters.CursorRecyclerViewAdapter;
import com.gaiagps.iburn.adapters.DividerItemDecoration;

import rx.Subscription;
import timber.log.Timber;

/**
 * Base class for iBurn list views describing Camps, Art, and Events.
 * <p>
 * Created by davidbrodsky on 8/3/13.
 */
public abstract class PlayaListViewFragment extends Fragment implements AdapterItemSelectedListener, Subscriber {

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

    public void setEmptyText(CharSequence text) {
        setListShown(false);
        mEmptyText.setText(text);
    }

    public void setListShown(boolean doShow) {
        if (doShow) {
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        } else {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);
        }
    }

    public void setListShownNoAnimation(boolean doShow) {
        setListShown(doShow);
    }

    public void onItemSelected(int modelId, Constants.PlayaItemType type) {

        Intent i = new Intent(getActivity(), PlayaItemViewActivity.class);
        i.putExtra("model_id", modelId);
        i.putExtra("model_type", type);
        getActivity().startActivity(i);
    }
}