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
    @get:Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME
    )
    val all: Flowable<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + PlayaItem.PLAYA_ID + " = :id"
    )
    fun getByPlayaId(id: String?): Single<EventWithUserData>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + PlayaItem.ID + " = :id"
    )
    fun getById(id: Int): Single<EventWithUserData>

    @get:Query(
        "SELECT e.*, 1 AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e INNER JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " ORDER BY e." + Event.START_TIME
    )
    val favorites: Flowable<List<EventWithUserData>>

    @Query(
        "SELECT e.*, 1 AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e INNER JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + Event.END_TIME + " >= :now ORDER BY e." + Event.START_TIME
    )
    fun getNonExpiredFavorites(now: String?): Flowable<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + PlayaItem.NAME + " LIKE :name OR e." + PlayaItem.DESC + " LIKE :name GROUP BY e." + PlayaItem.NAME
    )
    fun findByName(name: String?): Flowable<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " JOIN " + EventFts.TABLE_NAME +
            " ON e." + PlayaItem.ID + " = " + EventFts.TABLE_NAME + ".rowid" +
            " WHERE " + EventFts.TABLE_NAME + " MATCH :query"
    )
    fun searchFts(query: String?): Flowable<List<EventWithUserData>>


    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + Event.CAMP_PLAYA_ID + " = :campPlayaId GROUP BY e." + PlayaItem.NAME
    )
    fun findByCampPlayaId(campPlayaId: String?): Flowable<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + PlayaItem.PLAYA_ID + " = :playaId AND e." + PlayaItem.ID + " != :excludingId"
    )
    fun findOtherOccurrences(playaId: String?, excludingId: Int): Flowable<List<EventWithUserData>>


    //Event-related Queries
    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + Event.START_TIME_PRETTY + " LIKE :day AND " +
            "not(e." + Event.START_TIME + " <= :allDayStart AND e." + Event.END_TIME + " >= :allDayEnd)" +
            " ORDER BY " + Event.ALL_DAY + ", " + Event.START_TIME + " ASC"
    )
    fun findByDayTimed(
        day: String?, allDayStart: String?,
        allDayEnd: String?
    ): Flowable<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE (e." + Event.START_TIME_PRETTY +
            " LIKE :day AND e." + Event.END_TIME + ">= :now AND " +
            "not(e." + Event.START_TIME + " <= :allDayStart AND e." + Event.END_TIME + " >= :allDayEnd)) ORDER BY " +
            Event.ALL_DAY + ", " + Event.START_TIME + " ASC"
    )
    fun findByDayNoExpiredTimed(
        day: String?, now: String?,
        allDayStart: String?,
        allDayEnd: String?
    ): Flowable<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE (e." + Event.START_TIME_PRETTY +
            " LIKE :day AND e." + Event.START_TIME + " <= :allDayStart AND e." + Event.END_TIME + " >= :allDayEnd) ORDER BY " +
            Event.ALL_DAY + ", " + Event.START_TIME + " ASC"
    )
    fun findByDayAllDay(
        day: String?,
        allDayStart: String?,
        allDayEnd: String?
    ): Flowable<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE (e." + Event.START_TIME_PRETTY +
            " LIKE :day AND not(e." + Event.START_TIME + " <= :allDayStart AND e." + Event.END_TIME + " >= :allDayEnd) AND e." + Event.TYPE + " IN (:types)) ORDER BY " +
            Event.ALL_DAY + ", " + Event.START_TIME + " ASC"
    )
    fun findByDayAndTypeTimed(
        day: String?, types: List<String?>?,
        allDayStart: String?,
        allDayEnd: String?
    ): Flowable<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE (e." + Event.START_TIME_PRETTY +
            " LIKE :day AND e." + Event.END_TIME + ">= :now AND not(e." + Event.START_TIME + " <= :allDayStart AND e." + Event.END_TIME + " >= :allDayEnd) AND e." + Event.TYPE + " IN (:types)) ORDER BY " +
            Event.ALL_DAY + ", " + Event.START_TIME + " ASC"
    )
    fun findByDayAndTypeNoExpiredTimed(
        day: String?, types: List<String?>?, now: String?,
        allDayStart: String?, allDayEnd: String?
    ): Flowable<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE (e." + Event.START_TIME_PRETTY +
            " LIKE :day AND e." + Event.TYPE + " IN (:types) AND e." + Event.START_TIME + " <= :allDayStart AND e." + Event.END_TIME + " >= :allDayEnd) ORDER BY " +
            Event.ALL_DAY + ", " + Event.START_TIME + " ASC"
    )
    fun findByDayAndTypeAllDay(
        day: String?,
        types: List<String?>?,
        allDayStart: String?,
        allDayEnd: String?
    ): Flowable<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + Event.START_TIME + " BETWEEN :startDate AND :endDate AND e." + Event.ALL_DAY + " = 0 ORDER BY e." + Event.START_TIME
    )
    fun findInDateRange(startDate: String?, endDate: String?): Flowable<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE (e." + PlayaItem.LATITUDE + " BETWEEN :minLat AND :maxLat) " +
            "AND (e." + PlayaItem.LONGITUDE + " BETWEEN :minLon AND :maxLon)"
    )
    fun findInRegion(
        maxLat: Float,
        minLat: Float,
        maxLon: Float,
        minLon: Float
    ): Flowable<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.TABLE_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + PlayaItem.PLAYA_ID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE f." + Favorite.PLAYA_ID + " IS NOT NULL OR ((e." + PlayaItem.LATITUDE +
            " BETWEEN :minLat AND :maxLat) AND (e." + PlayaItem.LONGITUDE +
            " BETWEEN :minLon AND :maxLon))"
    )
    fun findInRegionOrFavorite(
        minLat: Float,
        maxLat: Float,
        minLon: Float,
        maxLon: Float
    ): Flowable<List<EventWithUserData>>

    @Insert
    fun insert(vararg arts: Event?)

    @Update
    fun update(vararg arts: Event?)
}
