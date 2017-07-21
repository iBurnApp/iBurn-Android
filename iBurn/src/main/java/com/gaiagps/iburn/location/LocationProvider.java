package com.gaiagps.iburn.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.SystemClock;

import com.gaiagps.iburn.BuildConfig;
import com.gaiagps.iburn.Geo;
import com.gaiagps.iburn.PermissionManager;
import com.google.android.gms.location.LocationRequest;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.patloew.rxlocation.RxLocation;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Fulfills requests for location and supports mocking based on the value of {@link BuildConfig#MOCK}
 * Created by davidbrodsky on 7/5/15.
 */
public class LocationProvider {

    // Location Mocking
    private static AtomicBoolean isMockingLocation = new AtomicBoolean(false);
    private static Disposable mockLocationSubscription;
    private static Location lastLocation;
    private static BehaviorSubject<Location> locationSubject = BehaviorSubject.create();

    private static final double MAX_MOCK_LAT = 40.8037;
    private static final double MIN_MOCK_LAT = 40.7727;
    private static final double MAX_MOCK_LON = -119.1851;
    private static final double MIN_MOCK_LON = -119.2210;

    @SuppressLint("MissingPermission")
    public static Observable<Location> getLastLocation(Context context) {
        init(context);

        return locationSubject.firstElement().toObservable();
    }

    @SuppressLint("MissingPermission")
    public static Observable<Location> observeCurrentLocation(Context context, LocationRequest request) {
        init(context);

        return locationSubject.hide();
    }

    private static void init(Context context) {

        if (BuildConfig.MOCK) mockCurrentLocation();
    }

    /**
     * Provide a location to clients of this component
     */
    public static void submitLocation(Location location) {
        lastLocation = location;
        locationSubject.onNext(location);
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
        mockLocation.setBearing((float) (Math.random() * 360));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        mockLocation.setTime(new Date().getTime()); // TODO : Should we use mocked date here as well?
        return mockLocation;
    }

    private static void mockCurrentLocation() {
        if (!isMockingLocation.get()) {
            lastLocation = createMockLocation();
            isMockingLocation.set(true);

            mockLocationSubscription = Observable.interval(15, 15, TimeUnit.SECONDS)
                    .startWith(-1l)
                    .subscribe(time -> {
                        submitLocation(createMockLocation());
                    });
        }
    }

    public static class MapboxMockLocationSource extends LocationEngine {

        private Disposable mockLocationSub;
        private boolean areUpdatesRequested = false;
        private Location lastLocation;

        public MapboxMockLocationSource() {
            super();
        }

        @Override
        public void activate() {
            Timber.d("activate mock location provider");
            mockCurrentLocation();

            deactivate();

            areUpdatesRequested = true;

            mockLocationSub = locationSubject
                    .takeWhile(ignored -> areUpdatesRequested)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(location -> {
                        lastLocation = location;
                        Timber.d("Delivering mock location");
                        for (LocationEngineListener listener : locationListeners) {
                            listener.onLocationChanged(location);
                        }
                    });

            // "Connection" is immediate here
            for (LocationEngineListener listener : locationListeners) {
                listener.onConnected();
            }
        }

        @Override
        public void deactivate() {
            Timber.d("Deactivate");
            areUpdatesRequested = false;
            if (mockLocationSub != null) {
                mockLocationSub.dispose();
                mockLocationSub = null;
            }
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @SuppressLint("MissingPermission")
        @Override
        public Location getLastLocation() {
            Location loc = new Location("MOCK_PROVIDER");
            if (lastLocation != null) {
                loc.setLatitude(lastLocation.getLatitude());
                loc.setLongitude(lastLocation.getLongitude());
            } else {
                loc.setLatitude(Geo.MAN_LAT);
                loc.setLongitude(Geo.MAN_LON);
            }
            loc.setBearing((float) (Math.random() * 360));
            loc.setAccuracy((float) (Math.random() * 30));
            return loc;
        }

        @Override
        public void requestLocationUpdates() {
            areUpdatesRequested = true;
        }

        @Override
        public void removeLocationUpdates() {
            Timber.d("removeLocationUpdates");
            areUpdatesRequested = false;
        }
    }
}
