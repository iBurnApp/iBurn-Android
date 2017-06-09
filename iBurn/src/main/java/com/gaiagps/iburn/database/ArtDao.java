package com.gaiagps.iburn.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import io.reactivex.Flowable;

import static com.gaiagps.iburn.database.Art.AUDIO_TOUR_URL;
import static com.gaiagps.iburn.database.Art.TABLE_NAME;
import static com.gaiagps.iburn.database.PlayaItem.FAVORITE;
import static com.gaiagps.iburn.database.PlayaItem.NAME;

/**
 * Created by dbro on 6/8/17.
 */

@Dao
public interface ArtDao {

    @Query("SELECT * FROM " + TABLE_NAME)
    Flowable<List<Art>> getAll();

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + FAVORITE + " = 1")
    Flowable<List<Art>> getFavorites();

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + NAME + " LIKE :name")
    Flowable<List<Art>> findByName(String name);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + AUDIO_TOUR_URL + " IS NOT NULL")
    Flowable<List<Art>> getAllWithAudioTour();

    @Insert
    void insert(Art... arts);

    @Update
    void update(Art... arts);
}