package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gaiagps.iburn.CurrentDateProvider;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.CursorRecyclerViewAdapter;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.adapters.EventSectionedCursorAdapter;
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.squareup.sqlbrite.SqlBrite;
import com.tonicartos.superslim.LayoutManager;

import java.util.Calendar;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
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

    protected CursorRecyclerViewAdapter getAdapter() {
        return new EventSectionedCursorAdapter(getActivity(), null, this);
    }

    @Override
    public Subscription createSubscription() {
        Calendar modifiedDate = Calendar.getInstance();
        modifiedDate.setTime(CurrentDateProvider.getCurrentDate());
        String lowerBoundDateStr = PlayaDateTypeAdapter.iso8601Format.format(modifiedDate.getTime());
        modifiedDate.add(Calendar.HOUR, 7);
        String upperBoundDateStr = PlayaDateTypeAdapter.iso8601Format.format(modifiedDate.getTime());


        // Get Events that start now to the next several hours
        return DataProvider.getInstance(getActivity().getApplicationContext())
                .subscribeOn(Schedulers.computation())
                .flatMap(dataProvider -> dataProvider.createQuery(PlayaDatabase.EVENTS, "SELECT " + DataProvider.makeProjectionString(adapter.getRequiredProjection()) + " FROM " + PlayaDatabase.EVENTS + " WHERE " + EventTable.startTime + " > '" + lowerBoundDateStr + "' AND " + EventTable.startTime + " < '" + upperBoundDateStr + "\' ORDER BY " + EventTable.startTime + " ASC LIMIT 100"))
                .map(SqlBrite.Query::run)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cursor -> {
                            Timber.d("Data onNext %d items", cursor.getCount());
                            onDataChanged(cursor);
                        },
                        throwable -> Timber.e(throwable, "Data onError"),
                        () -> Timber.d("Data onComplete"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_playa_list_view, container, false);
        mEmptyText = (TextView) v.findViewById(android.R.id.empty);
        mRecyclerView = ((RecyclerView) v.findViewById(android.R.id.list));
        mRecyclerView.setLayoutManager(new LayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        return v;
    }

    @Override
    public String getEmptyText() {
        return getString(R.string.no_now_items);
    }
}