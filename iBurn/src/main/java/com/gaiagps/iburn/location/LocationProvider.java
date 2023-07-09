package com.gaiagps.iburn.location;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.gaiagps.iburn.BuildConfig;
import com.gaiagps.iburn.PermissionManager;
import com.google.android.gms.location.LocationRequest;
import com.mapbox.mapboxsdk.location.engine.LocationEngine;
import com.mapbox.mapboxsdk.location.engine.LocationEngineCallback;
import com.mapbox.mapboxsdk.location.engine.LocationEngineRequest;
import com.mapbox.mapboxsdk.location.engine.LocationEngineResult;
import com.patloew.rxlocation.RxLocation;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Fulfills requests for location and supports mocking based on the value of {@link BuildConfig#MOCK}
 * Created by davidbrodsky on 7/5/15.
 */
public class LocationProvider {

    private static RxLocation locationProvider;

    // Location Mocking
    private static AtomicBoolean isMockingLocation = new AtomicBoolean(false);
    private static Disposable mockLocationSubscription;
    private static Location lastMockLocation = createMockLocation();
    private static PublishSubject<Location> mockLocationSubject = PublishSubject.create();

    private static final double MAX_MOCK_LAT = 40.8037;
    private static final double MIN_MOCK_LAT = 40.7727;
    private static final double MAX_MOCK_LON = -119.1851;
    private static final double MIN_MOCK_LON = -119.2210;

    @SuppressLint("MissingPermission")
    public static Observable<Location> getLastLocation(Context context) {
        init(context);

        if (BuildConfig.MOCK) {
            return Observable.just(lastMockLocation);
        } else {
            if (!PermissionManager.hasLocationPermissions(context)) {
                // TODO: stall location result until permission ready
                return Observable.empty();
            }
            return locationProvider.location().lastLocation().toObservable();
        }
    }

    @SuppressLint("MissingPermission")
    public static Observable<Location> observeCurrentLocation(Context context, LocationRequest request) {
        init(context);

        if (BuildConfig.MOCK) {
            return mockLocationSubject.startWith(lastMockLocation);
        } else {
            if (!PermissionManager.hasLocationPermissions(context)) {
                // TODO: stall location result until permission ready
                return Observable.empty();
            }

            return locationProvider.location().updates(request);
        }
    }

    private static void init(Context context) {
        if (locationProvider == null) {
            locationProvider = new RxLocation(context);

            if (BuildConfig.MOCK) mockCurrentLocation();
        }
    }

    /**
     * @return a mock {@link Location} generally within the bounds of BRC
     */
    public static Location createMockLocation() {
        Location mockLocation = new Location("mock");

        double mockLat = (Math.random() * (MAX_MOCK_LAT - MIN_MOCK_LAT)) + MIN_MOCK_LAT;
        double mockLon = (Math.random() * (MAX_MOCK_LON - MIN_MOCK_LON)) + MIN_MOCK_LON;
        mockLocation.setLatitude(mockLat);
        mockLocation.setLongitude(mockLon);
        mockLocation.setAccuracy(1.0f);
        mockLocation.setBearing(.4f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        mockLocation.setTime(new Date().getTime()); // TODO : Should we use mocked date here as well?
        return mockLocation;
    }

    private static void mockCurrentLocation() {
        if (!isMockingLocation.get()) {
            isMockingLocation.set(true);

            mockLocationSubscription = Observable.interval(2, 15, TimeUnit.SECONDS)
                    .startWith(-1l)
                    .subscribe(time -> {
                        lastMockLocation = createMockLocation();
                        mockLocationSubject.onNext(lastMockLocation);
                    });
        }
    }

    public static class MapboxMockLocationSource implements LocationEngine {

        private CompositeDisposable mockLocationSubs = new CompositeDisposable();
        private boolean areUpdatesRequested = false;

        public MapboxMockLocationSource() {
            super();
        }

        public void activate() {
            Timber.d("activate mock location provider");
            mockLocationSubs = new CompositeDisposable();
            mockCurrentLocation();

            deactivate();

            areUpdatesRequested = true;
            // "Connection" is immediate here
        }


        public void deactivate() {
            if (mockLocationSubs != null) {
                mockLocationSubs.dispose();
                mockLocationSubs = null;
            }
        }


        public boolean isConnected() {
            return true;
        }

        @SuppressLint({"MissingPermission", "CheckResult"})
        public void getLastLocation(@NonNull LocationEngineCallback<LocationEngineResult> callback) {
            mockLocationSubject
                    .take(1)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(location -> {
                        callback.onSuccess(LocationEngineResult.create(location));
                    });
        }


        public void requestLocationUpdates(PendingIntent intent) {
            // PendingIntent API is probably for maplibre internal use only - this would require
            // some knowledge about how to format result into the PendingIntent's "extra" keys and values
            throw new UnsupportedOperationException("PendingIntent API not supported");
        }

        public void requestLocationUpdates(LocationEngineRequest request, PendingIntent intent) {
            throw new UnsupportedOperationException("PendingIntent API not supported");
        }

        public void requestLocationUpdates(LocationEngineRequest request, LocationEngineCallback<LocationEngineResult> result, Looper looper) {
            areUpdatesRequested = true;
            Disposable requestLocationSub = mockLocationSubject
                    .takeWhile(ignored -> areUpdatesRequested)
                    .observeOn(AndroidSchedulers.from(looper))
                    .subscribe(location -> {
                        result.onSuccess(LocationEngineResult.create(location));
                    });
            if (mockLocationSubs != null) {
                mockLocationSubs.add(requestLocationSub);
            }
        }

        public void removeLocationUpdates(PendingIntent intent) {
            areUpdatesRequested = false;
        }

        public void removeLocationUpdates(LocationEngineCallback<LocationEngineResult> result) {
            areUpdatesRequested = false;
        }
    }
}
