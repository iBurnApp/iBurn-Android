package com.gaiagps.iburn.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import io.reactivex.Flowable;

import static com.gaiagps.iburn.database.Event.TABLE_NAME;
import static com.gaiagps.iburn.database.PlayaItem.FAVORITE;
import static com.gaiagps.iburn.database.PlayaItem.NAME;

/**
 * Created by dbro on 6/8/17.
 */

@Dao
public interface EventDao {

    @Query("SELECT * FROM " + TABLE_NAME)
    Flowable<List<Event>> getAll();

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + FAVORITE + " = 1")
    Flowable<List<Event>> getFavorites();

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + NAME + " LIKE :name")
    Flowable<List<Event>> findByName(String name);

    @Insert
    void insert(Event... arts);

    @Update
    void update(Event... arts);
}
