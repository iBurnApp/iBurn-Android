package com.gaiagps.iburn.location

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationTrailHistoryTest {

    @Test
    fun add_keepsOnlyPointsInsideHistoryWindow() {
        val history = LocationTrailHistory(maxAgeMillis = 1_000, maxPoints = 10)

        history.add(point(0))
        history.add(point(500))
        history.add(point(1_001))

        assertEquals(listOf(500L, 1_001L), history.points().map { it.recordedAtMillis })
    }

    @Test
    fun add_capsPointCount() {
        val history = LocationTrailHistory(maxAgeMillis = 10_000, maxPoints = 3)

        (1L..4L).forEach { history.add(point(it)) }

        assertEquals(listOf(2L, 3L, 4L), history.points().map { it.recordedAtMillis })
    }

    @Test
    fun prune_usesUpdatedHistoryWindow() {
        val history = LocationTrailHistory(maxAgeMillis = 10_000)
        history.add(point(1_000))
        history.add(point(5_000))
        history.maxAgeMillis = 2_000

        history.prune(nowMillis = 6_000)

        assertEquals(listOf(5_000L), history.points().map { it.recordedAtMillis })
    }

    @Test
    fun add_ignoresInvalidCoordinates() {
        val history = LocationTrailHistory(maxAgeMillis = 1_000)

        history.add(LocationTrailPoint(91.0, 0.0, 1))
        history.add(LocationTrailPoint(0.0, Double.NaN, 2))

        assertEquals(emptyList<LocationTrailPoint>(), history.points())
    }

    private fun point(recordedAtMillis: Long) =
        LocationTrailPoint(40.78, -119.20, recordedAtMillis)
}
