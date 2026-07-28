package com.gaiagps.iburn.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = EventOccurrence.TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = EventDefinition::class,
            parentColumns = [PlayaItem.ID],
            childColumns = [Event.EVENT_ID],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = [Event.EVENT_ID, Event.START_TIME], unique = true),
        Index(value = [PlayaItem.PLAYA_ID], unique = true),
        Index(value = [Event.START_TIME]),
        Index(value = [Event.END_TIME])
    ]
)
data class EventOccurrence(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = PlayaItem.ID)
    val id: Int = 0,
    @ColumnInfo(name = Event.EVENT_ID)
    val eventId: Int,
    @ColumnInfo(name = PlayaItem.PLAYA_ID)
    val playaId: String,
    @ColumnInfo(name = Event.START_TIME)
    val startTime: Long,
    @ColumnInfo(name = Event.END_TIME)
    val endTime: Long
) {
    companion object {
        const val TABLE_NAME = "event_occurrences"
    }
}
