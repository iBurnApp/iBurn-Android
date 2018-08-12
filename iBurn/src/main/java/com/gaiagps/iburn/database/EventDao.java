package com.gaiagps.iburn.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import io.reactivex.Flowable;

import static com.gaiagps.iburn.database.Event.ALL_DAY;
import static com.gaiagps.iburn.database.Event.CAMP_PLAYA_ID;
import static com.gaiagps.iburn.database.Event.END_TIME;
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

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + FAVORITE + " = 1 AND " + END_TIME + " >= :now")
    Flowable<List<Event>> getNonExpiredFavorites(String now);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + NAME + " LIKE :name GROUP BY " + NAME)
    Flowable<List<Event>> findByName(String name);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + CAMP_PLAYA_ID + " = :campPlayaId GROUP BY " + NAME)
    Flowable<List<Event>> findByCampPlayaId(String campPlayaId);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + PLAYA_ID + " = :playaId AND " + ID + " != :excludingId")
    Flowable<List<Event>> findOtherOccurrences(String playaId, int excludingId);



    //Event-related Queries
    @Query("SELECT * FROM " + TABLE_NAME + " WHERE " +
            START_TIME_PRETTY + " LIKE :day AND "+
            "not(s_time <= :allDayStart AND e_time >= :allDayEnd)"+
            "ORDER BY "
            + ALL_DAY + ", " + START_TIME + " ASC")
    Flowable<List<Event>> findByDayTimed(String day,String allDayStart,
                                         String allDayEnd);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE (" + START_TIME_PRETTY +
            " LIKE :day AND " + END_TIME + ">= :now AND " +
            "not(s_time <= :allDayStart AND e_time >= :allDayEnd)"+
            " ) ORDER BY "
            + ALL_DAY + ", " + START_TIME + " ASC")
    Flowable<List<Event>> findByDayNoExpiredTimed(String day,String now,
                                                  String allDayStart,
                                                  String allDayEnd);

    @Query("SELECT * FROM " + TABLE_NAME + " WHERE (" + START_TIME_PRETTY +
            " LIKE :day AND "+
            "s_time<= :allDayStart AND e_time >= :allDayEnd"+
            " ) ORDER BY " + ALL_DAY + ", " + START_TIME + " ASC")
    Flowable<List<Event>> findByDayAllDay(String day,
                                                    String allDayStart,
                                                    String allDayEnd);

      @Query("SELECT * FROM " + TABLE_NAME + " WHERE ("
              + START_TIME_PRETTY + " LIKE :day AND " +
              "not(s_time <= :allDayStart AND e_time >= :allDayEnd) AND "+
              TYPE + " IN (:types)) ORDER BY " + ALL_DAY +
              ", " + START_TIME + " ASC")
    Flowable<List<Event>> findByDayAndTypeTimed(String day, List<String> types,
                                                String allDayStart,
                                                String allDayEnd);

    @Query("SELECT * FROM " + TABLE_NAME +
            " WHERE (" + START_TIME_PRETTY +
            " LIKE :day AND " +
            END_TIME + ">= :now AND "+
            "not(s_time <= :allDayStart AND e_time >= :allDayEnd) AND "+
             TYPE + " IN (:types)) ORDER BY " + ALL_DAY + ", " + START_TIME + " ASC")
    Flowable<List<Event>> findByDayAndTypeNoExpiredTimed
            (String day, List<String> types, String now,
             String allDayStart, String allDayEnd);

    @Query("SELECT * FROM " + TABLE_NAME +
            " WHERE (" + START_TIME_PRETTY +
            " LIKE :day AND "
            + TYPE + " IN (:types) AND "+
            "s_time <= :allDayStart AND e_time >= :allDayEnd "+
            ") ORDER BY " + ALL_DAY + ", " + START_TIME + " ASC")
    Flowable<List<Event>> findByDayAndTypeAllDay(String day, List<String> types,
                                                          String allDayStart,
                                                          String allDayEnd);


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
