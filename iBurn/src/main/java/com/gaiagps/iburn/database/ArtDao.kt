package com.gaiagps.iburn.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.SkipQueryVerification
import io.reactivex.Flowable

/**
 * Created by dbro on 6/8/17.
 */
@Dao
interface ArtDao {
    @get:Query(
        "SELECT a.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Art.TABLE_NAME + " a LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON a." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " ORDER BY " + PlayaItem.NAME
    )
    val all: Flowable<List<ArtWithUserData>>
    @get:Query(
        "SELECT a.*, 1 AS " + UserData.FAVORITE +
            " FROM " + Art.TABLE_NAME + " a INNER JOIN " + Favorite.TABLE_NAME +
            " f ON a." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID
    )
    val favorites: Flowable<List<ArtWithUserData>>

    @Query(
        "SELECT a.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Art.TABLE_NAME + " a LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON a." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " WHERE a." + PlayaItem.NAME + " LIKE :name"
    )
    fun findByName(name: String?): Flowable<List<ArtWithUserData>>

    @SkipQueryVerification
    @Query(
        "SELECT a.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Art.TABLE_NAME + " a LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON a." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " WHERE a." + PlayaItem.ID + " IN (SELECT rowid FROM arts_fts WHERE arts_fts MATCH :query)"
    )
    fun searchFts(query: String?): Flowable<List<ArtWithUserData>>

    @Query(
        "SELECT a.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Art.TABLE_NAME + " a LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON a." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " WHERE (a." + PlayaItem.LATITUDE + " BETWEEN :minLat AND :maxLat) " +
            "AND (a." + PlayaItem.LONGITUDE + " BETWEEN :minLon AND :maxLon)"
    )
    fun findInRegion(
        maxLat: Float,
        minLat: Float,
        maxLon: Float,
        minLon: Float
    ): Flowable<List<ArtWithUserData>>

    @Query(
        "SELECT a.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Art.TABLE_NAME + " a LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON a." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " WHERE f." + Favorite.PLAYA_ID + " IS NOT NULL OR ((a." + PlayaItem.LATITUDE +
            " BETWEEN :minLat AND :maxLat) AND (a." + PlayaItem.LONGITUDE +
            " BETWEEN :minLon AND :maxLon))"
    )
    fun findInRegionOrFavorite(
        maxLat: Float,
        minLat: Float,
        maxLon: Float,
        minLon: Float
    ): Flowable<List<ArtWithUserData>>

    //    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + AUDIO_TOUR_URL + " IS NOT NULL")
    //    Flowable<List<Art>> getAllWithAudioTour();
    @Insert
    fun insert(vararg arts: Art?)

    @Update
    fun update(vararg arts: Art?)

    @Query(
        "SELECT a.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Art.TABLE_NAME + " a LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON a." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " WHERE a." + PlayaItem.PLAYA_ID + " = :playaId"
    )
    fun findByPlayaId(playaId: String): Flowable<ArtWithUserData>

    @Query(
        "SELECT a.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Art.TABLE_NAME + " a LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON a." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " WHERE a." + PlayaItem.ID + " = :id"
    )
    fun findById(id: Int): Flowable<ArtWithUserData>
}