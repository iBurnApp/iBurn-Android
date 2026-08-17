package com.gaiagps.iburn.database

import androidx.room.ColumnInfo

data class EventDateRange(
    @ColumnInfo(name = "first_start_time")
    val firstStartTime: Long?,
    @ColumnInfo(name = "last_end_time")
    val lastEndTime: Long?
)
