package com.gaiagps.iburn.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Flowable;

import static com.gaiagps.iburn.database.PlayaItem.NAME;
import static com.gaiagps.iburn.database.PlayaItem.PLAYA_ID;
import static com.gaiagps.iburn.database.UserPoi.TABLE_NAME;

/**
 * Created by dbro on 6/12/17.
 */

@Dao
public interface UserPoiDao {

    @Query("SELECT * FROM " + TABLE_NAME)
    Flowable<List<UserPoi>> getAll();

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + NAME + " LIKE :name")
    Flowable<List<UserPoi>> findByName(String name);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + PLAYA_ID + " LIKE :playaId")
    Flowable<UserPoi> findByPlayaId(String playaId);

    @Insert
    void insert(UserPoi... poi);

    @Update
    void update(UserPoi... poi);

    @Delete
    void delete(UserPoi... poi);
}
