package com.gaiagps.iburn.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.gaiagps.iburn.BuildConfig
import com.gaiagps.iburn.PermissionManager
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Fulfills requests for location and supports mocking based on the value of [BuildConfig.MOCK]
 * Created by davidbrodsky on 7/5/15.
 */
object LocationProvider {
    private var fusedClient: FusedLocationProviderClient? = null

    // Location Mocking
    private val isMockingLocation = AtomicBoolean(false)
    private var mockJob: Job? = null
    private var lastMockLocation = createMockLocation()
    private val mockLocationFlow = MutableSharedFlow<Location>(replay = 1)
    private const val MAX_MOCK_LAT = 40.8037
    private const val MIN_MOCK_LAT = 40.7727
    private const val MAX_MOCK_LON = -119.1851
    private const val MIN_MOCK_LON = -119.2210

    fun getLastLocationFlow(context: Context): Flow<Location> {
        init(context)
        return if (BuildConfig.MOCK) {
            flowOf(lastMockLocation)
        } else {
            if (!PermissionManager.hasLocationPermissions(context)) {
                emptyFlow()
            } else callbackFlow {
                val client = fusedClient!!
                client.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) trySend(loc)
                    close()
                }.addOnFailureListener { close(it) }
                awaitClose { }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun currentLocationFlow(context: Context, request: LocationRequest): Flow<Location> {
        init(context)
        return if (BuildConfig.MOCK) {
            mockLocationFlow.asSharedFlow()
        } else {
            if (!PermissionManager.hasLocationPermissions(context)) {
                emptyFlow()
            } else callbackFlow {
                val client = fusedClient!!
                val cb = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let { trySend(it) }
                    }
                }
                client.requestLocationUpdates(request, cb, Looper.getMainLooper())
                awaitClose { client.removeLocationUpdates(cb) }
            }
        }
    }

    private fun init(context: Context) {
        if (fusedClient == null) {
            fusedClient = LocationServices.getFusedLocationProviderClient(context)
            if (BuildConfig.MOCK) mockCurrentLocation()
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
            mockJob?.cancel()
            mockJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive) {
                    lastMockLocation = createMockLocation()
                    mockLocationFlow.emit(lastMockLocation)
                    kotlinx.coroutines.delay(TimeUnit.SECONDS.toMillis(15))
                }
            }
        }
    }

    class MapboxMockLocationSource : LocationEngine {
        private var mockLocationJob: Job? = null
        private var areUpdatesRequested = false
        fun activate() {
            Timber.d("activate mock location provider")
            mockCurrentLocation()
            deactivate()
            areUpdatesRequested = true
            // "Connection" is immediate here
        }

        fun deactivate() {
            mockLocationJob?.cancel()
            mockLocationJob = null
        }

        val isConnected: Boolean
            get() = true

        @SuppressLint("MissingPermission", "CheckResult")
        override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
            Handler(Looper.getMainLooper()).post {
                callback.onSuccess(LocationEngineResult.create(lastMockLocation))
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
            val handler = if (looper != null) Handler(looper) else Handler(Looper.getMainLooper())
            mockLocationJob?.cancel()
            mockLocationJob = CoroutineScope(Dispatchers.Default).launch {
                mockLocationFlow.collect { location ->
                    if (!areUpdatesRequested) return@collect
                    handler.post {
                        result.onSuccess(LocationEngineResult.create(location))
                    }
                }
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
