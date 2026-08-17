package com.gaiagps.iburn

import java.util.Date

object EventInfo {
    data class MockLocation(val latitude: Double, val longitude: Double)

    const val CURRENT_YEAR = AnnualMetadata.YEAR

    /**
     * The date when the event starts. Used to populate date selection pickers.
     */
    @JvmField
    val EVENT_START_DATE: Date = Date(AnnualMetadata.EVENT_START_EPOCH_MILLIS)

    /**
     * The date when the event ends. Used to populate date selection pickers.
     */
    @JvmField
    val EVENT_END_DATE: Date = Date(AnnualMetadata.EVENT_END_EPOCH_MILLIS)

    /**
     * The date when location data is publicly available without a staff unlock code.
     */
    @JvmField
    val LOCATION_EMBARGO_DATE: Date = Date(AnnualMetadata.LOCATION_EMBARGO_EPOCH_MILLIS)

    /** The date when camp street addresses become public. */
    @JvmField
    val CAMP_ADDRESS_EMBARGO_DATE: Date = Date(AnnualMetadata.CAMP_ADDRESS_EMBARGO_EPOCH_MILLIS)

    /**
     * The "current" date used by the 'mock' build variant.
     */
    @JvmField
    val MOCK_NOW_DATE: Date = Date(AnnualMetadata.MOCK_NOW_EPOCH_MILLIS)

    /** A fixed location for mock builds, or null to use the simulated walking path. */
    @JvmField
    val MOCK_LOCATION: MockLocation? = if (AnnualMetadata.HAS_MOCK_LOCATION) {
        MockLocation(
            AnnualMetadata.MOCK_LOCATION_LATITUDE,
            AnnualMetadata.MOCK_LOCATION_LONGITUDE
        )
    } else {
        null
    }
}
