package com.gaiagps.iburn.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import io.reactivex.Flowable;

import static com.gaiagps.iburn.database.UserPoi.TABLE_NAME;

/**
 * Created by dbro on 6/12/17.
 */

@Dao
public interface UserPoiDao {

    @Query("SELECT * FROM " + TABLE_NAME)
    Flowable<List<UserPoi>> getAll();

    @Insert
    void insert(UserPoi... poi);

    @Update
    void update(UserPoi... poi);
}
