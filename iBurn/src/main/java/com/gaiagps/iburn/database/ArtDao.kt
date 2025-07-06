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
interface ArtDao {
    @get:Query("SELECT * FROM " + Art.TABLE_NAME + " ORDER BY " + PlayaItem.NAME)
    val all: Flowable<List<Art>>

    @get:Query("SELECT * FROM " + Art.TABLE_NAME + " WHERE " + PlayaItem.FAVORITE + " = 1")
    val favorites: Flowable<List<Art>>

    @Query("SELECT * FROM " + Art.TABLE_NAME + " WHERE " + PlayaItem.NAME + " LIKE :name")
    fun findByName(name: String?): Flowable<List<Art>>

    @Query("SELECT * FROM " + Art.TABLE_NAME + " WHERE (" + PlayaItem.LATITUDE + " BETWEEN :minLat AND :maxLat) AND (" + PlayaItem.LONGITUDE + " BETWEEN :minLon AND :maxLon)")
    fun findInRegion(
        maxLat: Float,
        minLat: Float,
        maxLon: Float,
        minLon: Float
    ): Flowable<List<Art>>

    @Query("SELECT * FROM " + Art.TABLE_NAME + " WHERE " + PlayaItem.FAVORITE + " = 1 OR ((" + PlayaItem.LATITUDE + " BETWEEN :minLat AND :maxLat) AND (" + PlayaItem.LONGITUDE + " BETWEEN :minLon AND :maxLon))")
    fun findInRegionOrFavorite(
        maxLat: Float,
        minLat: Float,
        maxLon: Float,
        minLon: Float
    ): Flowable<List<Art>>

    @Query("UPDATE " + Art.TABLE_NAME + " SET " + PlayaItem.FAVORITE + " = :isFavorite WHERE " + PlayaItem.PLAYA_ID + " in (:playaIds)")
    fun updateFavorites(playaIds: List<String?>?, isFavorite: Boolean)

    //    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + AUDIO_TOUR_URL + " IS NOT NULL")
    //    Flowable<List<Art>> getAllWithAudioTour();
    @Insert
    fun insert(vararg arts: Art?)

    @Update
    fun update(vararg arts: Art?)
}