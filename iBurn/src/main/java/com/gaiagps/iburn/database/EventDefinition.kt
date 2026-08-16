package com.gaiagps.iburn.database

import android.annotation.SuppressLint
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@SuppressLint("ParcelCreator")
@Entity(
    tableName = EventDefinition.TABLE_NAME,
    indices = [
        Index(value = [PlayaItem.PLAYA_ID], unique = true),
        Index(value = [Event.CAMP_PLAYA_ID])
    ]
)
class EventDefinition : PlayaItem() {
    @ColumnInfo(name = Event.TYPE)
    var type: String? = null

    @ColumnInfo(name = Event.ALL_DAY)
    var allDay: Boolean = false

    @ColumnInfo(name = Event.CHECK_LOC)
    var checkLocation: Boolean = false

    @ColumnInfo(name = Event.CAMP_PLAYA_ID)
    var campPlayaId: String? = null

    @ColumnInfo(name = Event.ART_PLAYA_ID)
    var artPlayaId: String? = null

    companion object {
        const val TABLE_NAME = "events"
    }
}
