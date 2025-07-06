package com.gaiagps.iburn.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.reactivex.Flowable

/**
 * Created by dbro on 6/8/17.
 */
@Dao
interface CampDao {
    @get:Query("SELECT * FROM " + Camp.TABLE_NAME + " ORDER BY " + PlayaItem.NAME)
    val all: Flowable<List<Camp>>

    @get:Query("SELECT * FROM " + Camp.TABLE_NAME + " WHERE " + PlayaItem.FAVORITE + " = 1")
    val favorites: Flowable<List<Camp>>

    @Query("SELECT * FROM " + Camp.TABLE_NAME + " WHERE " + PlayaItem.NAME + " LIKE :name")
    fun findByName(name: String?): Flowable<List<Camp>>

    @Query("SELECT * FROM " + Camp.TABLE_NAME + " WHERE " + PlayaItem.PLAYA_ID + " = :playaId")
    fun findByPlayaId(playaId: String?): Flowable<Camp>

    @Query("SELECT * FROM " + Camp.TABLE_NAME + " WHERE (" + PlayaItem.LATITUDE + " BETWEEN :minLat AND :maxLat) AND (" + PlayaItem.LONGITUDE + " BETWEEN :minLon AND :maxLon)")
    fun findInRegion(
        maxLat: Float,
        minLat: Float,
        maxLon: Float,
        minLon: Float
    ): Flowable<List<Camp>>

    @Query("SELECT * FROM " + Camp.TABLE_NAME + " WHERE " + PlayaItem.FAVORITE + " = 1 OR ((" + PlayaItem.LATITUDE + " BETWEEN :minLat AND :maxLat) AND (" + PlayaItem.LONGITUDE + " BETWEEN :minLon AND :maxLon))")
    fun findInRegionOrFavorite(
        maxLat: Float,
        minLat: Float,
        maxLon: Float,
        minLon: Float
    ): Flowable<List<Camp>>

    @Query("UPDATE " + Camp.TABLE_NAME + " SET " + PlayaItem.FAVORITE + " = :isFavorite WHERE " + PlayaItem.PLAYA_ID + " in (:playaIds)")
    fun updateFavorites(playaIds: List<String?>?, isFavorite: Boolean)

    @Insert
    fun insert(vararg camps: Camp?)

    @Update
    fun update(vararg camps: Camp?)
}