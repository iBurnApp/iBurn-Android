package com.gaiagps.iburn

import java.util.Date

object EventInfo {
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
    val EMBARGO_DATE: Date = Date(AnnualMetadata.ART_EMBARGO_EPOCH_MILLIS)

    /**
     * Separate embargo date for Camp and Event locations. Defaults to event start date.
     * Change this to release camps/events at a different time than Art.
     */
    @JvmField
    val CAMP_EMBARGO_DATE: Date = Date(AnnualMetadata.CAMP_EVENT_EMBARGO_EPOCH_MILLIS)

    /**
     * The "current" date used by the 'mock' build variant.
     */
    @JvmField
    val MOCK_NOW_DATE: Date = Date(AnnualMetadata.MOCK_NOW_EPOCH_MILLIS)
}
