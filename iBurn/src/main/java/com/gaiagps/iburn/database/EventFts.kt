package com.gaiagps.iburn.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = Event::class)
@Entity(tableName = EventFts.TABLE_NAME)
data class EventFts(
    @ColumnInfo(name = PlayaItem.NAME) val name: String?,
    @ColumnInfo(name = PlayaItem.DESC) val description: String?
) {
    companion object { const val TABLE_NAME = "events_fts" }
}
