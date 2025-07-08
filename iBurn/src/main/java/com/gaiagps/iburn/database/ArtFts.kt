package com.gaiagps.iburn.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = Art::class)
@Entity(tableName = ArtFts.TABLE_NAME)
data class ArtFts(
    @ColumnInfo(name = PlayaItem.NAME) val name: String?,
    @ColumnInfo(name = PlayaItem.DESC) val description: String?,
    @ColumnInfo(name = Art.ARTIST) val artist: String?,
    @ColumnInfo(name = Art.ARTIST_LOCATION) val artistLocation: String?
) {
    companion object { const val TABLE_NAME = "arts_fts" }
}
