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
import kotlin.math.cos
import kotlin.random.Random

/**
 * Fulfills requests for location and supports mocking based on the value of [BuildConfig.MOCK]
 * Created by davidbrodsky on 7/5/15.
 */
object LocationProvider {
    private var fusedClient: FusedLocationProviderClient? = null

    // Location Mocking
    private val isMockingLocation = AtomicBoolean(false)
    private var mockJob: Job? = null
    private var mockBearing = Random.nextDouble(0.0, 360.0)
    private var mockSpeedMetersPerSecond = randomMockWalkingSpeed()
    private var mockStepsUntilTurn = 0
    private var mockPauseStepsRemaining = 0
    private var lastMockLocation = createMockLocation()
    private val mockLocationFlow = MutableSharedFlow<Location>(replay = 1)
    private const val MAX_MOCK_LAT = 40.8037
    private const val MIN_MOCK_LAT = 40.7727
    private const val MAX_MOCK_LON = -119.1851
    private const val MIN_MOCK_LON = -119.2210
    private const val MOCK_CENTER_LAT = (MAX_MOCK_LAT + MIN_MOCK_LAT) / 2.0
    private const val MOCK_CENTER_LON = (MAX_MOCK_LON + MIN_MOCK_LON) / 2.0
    private const val MOCK_UPDATE_SECONDS = 5L
    private const val METERS_PER_DEGREE_LATITUDE = 111_320.0
    private const val BOUNDARY_BUFFER_DEGREES = 0.0005

    @SuppressLint("MissingPermission")
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
        // Start somewhere near the middle of the event instead of at an arbitrary edge.
        val mockLat = MOCK_CENTER_LAT + Random.nextDouble(-0.002, 0.002)
        val mockLon = MOCK_CENTER_LON + Random.nextDouble(-0.002, 0.002)
        mockLocation.latitude = mockLat
        mockLocation.longitude = mockLon
        mockLocation.accuracy = randomMockAccuracy()
        mockLocation.bearing = mockBearing.toFloat()
        mockLocation.speed = 0f
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
                    lastMockLocation = nextMockLocation(lastMockLocation)
                    mockLocationFlow.emit(lastMockLocation)
                    kotlinx.coroutines.delay(TimeUnit.SECONDS.toMillis(MOCK_UPDATE_SECONDS))
                }
            }
        }
    }

    /**
     * Advances the mock position like a pedestrian: short walking segments, gradual course
     * changes, and occasional pauses. If a segment approaches the event bounds, the next
     * heading points back toward the center of the city.
     */
    private fun nextMockLocation(previous: Location): Location {
        if (mockPauseStepsRemaining > 0) {
            mockPauseStepsRemaining--
            return mockLocation(previous.latitude, previous.longitude, previous.bearing, 0f)
        }

        if (mockStepsUntilTurn <= 0) {
            if (Random.nextDouble() < 0.18) {
                // Stop for 10-40 seconds, as someone might when looking at art or talking.
                mockPauseStepsRemaining = Random.nextInt(1, 8)
                return mockLocation(previous.latitude, previous.longitude, previous.bearing, 0f)
            }
            mockBearing = normalizedBearing(mockBearing + Random.nextDouble(-55.0, 55.0))
            mockSpeedMetersPerSecond = randomMockWalkingSpeed()
            mockStepsUntilTurn = Random.nextInt(6, 25)
        } else {
            // Small changes on every sample keep the path from looking mechanically straight.
            mockBearing = normalizedBearing(mockBearing + Random.nextDouble(-7.0, 7.0))
        }

        mockBearing = bearingPointingInsideBounds(previous, mockBearing)
        mockStepsUntilTurn--

        val distanceMeters = mockSpeedMetersPerSecond * MOCK_UPDATE_SECONDS
        val bearingRadians = Math.toRadians(mockBearing)
        val latitude = previous.latitude +
            distanceMeters * cos(bearingRadians) / METERS_PER_DEGREE_LATITUDE
        val metersPerDegreeLongitude =
            METERS_PER_DEGREE_LATITUDE * cos(Math.toRadians(previous.latitude))
        val longitude = previous.longitude +
            distanceMeters * kotlin.math.sin(bearingRadians) / metersPerDegreeLongitude

        return mockLocation(
            latitude.coerceIn(MIN_MOCK_LAT, MAX_MOCK_LAT),
            longitude.coerceIn(MIN_MOCK_LON, MAX_MOCK_LON),
            mockBearing.toFloat(),
            mockSpeedMetersPerSecond.toFloat()
        )
    }

    private fun bearingPointingInsideBounds(previous: Location, proposedBearing: Double): Double {
        val isNearBoundary =
            previous.latitude < MIN_MOCK_LAT + BOUNDARY_BUFFER_DEGREES ||
                previous.latitude > MAX_MOCK_LAT - BOUNDARY_BUFFER_DEGREES ||
                previous.longitude < MIN_MOCK_LON + BOUNDARY_BUFFER_DEGREES ||
                previous.longitude > MAX_MOCK_LON - BOUNDARY_BUFFER_DEGREES
        if (!isNearBoundary) return proposedBearing

        val latitudeDelta = MOCK_CENTER_LAT - previous.latitude
        val longitudeDelta = (MOCK_CENTER_LON - previous.longitude) *
            cos(Math.toRadians(previous.latitude))
        val centerBearing = Math.toDegrees(kotlin.math.atan2(longitudeDelta, latitudeDelta))
        return normalizedBearing(centerBearing + Random.nextDouble(-25.0, 25.0))
    }

    private fun mockLocation(
        latitude: Double,
        longitude: Double,
        bearing: Float,
        speed: Float
    ) = Location("mock").apply {
        this.latitude = latitude
        this.longitude = longitude
        accuracy = randomMockAccuracy()
        this.bearing = bearing
        this.speed = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        time = Date().time
    }

    private fun normalizedBearing(bearing: Double) = (bearing + 360.0) % 360.0

    private fun randomMockAccuracy() = Random.nextDouble(4.0, 11.0).toFloat()

    private fun randomMockWalkingSpeed() = Random.nextDouble(0.8, 1.65)

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
