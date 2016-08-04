package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.ArtCursorAdapter;
import com.gaiagps.iburn.adapters.CursorRecyclerViewAdapter;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.database.ArtTable;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.gaiagps.iburn.view.ArtListHeader;
import com.squareup.sqlbrite.SqlBrite;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

/**
 * Fragment displaying all Playa Art
 * <p/>
 * Created by davidbrodsky on 8/3/13.
 */
public class ArtListViewFragment extends PlayaListViewFragment implements ArtListHeader.Listener {

    public static ArtListViewFragment newInstance() {
        return new ArtListViewFragment();
    }

    private boolean showAudioTourOnly;

    protected CursorRecyclerViewAdapter getAdapter() {
        return new ArtCursorAdapter(getActivity(), null, this);
    }

    @Override
    public Subscription createSubscription() {
        return DataProvider.getInstance(getActivity().getApplicationContext())
                .flatMap(dataProvider -> {
                    if (showAudioTourOnly) {
                        return dataProvider.observeTable(PlayaDatabase.ART, adapter.getRequiredProjection(), ArtTable.audioTourUrl + " IS NOT NULL");

                    } else {
                        return dataProvider.observeTable(PlayaDatabase.ART, getAdapter().getRequiredProjection());
                    }
                })
                .map(SqlBrite.Query::run)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cursor -> {
                            Timber.d("Data onNext");
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
        View v = inflater.inflate(R.layout.fragment_art_list_view, container, false);
        mEmptyText = (TextView) v.findViewById(android.R.id.empty);
        mRecyclerView = ((RecyclerView) v.findViewById(android.R.id.list));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        ((ArtListHeader) v.findViewById(R.id.header)).setListener(this);
        return v;
    }

    @Override
    public void onSelectionChanged(boolean showAudioTourOnly) {
        this.showAudioTourOnly = showAudioTourOnly;
        unsubscribeFromData();
        subscribeToData();
    }
}