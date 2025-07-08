package com.gaiagps.iburn.database

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = Favorite.TABLE_NAME, primaryKeys = [Favorite.PLAYA_ID, Favorite.START_TIME])
data class Favorite(
    @ColumnInfo(name = PLAYA_ID) val playaId: String,
    @ColumnInfo(name = START_TIME) val startTime: String = ""
) {
    companion object {
        const val TABLE_NAME = "favorites"
        const val PLAYA_ID = PlayaItem.PLAYA_ID
        const val START_TIME = Event.START_TIME
    }
}
