package com.gaiagps.iburn.fragment;

import android.os.Bundle;

import com.gaiagps.iburn.database.DataProvider;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

/**
 * Fragment displaying all Playa Camps
 * <p/>
 * Created by davidbrodsky on 8/3/13.
 */
public class CampListViewFragment extends PlayaListViewFragment {

    public static CampListViewFragment newInstance() {
        return new CampListViewFragment();
    }

    @Override
    public Disposable createDisposable() {
        return DataProvider.getInstance(getActivity().getApplicationContext())
                .flatMap(dataProvider -> dataProvider.observeCamps())
                .doOnNext(query -> Timber.d("Got query"))
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
}