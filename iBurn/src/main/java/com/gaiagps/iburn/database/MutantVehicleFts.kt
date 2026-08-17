package com.gaiagps.iburn.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = MutantVehicle::class)
@Entity(tableName = MutantVehicleFts.TABLE_NAME)
data class MutantVehicleFts(
    @ColumnInfo(name = PlayaItem.NAME) val name: String?,
    @ColumnInfo(name = PlayaItem.DESC) val description: String?,
    @ColumnInfo(name = MutantVehicle.ARTIST) val artist: String?,
    @ColumnInfo(name = MutantVehicle.HOMETOWN) val hometown: String?,
    @ColumnInfo(name = MutantVehicle.TAGS) val tags: String?
) {
    companion object { const val TABLE_NAME = "mutant_vehicles_fts" }
}
