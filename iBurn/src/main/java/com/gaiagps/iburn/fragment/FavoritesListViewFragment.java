package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.database.DataProvider;
import com.tonicartos.superslim.LayoutManager;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

/**
 * Display Camps, Art, and Events that the user has favorited
 * Created by davidbrodsky on 8/3/13.
 */
public class FavoritesListViewFragment extends PlayaListViewFragment {

    public static FavoritesListViewFragment newInstance() {
        return new FavoritesListViewFragment();
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
    protected Disposable createDisposable() {

        return DataProvider.getInstance(getActivity().getApplicationContext())
                .flatMap(provider -> provider.observeFavorites().toObservable()) // TODO : rm toObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDataChanged, throwable -> Timber.e(throwable, "Failed to load favorites"));
    }

    @Override
    public String getEmptyText() {
        return getString(R.string.mark_some_favorites);
    }
}
