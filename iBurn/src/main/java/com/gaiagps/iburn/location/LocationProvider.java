package com.gaiagps.iburn.location;

import android.content.Context;
import android.location.Location;

import com.gaiagps.iburn.BuildConfig;
import com.gaiagps.iburn.Geo;
import com.google.android.gms.location.LocationRequest;

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
        if (BuildConfig.DEBUG) {
            Location targetLocation = new Location("");
            targetLocation.setLatitude(Geo.MAN_LAT);
            targetLocation.setLongitude(Geo.MAN_LON);
            return Observable.just(targetLocation);
        }

        if (locationProvider == null) {
            locationObservable = locationSubject.cache(1);
            locationProvider = new ReactiveLocationProvider(context);
            locationProvider.getLastKnownLocation()
                    .subscribe(locationSubject::onNext);
        }

        return locationObservable;
    }

    public static Observable<Location> observeCurrentLocation(Context context, LocationRequest request) {
        if (locationProvider == null) {
            locationProvider = new ReactiveLocationProvider(context);
        }

        ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(context);
        return locationProvider.getUpdatedLocation(request);
    }
}
