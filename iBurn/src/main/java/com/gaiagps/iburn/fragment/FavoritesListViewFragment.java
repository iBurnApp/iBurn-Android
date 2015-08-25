package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.CursorRecyclerViewAdapter;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.adapters.PlayaSearchResponseCursorAdapter;
import com.gaiagps.iburn.database.DataProvider;
import com.squareup.sqlbrite.SqlBrite;
import com.tonicartos.superslim.LayoutManager;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

/**
 * Created by davidbrodsky on 8/3/13.
 */
public class FavoritesListViewFragment extends PlayaListViewFragment {

    public static FavoritesListViewFragment newInstance() {
        return new FavoritesListViewFragment();
    }

    protected CursorRecyclerViewAdapter getAdapter() {
        return new PlayaSearchResponseCursorAdapter(getActivity(), null, this);
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
    protected Subscription createSubscription() {

        return DataProvider.getInstance(getActivity().getApplicationContext())
                .flatMap(dataProvider -> dataProvider.observeFavorites(getAdapter().getRequiredProjection()))
                .map(SqlBrite.Query::run)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDataChanged, throwable -> Timber.e(throwable, "Failed to load favorites"));
    }

    @Override
    public String getEmptyText() {
        return getString(R.string.mark_some_favorites);
    }
}
