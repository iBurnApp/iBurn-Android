package com.gaiagps.iburn.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

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
