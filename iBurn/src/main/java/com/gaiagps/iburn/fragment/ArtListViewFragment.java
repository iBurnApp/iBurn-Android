package com.gaiagps.iburn.fragment;

import android.os.Bundle;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.adapters.CursorRecyclerViewAdapter;
import com.gaiagps.iburn.adapters.PlayaItemCursorAdapter;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.squareup.sqlbrite.SqlBrite;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Fragment displaying all Playa Art
 * <p/>
 * Created by davidbrodsky on 8/3/13.
 */
public class ArtListViewFragment extends PlayaListViewFragment {

    public static ArtListViewFragment newInstance() {
        return new ArtListViewFragment();
    }

    protected CursorRecyclerViewAdapter getAdapter() {
        return new PlayaItemCursorAdapter(getActivity(), null, Constants.PlayaItemType.ART, this);
    }

    @Override
    protected Subscription subscribeToData() {
        return DataProvider.getInstance(getActivity())
                .observeTable(PlayaDatabase.ART)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(query -> {
                    Timber.d("Got update on thread");
                    onDataChanged(query.run());
                });
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}