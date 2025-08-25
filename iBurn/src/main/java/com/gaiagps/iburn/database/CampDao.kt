package com.gaiagps.iburn.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Created by dbro on 6/8/17.
 */
@Dao
interface CampDao {
    @get:Query(
        "SELECT c.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Camp.TABLE_NAME + " c LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON c." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " ORDER BY " + PlayaItem.NAME
    )
    val all: Flow<List<CampWithUserData>>

    @get:Query(
        "SELECT c.*, 1 AS " + UserData.FAVORITE +
            " FROM " + Camp.TABLE_NAME + " c INNER JOIN " + Favorite.TABLE_NAME +
            " f ON c." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID
    )
    val favorites: Flow<List<CampWithUserData>>

    @Query(
        "SELECT c.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Camp.TABLE_NAME + " c LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON c." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " WHERE c." + PlayaItem.NAME + " LIKE :name"
    )
    fun findByName(name: String?): Flow<List<CampWithUserData>>

    @Query(
        "SELECT c.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Camp.TABLE_NAME + " c LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON c." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " JOIN " + CampFts.TABLE_NAME +
            " ON c." + PlayaItem.ID + " = " + CampFts.TABLE_NAME + ".rowid" +
            " WHERE " + CampFts.TABLE_NAME + " MATCH :query"
    )
    fun searchFts(query: String?): Flow<List<CampWithUserData>>

    @Query(
        "SELECT c.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Camp.TABLE_NAME + " c LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON c." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " WHERE c." + PlayaItem.PLAYA_ID + " = :playaId"
    )
    fun findByPlayaId(playaId: String?): Flow<CampWithUserData>

    @Query(
        "SELECT c.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Camp.TABLE_NAME + " c LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON c." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " WHERE c." + PlayaItem.ID + " = :id"
    )
    fun findById(id: Int): Flow<CampWithUserData>

    @Query(
        "SELECT c.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Camp.TABLE_NAME + " c LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON c." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " WHERE (c." + PlayaItem.LATITUDE + " BETWEEN :minLat AND :maxLat) " +
            "AND (c." + PlayaItem.LONGITUDE + " BETWEEN :minLon AND :maxLon)"
    )
    fun findInRegion(
        maxLat: Float,
        minLat: Float,
        maxLon: Float,
        minLon: Float
    ): Flow<List<CampWithUserData>>

    @Query(
        "SELECT c.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Camp.TABLE_NAME + " c LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON c." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " WHERE f." + Favorite.PLAYA_ID + " IS NOT NULL OR ((c." + PlayaItem.LATITUDE +
            " BETWEEN :minLat AND :maxLat) AND (c." + PlayaItem.LONGITUDE +
            " BETWEEN :minLon AND :maxLon))"
    )
    fun findInRegionOrFavorite(
        maxLat: Float,
        minLat: Float,
        maxLon: Float,
        minLon: Float
    ): Flow<List<CampWithUserData>>

    @Insert
    fun insert(vararg camps: Camp?)

    @Update
    fun update(vararg camps: Camp?)
}
