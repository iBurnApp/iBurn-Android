package com.gaiagps.iburn.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MutantVehicleDao {
    @get:Query(
        "SELECT v.*, CASE WHEN f.${Favorite.PLAYA_ID} IS NOT NULL THEN 1 ELSE 0 END AS ${UserData.FAVORITE} " +
            "FROM ${MutantVehicle.TABLE_NAME} v LEFT JOIN ${Favorite.TABLE_NAME} f " +
            "ON v.${PlayaItem.PLAYA_ID} = f.${Favorite.PLAYA_ID} ORDER BY ${PlayaItem.NAME}"
    )
    val all: Flow<List<MutantVehicleWithUserData>>

    @get:Query(
        "SELECT v.*, 1 AS ${UserData.FAVORITE} FROM ${MutantVehicle.TABLE_NAME} v " +
            "INNER JOIN ${Favorite.TABLE_NAME} f ON v.${PlayaItem.PLAYA_ID} = f.${Favorite.PLAYA_ID}"
    )
    val favorites: Flow<List<MutantVehicleWithUserData>>

    @Query(
        "SELECT v.*, CASE WHEN f.${Favorite.PLAYA_ID} IS NOT NULL THEN 1 ELSE 0 END AS ${UserData.FAVORITE} " +
            "FROM ${MutantVehicle.TABLE_NAME} v LEFT JOIN ${Favorite.TABLE_NAME} f " +
            "ON v.${PlayaItem.PLAYA_ID} = f.${Favorite.PLAYA_ID} JOIN ${MutantVehicleFts.TABLE_NAME} " +
            "ON v.${PlayaItem.ID} = ${MutantVehicleFts.TABLE_NAME}.rowid " +
            "WHERE ${MutantVehicleFts.TABLE_NAME} MATCH :query"
    )
    fun searchFts(query: String?): Flow<List<MutantVehicleWithUserData>>

    @Query(
        "SELECT v.*, CASE WHEN f.${Favorite.PLAYA_ID} IS NOT NULL THEN 1 ELSE 0 END AS ${UserData.FAVORITE} " +
            "FROM ${MutantVehicle.TABLE_NAME} v LEFT JOIN ${Favorite.TABLE_NAME} f " +
            "ON v.${PlayaItem.PLAYA_ID} = f.${Favorite.PLAYA_ID} WHERE v.${PlayaItem.PLAYA_ID} = :playaId"
    )
    fun findByPlayaId(playaId: String): Flow<MutantVehicleWithUserData>

    @Query(
        "SELECT v.*, CASE WHEN f.${Favorite.PLAYA_ID} IS NOT NULL THEN 1 ELSE 0 END AS ${UserData.FAVORITE} " +
            "FROM ${MutantVehicle.TABLE_NAME} v LEFT JOIN ${Favorite.TABLE_NAME} f " +
            "ON v.${PlayaItem.PLAYA_ID} = f.${Favorite.PLAYA_ID} WHERE v.${PlayaItem.ID} = :id"
    )
    fun findById(id: Int): Flow<MutantVehicleWithUserData>

    @Update
    fun update(vararg vehicles: MutantVehicle?)
}
