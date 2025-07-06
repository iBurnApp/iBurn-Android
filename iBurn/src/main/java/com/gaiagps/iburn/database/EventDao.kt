package com.gaiagps.iburn.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.reactivex.Flowable
import io.reactivex.Single

/**
 * Created by dbro on 6/8/17.
 */
@Dao
interface EventDao {
    @get:Query("SELECT * FROM " + Event.TABLE_NAME)
    val all: Flowable<List<Event>>

    @Query("SELECT * FROM " + Event.TABLE_NAME + " WHERE " + PlayaItem.PLAYA_ID + " = :id")
    fun getByPlayaId(id: String?): Single<Event>

    @get:Query("SELECT * FROM " + Event.TABLE_NAME + " WHERE " + PlayaItem.FAVORITE + " = 1 ORDER BY " + Event.START_TIME)
    val favorites: Flowable<List<Event>>

    @Query("SELECT * FROM " + Event.TABLE_NAME + " WHERE " + PlayaItem.FAVORITE + " = 1 AND " + Event.END_TIME + " >= :now ORDER BY " + Event.START_TIME)
    fun getNonExpiredFavorites(now: String?): Flowable<List<Event>>

    @Query("SELECT * FROM " + Event.TABLE_NAME + " WHERE " + PlayaItem.NAME + " LIKE :name OR " + PlayaItem.DESC + " LIKE :name GROUP BY " + PlayaItem.NAME)
    fun findByName(name: String?): Flowable<List<Event>>

    @Query("SELECT * FROM " + Event.TABLE_NAME + " WHERE " + Event.CAMP_PLAYA_ID + " = :campPlayaId GROUP BY " + PlayaItem.NAME)
    fun findByCampPlayaId(campPlayaId: String?): Flowable<List<Event>>

    @Query("SELECT * FROM " + Event.TABLE_NAME + " WHERE " + PlayaItem.PLAYA_ID + " = :playaId AND " + PlayaItem.ID + " != :excludingId")
    fun findOtherOccurrences(playaId: String?, excludingId: Int): Flowable<List<Event>>


    //Event-related Queries
    @Query(
        ("SELECT * FROM " + Event.TABLE_NAME + " WHERE " +
                Event.START_TIME_PRETTY + " LIKE :day AND " +
                "not(s_time <= :allDayStart AND e_time >= :allDayEnd)" +
                "ORDER BY "
                + Event.ALL_DAY + ", " + Event.START_TIME + " ASC")
    )
    fun findByDayTimed(
        day: String?, allDayStart: String?,
        allDayEnd: String?
    ): Flowable<List<Event>>

    @Query(
        ("SELECT * FROM " + Event.TABLE_NAME + " WHERE (" + Event.START_TIME_PRETTY +
                " LIKE :day AND " + Event.END_TIME + ">= :now AND " +
                "not(s_time <= :allDayStart AND e_time >= :allDayEnd)" +
                " ) ORDER BY "
                + Event.ALL_DAY + ", " + Event.START_TIME + " ASC")
    )
    fun findByDayNoExpiredTimed(
        day: String?, now: String?,
        allDayStart: String?,
        allDayEnd: String?
    ): Flowable<List<Event>>

    @Query(
        ("SELECT * FROM " + Event.TABLE_NAME + " WHERE (" + Event.START_TIME_PRETTY +
                " LIKE :day AND " +
                "s_time<= :allDayStart AND e_time >= :allDayEnd" +
                " ) ORDER BY " + Event.ALL_DAY + ", " + Event.START_TIME + " ASC")
    )
    fun findByDayAllDay(
        day: String?,
        allDayStart: String?,
        allDayEnd: String?
    ): Flowable<List<Event>>

    @Query(
        ("SELECT * FROM " + Event.TABLE_NAME + " WHERE ("
                + Event.START_TIME_PRETTY + " LIKE :day AND " +
                "not(s_time <= :allDayStart AND e_time >= :allDayEnd) AND " +
                Event.TYPE + " IN (:types)) ORDER BY " + Event.ALL_DAY +
                ", " + Event.START_TIME + " ASC")
    )
    fun findByDayAndTypeTimed(
        day: String?, types: List<String?>?,
        allDayStart: String?,
        allDayEnd: String?
    ): Flowable<List<Event>>

    @Query(
        ("SELECT * FROM " + Event.TABLE_NAME +
                " WHERE (" + Event.START_TIME_PRETTY +
                " LIKE :day AND " +
                Event.END_TIME + ">= :now AND " +
                "not(s_time <= :allDayStart AND e_time >= :allDayEnd) AND " +
                Event.TYPE + " IN (:types)) ORDER BY " + Event.ALL_DAY + ", " + Event.START_TIME + " ASC")
    )
    fun findByDayAndTypeNoExpiredTimed(
        day: String?, types: List<String?>?, now: String?,
        allDayStart: String?, allDayEnd: String?
    ): Flowable<List<Event>>

    @Query(
        ("SELECT * FROM " + Event.TABLE_NAME +
                " WHERE (" + Event.START_TIME_PRETTY +
                " LIKE :day AND "
                + Event.TYPE + " IN (:types) AND " +
                "s_time <= :allDayStart AND e_time >= :allDayEnd " +
                ") ORDER BY " + Event.ALL_DAY + ", " + Event.START_TIME + " ASC")
    )
    fun findByDayAndTypeAllDay(
        day: String?,
        types: List<String?>?,
        allDayStart: String?,
        allDayEnd: String?
    ): Flowable<List<Event>>


    @Query("SELECT * FROM " + Event.TABLE_NAME + " WHERE " + Event.START_TIME + " BETWEEN :startDate AND :endDate AND " + Event.ALL_DAY + " = 0  ORDER BY " + Event.START_TIME)
    fun findInDateRange(startDate: String?, endDate: String?): Flowable<List<Event>>

    @Query("SELECT * FROM " + Event.TABLE_NAME + " WHERE (" + PlayaItem.LATITUDE + " BETWEEN :minLat AND :maxLat) AND (" + PlayaItem.LONGITUDE + " BETWEEN :minLon AND :maxLon)")
    fun findInRegion(
        maxLat: Float,
        minLat: Float,
        maxLon: Float,
        minLon: Float
    ): Flowable<List<Event>>

    @Query("SELECT * FROM " + Event.TABLE_NAME + " WHERE " + PlayaItem.FAVORITE + " = 1 OR ((" + PlayaItem.LATITUDE + " BETWEEN :minLat AND :maxLat) AND (" + PlayaItem.LONGITUDE + " BETWEEN :minLon AND :maxLon))")
    fun findInRegionOrFavorite(
        minLat: Float,
        maxLat: Float,
        minLon: Float,
        maxLon: Float
    ): Flowable<List<Event>>

    @Query("UPDATE " + Event.TABLE_NAME + " SET " + PlayaItem.FAVORITE + " = :isFavorite WHERE " + PlayaItem.PLAYA_ID + " in (:playaIds)")
    fun updateFavorites(playaIds: List<String?>?, isFavorite: Boolean)

    @Insert
    fun insert(vararg arts: Event?)

    @Update
    fun update(vararg arts: Event?)
}
