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
import android.widget.Toast;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.IntentUtil;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.Subscriber;
import com.gaiagps.iburn.activity.PlayaItemViewActivity;
import com.gaiagps.iburn.adapters.AdapterListener;
import com.gaiagps.iburn.adapters.CursorRecyclerViewAdapter;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.tonicartos.superslim.LayoutManager;

import rx.Subscription;
import timber.log.Timber;

/**
 * Base class for iBurn list views describing Camps, Art, and Events.
 * <p>
 * Created by davidbrodsky on 8/3/13.
 */
public abstract class PlayaListViewFragment extends Fragment implements AdapterListener, Subscriber {

    public static final String ARG_SCROLL_POS = "spos";

    protected CursorRecyclerViewAdapter adapter;

    protected RecyclerView mRecyclerView;
    protected TextView mEmptyText;

    protected Subscription subscription;

    private int lastScrollPos;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            lastScrollPos = savedInstanceState.getInt(ARG_SCROLL_POS, 0);
            Timber.d("%s onCreate with scroll ps %d", getClass().getSimpleName(), lastScrollPos);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = getAdapter();
        mRecyclerView.setAdapter(adapter);
    }

    public void onSaveInstanceState(Bundle outState) {
        // Save the scroll position with this instance and in Bundle for retrieval whether or not
        // this instance is recreated
        lastScrollPos = getScrollPosition();
        outState.putInt(ARG_SCROLL_POS, lastScrollPos);
        Timber.d("%s onSaveInstanceState with scroll pos %d", getClass().getSimpleName(), lastScrollPos);
    }

    @Override
    public void onStop() {
        super.onStop();
        unsubscribeFromData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("%s onCreateView", getClass().getSimpleName());
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

        Timber.d("%s onDataChanged Had %d items. Now %d items", getClass().getSimpleName(),
                adapter.getItemCount(), newData.getCount());

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

        // Important to use changeCursor bc, unlike swapCursor, it can be executed asynchronously
        adapter.changeCursor(newData);

        Timber.d("%s Scrolling to prior scroll position %d", getClass().getSimpleName(), lastScrollPos);
        mRecyclerView.scrollToPosition(lastScrollPos);
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
            mRecyclerView.setVisibility(View.INVISIBLE);  // Using GONE caused the recyclerview to improperly render after a transition to the empty state
            mEmptyText.setText(getEmptyText());
            mEmptyText.setVisibility(View.VISIBLE);
        }
    }

    public void setListShownNoAnimation(boolean doShow) {
        setListShown(doShow);
    }

    @Override
    public void onItemSelected(int modelId, Constants.PlayaItemType type) {
        IntentUtil.viewItemDetail(getActivity(), modelId, type);
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

    /**
     * @return the current scroll position, or 0 if no scrolling has occurred
     */
    private int getScrollPosition() {
        if (mRecyclerView != null) {
            int scrollPos = 0;
            if (mRecyclerView.getLayoutManager() instanceof LayoutManager) {
                scrollPos = ((LayoutManager) mRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
            } else if (mRecyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                scrollPos = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
            }

            Timber.d("%s onSaveInstanceState scrollPos %d", getClass().getSimpleName(), scrollPos);
            return scrollPos;
        }
        return 0;
    }
}