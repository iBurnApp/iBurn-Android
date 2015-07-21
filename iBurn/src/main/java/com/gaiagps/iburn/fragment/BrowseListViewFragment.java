package com.gaiagps.iburn.fragment;

import android.database.Cursor;
import android.os.Bundle;
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
import com.gaiagps.iburn.adapters.AdapterItemSelectedListener;
import com.gaiagps.iburn.adapters.CursorRecyclerViewAdapter;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.adapters.EventCursorAdapter;
import com.gaiagps.iburn.adapters.PlayaItemCursorAdapter;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.gaiagps.iburn.view.BrowseListHeader;
import com.gaiagps.iburn.view.EventListHeader;
import com.squareup.sqlbrite.SqlBrite;

import java.util.ArrayList;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

/**
 * Fragment displaying Playa data
 * <p/>
 * Created by davidbrodsky on 8/3/13.
 */
public final class BrowseListViewFragment extends PlayaListViewFragment implements EventListHeader.PlayaListViewHeaderReceiver, BrowseListHeader.BrowseSelectionListener, AdapterItemSelectedListener, Subscriber {

    public static BrowseListViewFragment newInstance() {
        return new BrowseListViewFragment();
    }

    private ViewGroup eventListHeader;
    private BrowseListHeader.BrowseSelection categorySelection = BrowseListHeader.BrowseSelection.CAMPS;

    // Event filtering
    private String selectedDay = "8/25";
    private ArrayList<String> selectedTypes;

    @Override
    protected Subscription createSubscription() {
        switch (categorySelection) {

            case CAMPS:
                adapter = new PlayaItemCursorAdapter(getActivity(), null, Constants.PlayaItemType.CAMP, this);
                if (mRecyclerView != null) mRecyclerView.setAdapter(adapter);
                return DataProvider.getInstance(getActivity())
                        .flatMap(dataProvider -> dataProvider.observeTable(PlayaDatabase.CAMPS, adapter.getRequiredProjection()))
                        .doOnNext(query -> Timber.d("Got query"))
                        .map(SqlBrite.Query::run)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(cursor -> {
                                    Timber.d("Data onNext");
                                    onDataChanged(cursor);
                                },
                                throwable -> Timber.e(throwable, "Data onError"),
                                () -> Timber.d("Data onComplete"));


            case ART:
                adapter = new PlayaItemCursorAdapter(getActivity(), null, Constants.PlayaItemType.ART, this);
                if (mRecyclerView != null) mRecyclerView.setAdapter(adapter);
                return DataProvider.getInstance(getActivity())
                        .flatMap(dataProvider -> dataProvider.observeTable(PlayaDatabase.ART, adapter.getRequiredProjection()))
                        .map(SqlBrite.Query::run)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(cursor -> {
                                    Timber.d("Data onNext");
                                    onDataChanged(cursor);
                                },
                                throwable -> Timber.e(throwable, "Data onError"),
                                () -> Timber.d("Data onComplete"));


            case EVENT:
                adapter = new EventCursorAdapter(getActivity(), null, false, this);
                if (mRecyclerView != null) mRecyclerView.setAdapter(adapter);
                return DataProvider.getInstance(getActivity())
                        .flatMap(dataProvider -> dataProvider.observeEventsOnDayOfTypes(selectedDay, selectedTypes, adapter.getRequiredProjection()))
                        .map(SqlBrite.Query::run)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(cursor -> {
                                    Timber.d("Data onNext %d items", cursor.getCount());
                                    onDataChanged(cursor);
                                },
                                throwable -> Timber.e(throwable, "Data onError"),
                                () -> Timber.d("Data onComplete"));

        }

        return null;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_browse_list_view, container, false);
        eventListHeader = (ViewGroup) v.findViewById(R.id.eventHeader);
        mEmptyText = (TextView) v.findViewById(android.R.id.empty);
        mRecyclerView = ((RecyclerView) v.findViewById(android.R.id.list));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        ((BrowseListHeader) v.findViewById(R.id.header)).setBrowseSelectionListener(this);
        ((EventListHeader) v.findViewById(R.id.eventHeader)).setReceiver(this);
        return v;
    }

    @Override
    public void onSelectionChanged(String day, ArrayList<String> types) {
        selectedDay = day;
        selectedTypes = types;
        unsubscribeFromData();
        subscribeToData();
    }

    @Override
    public void onSelectionChanged(BrowseListHeader.BrowseSelection selection) {
        Timber.d("Browse selection made " + selection);

        switch (selection) {
            case CAMPS:
                eventListHeader.setVisibility(View.GONE);

                break;

            case ART:
                eventListHeader.setVisibility(View.GONE);

                break;

            case EVENT:
                eventListHeader.setVisibility(View.VISIBLE);
                break;
        }

        if (categorySelection != selection) {
            categorySelection = selection;
            unsubscribeFromData();
            subscribeToData();
        }
    }

    public void onDataChanged(Cursor newData) {
        super.onDataChanged(newData);

        AlphaAnimation fadeAnimation = new AlphaAnimation(0, 1);
        fadeAnimation.setDuration(250);
        fadeAnimation.setStartOffset(100);
        fadeAnimation.setFillAfter(true);
        fadeAnimation.setFillEnabled(true);
        mRecyclerView.startAnimation(fadeAnimation);
    }

    @Override
    protected CursorRecyclerViewAdapter getAdapter() {
        return new PlayaItemCursorAdapter(getActivity(), null, Constants.PlayaItemType.CAMP, this);
    }
}