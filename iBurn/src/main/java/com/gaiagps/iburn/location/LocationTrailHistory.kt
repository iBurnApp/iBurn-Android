package com.gaiagps.iburn.location

import java.util.ArrayDeque

internal data class LocationTrailPoint(
    val latitude: Double,
    val longitude: Double,
    val recordedAtMillis: Long
)

/**
 * A bounded, in-memory location history. Timestamps use elapsed realtime so clock changes do not
 * unexpectedly grow or erase the trail.
 */
internal class LocationTrailHistory(
    maxAgeMillis: Long,
    private val maxPoints: Int = 500
) {
    private val history = ArrayDeque<LocationTrailPoint>()

    var maxAgeMillis: Long = maxAgeMillis
        set(value) {
            require(value > 0)
            field = value
        }

    init {
        require(maxAgeMillis > 0)
        require(maxPoints >= 2)
    }

    fun add(point: LocationTrailPoint) {
        if (!point.latitude.isFinite() || !point.longitude.isFinite()) return
        if (point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0) return

        history.addLast(point)
        prune(point.recordedAtMillis)
        while (history.size > maxPoints) {
            history.removeFirst()
        }
    }

    fun prune(nowMillis: Long) {
        val oldestAllowed = nowMillis - maxAgeMillis
        while (history.firstOrNull()?.recordedAtMillis?.let { it < oldestAllowed } == true) {
            history.removeFirst()
        }
    }

    fun points(): List<LocationTrailPoint> = history.toList()
}
