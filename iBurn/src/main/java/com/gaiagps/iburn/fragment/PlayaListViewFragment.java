package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.gaiagps.iburn.IntentUtil;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.Subscriber;
import com.gaiagps.iburn.adapters.AdapterListener;
import com.gaiagps.iburn.adapters.CursorRecyclerViewAdapter;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.adapters.MultiTypePlayaItemAdapter;
import com.gaiagps.iburn.adapters.PlayaItemAdapter;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaItem;
import com.gaiagps.iburn.database.PlayaItemWithUserData;
import com.tonicartos.superslim.LayoutManager;

import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Base class for iBurn list views describing Camps, Art, and Events.
 * <p>
 * Created by davidbrodsky on 8/3/13.
 */
public abstract class PlayaListViewFragment extends Fragment implements AdapterListener, Subscriber {

    public static final String ARG_SCROLL_POS = "spos";

    protected PlayaItemAdapter adapter;

    protected RecyclerView mRecyclerView;
    protected TextView mEmptyText;

    protected Disposable disposable;

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
    public void onStart() {
        super.onStart();
        subscribeToData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("%s onCreateView", getClass().getSimpleName());
        View v = inflater.inflate(R.layout.fragment_playa_list_view, container, false);
        //super.onCreateView(inflater, container, savedInstanceState);
        mEmptyText = v.findViewById(android.R.id.empty);
        mRecyclerView = v.findViewById(android.R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        return v;
    }

    /**
     * Handle notifications that the data corresponding to our query changed,
     * and we should update UI
     */
    protected void onDataChanged(List<? extends PlayaItemWithUserData> newData) {
        if (newData == null) {
            Timber.w("Got null data onDataChanged");
            return;
        }

        prepareForNewData(newData);

        // Important to use changeCursor bc, unlike swapCursor, it can be executed asynchronously
        adapter.setItems(newData);

        restoreScrollPosition();
    }

    protected void onDataChanged(DataProvider.SectionedPlayaItems newData) {
        if (newData == null) {
            Timber.w("Got null data onDataChanged");
            return;
        }

        prepareForNewData(newData.getData());

        // Important to use changeCursor bc, unlike swapCursor, it can be executed asynchronously
        if (adapter instanceof MultiTypePlayaItemAdapter) {
            ((MultiTypePlayaItemAdapter) adapter).setSectionedItems(newData);
        } else {
            Timber.w("Sectioned data provided but adapter does not seem to support sections");
            adapter.setItems(newData.getData());
        }
        restoreScrollPosition();
    }

    private void prepareForNewData(List<? extends PlayaItemWithUserData> newData) {
        // Also save the scroll position here so updating a data list doesn't lose position
        lastScrollPos = getScrollPosition();

        Timber.d("%s onDataChanged Had %d items. Now %d items", getClass().getSimpleName(),
                adapter.getItemCount(), newData.size());

        boolean adapterWasEmpty = adapter.getItemCount() == 0;

        if (adapterWasEmpty && newData.size() > 0) {
            // Fade in the initial data, but let updates happen without animation
            AlphaAnimation fadeAnimation = new AlphaAnimation(0, 1);
            fadeAnimation.setDuration(250);
            fadeAnimation.setStartOffset(100);
            fadeAnimation.setFillAfter(true);
            fadeAnimation.setFillEnabled(true);
            mRecyclerView.startAnimation(fadeAnimation);
        }

        if (newData.size() == 0) {
            setListShown(false);
        } else {
            setListShown(true);
        }
    }

    private void restoreScrollPosition() {
        Timber.d("%s Scrolling to prior scroll position %d", getClass().getSimpleName(), lastScrollPos);
        mRecyclerView.scrollToPosition(lastScrollPos);
    }

    /**
     * Provide a {@link CursorRecyclerViewAdapter} to bind data to Recyclerview items.
     * This will be called during {@link #onActivityCreated(Bundle)}, so {@link #getActivity()}
     * is guaranteed to be non-null.
     */
    protected PlayaItemAdapter getAdapter() {
        return new PlayaItemAdapter(getContext(), this);
    }

    protected abstract Disposable createDisposable();

    /**
     * Subscribe to the data describing this fragment's view
     */
    @Override
    public void subscribeToData() {
        if (disposable == null || disposable.isDisposed()) {
            disposable = createDisposable();
        }
    }

    /**
     * Unsubscribe from the data describing this fragment's view
     */
    protected void unsubscribeFromData() {
        if (disposable != null && !disposable.isDisposed())
            disposable.dispose();
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
    public void onItemSelected(PlayaItemWithUserData item) {
        IntentUtil.viewItemDetail(getActivity(), item.getItem());
    }

    @Override
    public void onItemFavoriteButtonSelected(PlayaItem item) {
        Timber.d("onItemFavoriteButtonSelected for %s", item.playaId);
        DataProvider.Companion.getInstance(getActivity().getApplicationContext())
                .observeOn(Schedulers.io())
                .subscribe(dataProvider -> {
                    dataProvider.toggleFavorite(item);
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