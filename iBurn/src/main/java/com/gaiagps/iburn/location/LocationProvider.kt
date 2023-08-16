package com.gaiagps.iburn.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import com.gaiagps.iburn.BuildConfig
import com.gaiagps.iburn.PermissionManager
import com.google.android.gms.location.LocationRequest
import com.mapbox.mapboxsdk.location.engine.LocationEngine
import com.mapbox.mapboxsdk.location.engine.LocationEngineCallback
import com.mapbox.mapboxsdk.location.engine.LocationEngineRequest
import com.mapbox.mapboxsdk.location.engine.LocationEngineResult
import com.patloew.colocation.CoLocation
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.rx2.asObservable
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Fulfills requests for location and supports mocking based on the value of [BuildConfig.MOCK]
 * Created by davidbrodsky on 7/5/15.
 */
object LocationProvider {
    private var locationProvider: CoLocation? = null

    // Location Mocking
    private val isMockingLocation = AtomicBoolean(false)
    private var mockLocationSubscription: Disposable? = null
    private var lastMockLocation = createMockLocation()
    private val mockLocationSubject = PublishSubject.create<Location>()
    private const val MAX_MOCK_LAT = 40.8037
    private const val MIN_MOCK_LAT = 40.7727
    private const val MAX_MOCK_LON = -119.1851
    private const val MIN_MOCK_LON = -119.2210

    @SuppressLint("MissingPermission")
    fun getLastLocation(context: Context): Observable<Location> {
        return getLastLocationFlow(context).asObservable()
    }

    fun getLastLocationFlow(context: Context): Flow<Location> {
        init(context)
        return if (BuildConfig.MOCK) {
            return flow {
                emit(lastMockLocation)
            }
        } else {
            if (!PermissionManager.hasLocationPermissions(context)) {
                flow {
                }
            } else flow {
                val lastLocation = locationProvider?.getLastLocation()
                if (lastLocation != null) {
                    emit(lastLocation)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun observeCurrentLocation(context: Context, request: LocationRequest): Observable<Location> {
        return currentLocationFlow(context, request).asObservable()
    }

    private fun currentLocationFlow(context: Context, request: LocationRequest): Flow<Location> {
        init(context)
        return if (BuildConfig.MOCK) {
            flow {
                lastMockLocation
            }
        } else {
            if (!PermissionManager.hasLocationPermissions(context)) {
                flow {

                }
            } else locationProvider!!.getLocationUpdates(request)
        }
    }

    private fun init(context: Context) {
        if (locationProvider == null) {
            locationProvider = CoLocation.from(context)
            if (BuildConfig.MOCK) {
                mockCurrentLocation()
            }
        }
    }

    /**
     * @return a mock [Location] generally within the bounds of BRC
     */
    fun createMockLocation(): Location {
        val mockLocation = Location("mock")
        val mockLat = Math.random() * (MAX_MOCK_LAT - MIN_MOCK_LAT) + MIN_MOCK_LAT
        val mockLon = Math.random() * (MAX_MOCK_LON - MIN_MOCK_LON) + MIN_MOCK_LON
        mockLocation.latitude = mockLat
        mockLocation.longitude = mockLon
        mockLocation.accuracy = 1.0f
        mockLocation.bearing = .4f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mockLocation.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        mockLocation.time = Date().time // TODO : Should we use mocked date here as well?
        return mockLocation
    }

    private fun mockCurrentLocation() {
        if (!isMockingLocation.get()) {
            isMockingLocation.set(true)
            mockLocationSubscription = Observable.interval(2, 15, TimeUnit.SECONDS)
                .startWith(-1L)
                .subscribe { time: Long? ->
                    lastMockLocation = createMockLocation()
                    mockLocationSubject.onNext(lastMockLocation)
                }
        }
    }

    class MapboxMockLocationSource : LocationEngine {
        private var mockLocationSubs: CompositeDisposable? = CompositeDisposable()
        private var areUpdatesRequested = false
        fun activate() {
            Timber.d("activate mock location provider")
            mockLocationSubs = CompositeDisposable()
            mockCurrentLocation()
            deactivate()
            areUpdatesRequested = true
            // "Connection" is immediate here
        }

        fun deactivate() {
            if (mockLocationSubs != null) {
                mockLocationSubs!!.dispose()
                mockLocationSubs = null
            }
        }

        val isConnected: Boolean
            get() = true

        @SuppressLint("MissingPermission", "CheckResult")
        override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
            mockLocationSubject
                .take(1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { location: Location? ->
                    callback.onSuccess(
                        LocationEngineResult.create(
                            location
                        )
                    )
                }
        }

        fun requestLocationUpdates(intent: PendingIntent?) {
            // PendingIntent API is probably for maplibre internal use only - this would require
            // some knowledge about how to format result into the PendingIntent's "extra" keys and values
            throw UnsupportedOperationException("PendingIntent API not supported")
        }

        override fun requestLocationUpdates(request: LocationEngineRequest, intent: PendingIntent) {
            throw UnsupportedOperationException("PendingIntent API not supported")
        }

        override fun requestLocationUpdates(
            request: LocationEngineRequest,
            result: LocationEngineCallback<LocationEngineResult>,
            looper: Looper?
        ) {
            areUpdatesRequested = true
            val requestLocationSub = mockLocationSubject
                .takeWhile { ignored: Location? -> areUpdatesRequested }
                .observeOn(AndroidSchedulers.from(looper))
                .subscribe { location: Location? ->
                    result.onSuccess(
                        LocationEngineResult.create(
                            location
                        )
                    )
                }
            if (mockLocationSubs != null) {
                mockLocationSubs!!.add(requestLocationSub)
            }
        }

        override fun removeLocationUpdates(intent: PendingIntent) {
            areUpdatesRequested = false
        }

        override fun removeLocationUpdates(result: LocationEngineCallback<LocationEngineResult>) {
            areUpdatesRequested = false
        }
    }
}