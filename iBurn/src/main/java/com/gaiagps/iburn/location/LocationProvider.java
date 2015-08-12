package com.gaiagps.iburn.location;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.SystemClock;

import com.gaiagps.iburn.BuildConfig;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.LocationSource;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.Subscription;
import rx.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Created by davidbrodsky on 7/5/15.
 */
public class LocationProvider {

    private static AtomicBoolean isMockingLocation = new AtomicBoolean(false);
    private static Subscription mockLocationSubscription;
    private static PublishSubject<Location> locationSubject = PublishSubject.create();
    private static ReactiveLocationProvider locationProvider;

    private static final double MAX_MOCK_LAT = 40.8037;
    private static final double MIN_MOCK_LAT = 40.7727;
    private static final double MAX_MOCK_LON = -119.1851;
    private static final double MIN_MOCK_LON = -119.2210;

    public static Observable<Location> getLastLocation(Context context) {
        if (locationProvider == null) {
            locationProvider = new ReactiveLocationProvider(context);

            if (BuildConfig.MOCK) {
                mockCurrentLocation();
            } else {
                locationProvider.getLastKnownLocation()
                        .subscribe(locationSubject::onNext);
            }
        }

        return locationSubject.cache(1).first();
    }

    public static Observable<Location> observeCurrentLocation(Context context, LocationRequest request) {
        if (locationProvider == null) {
            locationProvider = new ReactiveLocationProvider(context);

            if (BuildConfig.MOCK) {
                mockCurrentLocation();
            }
        }

        // Mocking doesn't seem to work for getUpdatedLocation...
        if (BuildConfig.MOCK) {
            return locationSubject.asObservable();
        } else {
            return locationProvider.getUpdatedLocation(request);
        }
    }

    /**
     * @return a mock {@link Location} generally within the bounds of BRC
     */
    private static Location createMockLocation() {
        Location mockLocation = new Location("mock");

        double mockLat = (Math.random() * (MAX_MOCK_LAT - MIN_MOCK_LAT)) + MIN_MOCK_LAT;
        double mockLon = (Math.random() * (MAX_MOCK_LON - MIN_MOCK_LON)) + MIN_MOCK_LON;
        mockLocation.setLatitude(mockLat);
        mockLocation.setLongitude(mockLon);
        mockLocation.setAccuracy(1.0f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        mockLocation.setTime(new Date().getTime()); // TODO : Should we use mocked date here as well?
        //Timber.d("Creating mock location (%f, %f)", mockLat, mockLon);
        return mockLocation;
    }

    private static void mockCurrentLocation() {
        if (!isMockingLocation.get()) {
            //Timber.d("Activating current location mock");
            isMockingLocation.set(true);

            locationProvider.mockLocation(locationSubject);
            mockLocationSubscription = Observable.timer(0, 10, TimeUnit.SECONDS)
                    .subscribe(time -> {
                        //Timber.d("Providing mock location");
                        locationSubject.onNext(createMockLocation());
                    });
        }
    }

    /**
     * A Mock {@link LocationSource} for use with a GoogleMap
     */
    public static class MockLocationSource implements LocationSource {

        private Subscription locationSubscription;

        @Override
        public void activate(final OnLocationChangedListener onLocationChangedListener) {
            //Timber.d("activating MockLocationSource");
            mockCurrentLocation();
            locationSubscription = locationSubject.subscribe(onLocationChangedListener::onLocationChanged,
                    throwable -> Timber.e(throwable, "Error sending mock location to map"));
        }

        @Override
        public void deactivate() {
            //Timber.d("deactivating MockLocationSource");
            if (locationSubscription != null) {
                locationSubscription.unsubscribe();
                locationSubscription = null;
            }
        }
    }
}
