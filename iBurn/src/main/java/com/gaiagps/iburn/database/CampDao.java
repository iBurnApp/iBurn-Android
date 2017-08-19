package com.gaiagps.iburn.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import io.reactivex.Flowable;

import static com.gaiagps.iburn.database.Camp.TABLE_NAME;
import static com.gaiagps.iburn.database.PlayaItem.FAVORITE;
import static com.gaiagps.iburn.database.PlayaItem.LATITUDE;
import static com.gaiagps.iburn.database.PlayaItem.LONGITUDE;
import static com.gaiagps.iburn.database.PlayaItem.NAME;
import static com.gaiagps.iburn.database.PlayaItem.PLAYA_ID;

/**
 * Created by dbro on 6/8/17.
 */

@Dao
public interface CampDao {

    @Query("SELECT * FROM " + TABLE_NAME + " ORDER BY " + NAME)
    Flowable<List<Camp>> getAll();

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + FAVORITE + " = 1")
    Flowable<List<Camp>> getFavorites();

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + NAME + " LIKE :name")
    Flowable<List<Camp>> findByName(String name);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + PLAYA_ID + " = :playaId")
    Flowable<Camp> findByPlayaId(String playaId);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE (" + LATITUDE + " BETWEEN :minLat AND :maxLat) AND (" + LONGITUDE + " BETWEEN :minLon AND :maxLon)")
    Flowable<List<Camp>> findInRegion(float maxLat, float minLat, float maxLon, float minLon);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + FAVORITE + " = 1 OR ((" + LATITUDE + " BETWEEN :minLat AND :maxLat) AND (" + LONGITUDE + " BETWEEN :minLon AND :maxLon))")
    Flowable<List<Camp>> findInRegionOrFavorite(float maxLat, float minLat, float maxLon, float minLon);

    @Insert
    void insert(Camp... camps);

    @Update
    void update(Camp... camps);
}