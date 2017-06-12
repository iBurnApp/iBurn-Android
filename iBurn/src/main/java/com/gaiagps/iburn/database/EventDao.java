package com.gaiagps.iburn.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import io.reactivex.Flowable;

import static com.gaiagps.iburn.database.Event.ALL_DAY;
import static com.gaiagps.iburn.database.Event.CAMP_PLAYA_ID;
import static com.gaiagps.iburn.database.Event.START_TIME;
import static com.gaiagps.iburn.database.Event.START_TIME_PRETTY;
import static com.gaiagps.iburn.database.Event.TABLE_NAME;
import static com.gaiagps.iburn.database.Event.TYPE;
import static com.gaiagps.iburn.database.PlayaItem.FAVORITE;
import static com.gaiagps.iburn.database.PlayaItem.ID;
import static com.gaiagps.iburn.database.PlayaItem.LATITUDE;
import static com.gaiagps.iburn.database.PlayaItem.LONGITUDE;
import static com.gaiagps.iburn.database.PlayaItem.NAME;
import static com.gaiagps.iburn.database.PlayaItem.PLAYA_ID;

/**
 * Created by dbro on 6/8/17.
 */

@Dao
public interface EventDao {
    @Query("SELECT * FROM " + TABLE_NAME)
    Flowable<List<Event>> getAll();

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + FAVORITE + " = 1")
    Flowable<List<Event>> getFavorites();

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + NAME + " LIKE :name GROUP BY " + NAME)
    Flowable<List<Event>> findByName(String name);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + CAMP_PLAYA_ID + " = :campPlayaId GROUP BY " + NAME)
    Flowable<List<Event>> findByCampPlayaId(String campPlayaId);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + PLAYA_ID + " = :playaId AND " + ID + " != :excludingId")
    Flowable<List<Event>> findOtherOccurrences(String playaId, int excludingId);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + START_TIME_PRETTY + " LIKE :day ORDER BY " + ALL_DAY + ", " + START_TIME + " ASC")
    Flowable<List<Event>> findByDay(String day);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE (" + START_TIME_PRETTY + " LIKE :day AND " + TYPE + " IN (:types)) ORDER BY " + ALL_DAY + ", " + START_TIME + " ASC")
    Flowable<List<Event>> findByDayAndType(String day, List<String> types);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + START_TIME + " BETWEEN :startDate AND :endDate AND " + ALL_DAY + " = 0  ORDER BY " + START_TIME)
    Flowable<List<Event>> findInDateRange(String startDate, String endDate);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE (" + LATITUDE + " BETWEEN :minLat AND :maxLat) AND (" + LONGITUDE + " BETWEEN :minLon AND :maxLon)")
    Flowable<List<Event>> findInRegion(float maxLat, float minLat, float maxLon, float minLon);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + FAVORITE + " = 1 OR ((" + LATITUDE + " BETWEEN :minLat AND :maxLat) AND (" + LONGITUDE + " BETWEEN :minLon AND :maxLon))")
    Flowable<List<Event>> findInRegionOrFavorite(float minLat, float maxLat, float minLon, float maxLon);

    @Insert
    void insert(Event... arts);

    @Update
    void update(Event... arts);
}
