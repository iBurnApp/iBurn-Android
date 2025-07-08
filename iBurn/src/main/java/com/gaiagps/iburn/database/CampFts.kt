package com.gaiagps.iburn.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = Camp::class)
@Entity(tableName = CampFts.TABLE_NAME)
data class CampFts(
    @ColumnInfo(name = PlayaItem.NAME) val name: String?,
    @ColumnInfo(name = PlayaItem.DESC) val description: String?,
    @ColumnInfo(name = Camp.HOMETOWN) val hometown: String?
) {
    companion object { const val TABLE_NAME = "camps_fts" }
}
