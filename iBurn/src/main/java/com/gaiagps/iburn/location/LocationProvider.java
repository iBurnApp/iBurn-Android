package com.gaiagps.iburn.location;

import android.content.Context;
import android.location.Location;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * Created by davidbrodsky on 7/5/15.
 */
public class LocationProvider {

    private static PublishSubject<Location> locationSubject = PublishSubject.create();
    private static Observable<Location> locationObservable;
    private static ReactiveLocationProvider locationProvider;

    public static Observable<Location> getLastLocation(Context context) {
        if (locationProvider == null) {
            locationObservable = locationSubject.cache(1);
            locationProvider = new ReactiveLocationProvider(context);
            locationProvider.getLastKnownLocation()
                    .subscribe(locationSubject::onNext);
        }

        return locationObservable;
    }
}
