package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gaiagps.iburn.BuildConfig;
import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.CurrentDateProvider;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.CursorRecyclerViewAdapter;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.adapters.EventCursorAdapter;
import com.gaiagps.iburn.adapters.EventSectionedCursorAdapter;
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.gaiagps.iburn.view.PlayaListViewHeader;
import com.squareup.sqlbrite.SqlBrite;
import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslim.LinearSLM;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

/**
 * Fragment displaying all Playa Events
 * <p/>
 * Created by davidbrodsky on 8/3/13.
 */
public class ExploreListViewFragment extends PlayaListViewFragment {

    public static ExploreListViewFragment newInstance() {
        return new ExploreListViewFragment();
    }

    private String selectedDay = "8/25";
    private ArrayList<String> selectedTypes;

    protected CursorRecyclerViewAdapter getAdapter() {
        return new EventSectionedCursorAdapter(getActivity(), null, false, this);
    }

    @Override
    protected Subscription _subscribeToData() {

        // TODO : Get debug date dynamically
        Calendar modifiedDate = Calendar.getInstance();
        modifiedDate.setTime(CurrentDateProvider.getCurrentDate());
//        modifiedDate.add(Calendar.HOUR, -1);
        String lowerBoundDateStr = PlayaDateTypeAdapter.iso8601Format.format(modifiedDate.getTime());
        modifiedDate.add(Calendar.HOUR, 7);
        String upperBoundDateStr = PlayaDateTypeAdapter.iso8601Format.format(modifiedDate.getTime());


        // Get Events that started from within the last hour to in the next 6 hours
        return DataProvider.getInstance(getActivity())
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

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_explore_list_view, container, false);
        mEmptyText = (TextView) v.findViewById(android.R.id.empty);
        mRecyclerView = ((RecyclerView) v.findViewById(android.R.id.list));
        mRecyclerView.setLayoutManager(new LayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        return v;
    }
}