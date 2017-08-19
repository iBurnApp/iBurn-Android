package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gaiagps.iburn.CurrentDateProvider;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.adapters.PlayaItemAdapter;
import com.gaiagps.iburn.adapters.UpcomingEventsAdapter;
import com.gaiagps.iburn.database.DataProvider;
import com.tonicartos.superslim.LayoutManager;

import java.util.Calendar;
import java.util.Date;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Fragment displaying Playa Events happening right now / soon
 * <p/>
 * Created by davidbrodsky on 8/3/13.
 */
public class ExploreListViewFragment extends PlayaListViewFragment {

    public static ExploreListViewFragment newInstance() {
        return new ExploreListViewFragment();
    }

    @Override
    protected PlayaItemAdapter getAdapter() {
        return new UpcomingEventsAdapter(getContext().getApplicationContext(), this);
    }

    @Override
    public Disposable createDisposable() {
        Date now = CurrentDateProvider.getCurrentDate();

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(now);
        endCal.add(Calendar.HOUR, 7);
        Date end = endCal.getTime();

        return DataProvider.Companion.getInstance(getActivity().getApplicationContext())
                .subscribeOn(Schedulers.computation())
                .flatMap(dataProvider -> dataProvider.observeEventBetweenDates(now, end).toObservable())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(events -> {
                            Timber.d("Data onNext %d items", events.size());
                            onDataChanged(events);
                        },
                        throwable -> Timber.e(throwable, "Data onError"),
                        () -> Timber.d("Data onComplete"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_playa_list_view, container, false);
        mEmptyText = (TextView) v.findViewById(android.R.id.empty);
        mRecyclerView = ((RecyclerView) v.findViewById(android.R.id.list));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        // TODO : Implement sectioned adapter
        mRecyclerView.setLayoutManager(new LayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        return v;
    }

    @Override
    public String getEmptyText() {
        return getString(R.string.no_now_items);
    }
}