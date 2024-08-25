package com.gaiagps.iburn.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Flowable;

import static com.gaiagps.iburn.database.Art.TABLE_NAME;
import static com.gaiagps.iburn.database.PlayaItem.FAVORITE;
import static com.gaiagps.iburn.database.PlayaItem.LATITUDE;
import static com.gaiagps.iburn.database.PlayaItem.LONGITUDE;
import static com.gaiagps.iburn.database.PlayaItem.NAME;
import static com.gaiagps.iburn.database.PlayaItem.PLAYA_ID;

/**
 * Created by dbro on 6/8/17.
 */

@Dao
public interface ArtDao {

    @Query("SELECT * FROM " + TABLE_NAME + " ORDER BY " + NAME)
    Flowable<List<Art>> getAll();

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + FAVORITE + " = 1")
    Flowable<List<Art>> getFavorites();

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + NAME + " LIKE :name")
    Flowable<List<Art>> findByName(String name);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE (" + LATITUDE + " BETWEEN :minLat AND :maxLat) AND (" + LONGITUDE + " BETWEEN :minLon AND :maxLon)")
    Flowable<List<Art>> findInRegion(float maxLat, float minLat, float maxLon, float minLon);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + FAVORITE + " = 1 OR ((" + LATITUDE + " BETWEEN :minLat AND :maxLat) AND (" + LONGITUDE + " BETWEEN :minLon AND :maxLon))")
    Flowable<List<Art>> findInRegionOrFavorite(float maxLat, float minLat, float maxLon, float minLon);

    @Query("UPDATE " + TABLE_NAME + " SET " + FAVORITE + " = :isFavorite WHERE " + PLAYA_ID + " in (:playaIds)")
    void updateFavorites(List<String> playaIds, boolean isFavorite);

//    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + AUDIO_TOUR_URL + " IS NOT NULL")
//    Flowable<List<Art>> getAllWithAudioTour();

    @Insert
    void insert(Art... arts);

    @Update
    void update(Art... arts);
}