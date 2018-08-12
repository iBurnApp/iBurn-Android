package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.Subscriber;
import com.gaiagps.iburn.VerticalRecyclerViewFastScroller2;
import com.gaiagps.iburn.adapters.AdapterListener;
import com.gaiagps.iburn.adapters.AdapterUtils;
import com.gaiagps.iburn.adapters.AlphabeticalSectionIndexer;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.adapters.EventStartTimeSectionIndexer;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaItem;
import com.gaiagps.iburn.view.ArtListHeader;
import com.gaiagps.iburn.view.BrowseListHeader;
import com.gaiagps.iburn.view.EventListHeader;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;
import xyz.danoz.recyclerviewfastscroller.sectionindicator.title.SectionTitleIndicator;
import xyz.danoz.recyclerviewfastscroller.vertical.VerticalRecyclerViewFastScroller;

/**
 * Fragment displaying lists of Camps, Art, and Events
 * <p/>
 * Created by davidbrodsky on 8/3/13.
 */
public final class BrowseListViewFragment extends PlayaListViewFragment implements EventListHeader.PlayaListViewHeaderReceiver, BrowseListHeader.BrowseSelectionListener, AdapterListener, Subscriber, ArtListHeader.Listener {

    public static BrowseListViewFragment newInstance() {
        return new BrowseListViewFragment();
    }

//    private ViewGroup artListHeader;
    private ViewGroup eventListHeader;
    private BrowseListHeader.BrowseSelection categorySelection = BrowseListHeader.BrowseSelection.CAMPS;

    // Event filtering
    private String selectedDay = AdapterUtils.getCurrentOrFirstDayAbbreviation();
    private ArrayList<String> selectedTypes = null;
    private Boolean includeExpired = false;
    private String eventTiming = "timed";

    // Art filtering
    private boolean showAudioTourOnly;

    @Override
    protected Disposable createDisposable() {

        Observable<DataProvider> dataProvider = DataProvider.Companion.getInstance(getActivity().getApplicationContext());
        Observable<List<? extends PlayaItem>> playaItems = null;

        switch (categorySelection) {

            case CAMPS:
                playaItems = dataProvider
                        .flatMap(provider -> provider.observeCamps().toObservable()); // TODO : rm toObservable
                adapter.setSectionIndexer(new AlphabeticalSectionIndexer());
                break;

            case ART:
                playaItems = dataProvider
                        .flatMap(dp -> {
                            if (showAudioTourOnly) {
                                return dp.observeArtWithAudioTour().toObservable();
                            } else {
                                return dp.observeArt().toObservable();
                            }
                        });
                adapter.setSectionIndexer(new AlphabeticalSectionIndexer());

                break;

            case EVENT:
                playaItems = dataProvider
                        .flatMap(dp -> dp.observeEventsOnDayOfTypes(
                                selectedDay,
                                selectedTypes,
                                includeExpired,
                                eventTiming)
                                .toObservable());
                adapter.setSectionIndexer(new EventStartTimeSectionIndexer());
                break;
        }

        if (playaItems != null) {
            return playaItems.observeOn(AndroidSchedulers.mainThread())
                    .subscribe(items -> {
                                Timber.d("Data onNext");
                                onDataChanged(items);
                            },
                            throwable -> Timber.e(throwable, "Data onError"),
                            () -> Timber.d("Data onComplete"));
        } else {
            return null;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_browse_list_view, container, false);
        eventListHeader = v.findViewById(R.id.eventHeader);
//        artListHeader = v.findViewById(R.id.artHeader);
        mEmptyText = v.findViewById(android.R.id.empty);
        mRecyclerView = v.findViewById(android.R.id.list);

        VerticalRecyclerViewFastScroller2 fastScroller = (VerticalRecyclerViewFastScroller2) v.findViewById(R.id.fastScroller);
        SectionTitleIndicator sectionTitleIndicator =
                (SectionTitleIndicator) v.findViewById(R.id.fastScrollerSectionIndicator);

        // Connect the recycler to the scroller (to let the scroller scroll the list)
        fastScroller.setRecyclerView(mRecyclerView);

        // Connect the scroller to the recycler (to let the recycler scroll the scroller's handle)
        mRecyclerView.addOnScrollListener(fastScroller.getOnScrollListener());
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        fastScroller.setSectionIndicator(sectionTitleIndicator);

        setRecyclerViewLayoutManager(mRecyclerView);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        ((BrowseListHeader) v.findViewById(R.id.header)).setBrowseSelectionListener(this);
        ((EventListHeader) v.findViewById(R.id.eventHeader)).setReceiver(this);
//        ((ArtListHeader) v.findViewById(R.id.artHeader)).setListener(this);
        return v;
    }

    private void setRecyclerViewLayoutManager(RecyclerView recyclerView) {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (recyclerView.getLayoutManager() != null) {
            scrollPosition =
                    ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        }

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());

        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.scrollToPosition(scrollPosition);
    }

    @Override
    public void onSelectionChanged(String day, ArrayList<String> types,
                                   boolean expired,
                                   String timing) {
        selectedDay = day;
        selectedTypes = types;
        includeExpired = expired;
        eventTiming = timing;
        reSubscribeToData();
    }

    @Override
    public void onSelectionChanged(BrowseListHeader.BrowseSelection selection) {
        Timber.d("Browse selection made " + selection);

        switch (selection) {
            case CAMPS:
                eventListHeader.setVisibility(View.GONE);
//                artListHeader.setVisibility(View.GONE);
                break;

            case ART:
                eventListHeader.setVisibility(View.GONE);
//                artListHeader.setVisibility(View.VISIBLE);

                break;

            case EVENT:
                eventListHeader.setVisibility(View.VISIBLE);
//                artListHeader.setVisibility(View.GONE);
                break;
        }

        if (categorySelection != selection) {
            categorySelection = selection;
            unsubscribeFromData();
            // Clear items show new data is treated as initial with fade-in animation
            adapter.setItems(new ArrayList<>(0));
            subscribeToData();
        }
    }

    @Override
    public void onSelectionChanged(boolean showAudioTourOnly) {
        this.showAudioTourOnly = showAudioTourOnly;
        unsubscribeFromData();
        // Clear items show new data is treated as initial with fade-in animation
        adapter.setItems(new ArrayList<>(0));
        subscribeToData();
    }
}