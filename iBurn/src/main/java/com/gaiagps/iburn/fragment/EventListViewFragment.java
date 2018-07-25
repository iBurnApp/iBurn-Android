package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.AdapterUtils;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.view.EventListHeader;

import java.util.ArrayList;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

/**
 * Fragment displaying all Playa Events
 * <p/>
 * Created by davidbrodsky on 8/3/13.
 */
public class EventListViewFragment extends PlayaListViewFragment implements EventListHeader.PlayaListViewHeaderReceiver {

    public static EventListViewFragment newInstance() {
        return new EventListViewFragment();
    }

    private String selectedDay = AdapterUtils.getCurrentOrFirstDayAbbreviation();
    private ArrayList<String> selectedTypes = null;
    private boolean includeExpired = false;
    private String eventTiming = "timed";

    @Override
    public Disposable createDisposable() {
        return DataProvider.Companion.getInstance(getActivity().getApplicationContext())
                .flatMap(dataProvider -> dataProvider.observeEventsOnDayOfTypes(
                        selectedDay,
                        selectedTypes,
                        includeExpired,
                        eventTiming)
                        .toObservable()) // TODO : RM toObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(events -> {
                            Timber.d("Data onNext %d items", events.size());
                            onDataChanged(events);
                        },
                        throwable -> Timber.e(throwable, "Data onError"),
                        () -> Timber.d("Data onComplete"));
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_event_list_view, container, false);
        mEmptyText = (TextView) v.findViewById(android.R.id.empty);
        mRecyclerView = ((RecyclerView) v.findViewById(android.R.id.list));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        ((EventListHeader) v.findViewById(R.id.header)).setReceiver(this);
        return v;
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
}