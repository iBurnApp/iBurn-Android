package com.gaiagps.iburn.database

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Created by dbro on 6/8/17.
 */
@Dao
interface EventDao {
    @Query(
        "SELECT " +
            "(SELECT MIN(" + Event.START_TIME + ") FROM " + EventOccurrence.TABLE_NAME + ") " +
            "AS first_start_time, " +
            "(SELECT MAX(" + Event.END_TIME + ") FROM " + EventOccurrence.TABLE_NAME + ") " +
            "AS last_end_time"
    )
    suspend fun getDateRange(): EventDateRange

    @get:Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME
    )
    val all: Flow<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + PlayaItem.PLAYA_ID + " = :id OR e." + Event.EVENT_UID +
            " = :id ORDER BY e." + Event.START_TIME + " LIMIT 1"
    )
    suspend fun getByPlayaId(id: String?): EventWithUserData

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + PlayaItem.ID + " = :id"
    )
    suspend fun getById(id: Int): EventWithUserData

    @get:Query(
        "SELECT e.*, 1 AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e INNER JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " ORDER BY e." + Event.START_TIME
    )
    val favorites: Flow<List<EventWithUserData>>

    @Query(
        "SELECT e.*, 1 AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e INNER JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + Event.END_TIME + " >= :now ORDER BY e." + Event.START_TIME
    )
    fun getNonExpiredFavorites(now: Long): Flow<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + PlayaItem.NAME + " LIKE :name OR e." + PlayaItem.DESC + " LIKE :name GROUP BY e." + PlayaItem.NAME
    )
    fun findByName(name: String?): Flow<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " JOIN " + EventFts.TABLE_NAME +
            " ON e." + Event.EVENT_ID + " = " + EventFts.TABLE_NAME + ".rowid" +
            " WHERE " + EventFts.TABLE_NAME + " MATCH :query"
    )
    fun searchFts(query: String?): Flow<List<EventWithUserData>>


    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + Event.CAMP_PLAYA_ID + " = :campPlayaId GROUP BY e." + PlayaItem.NAME
    )
    fun findByCampPlayaId(campPlayaId: String?): Flow<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + Event.EVENT_ID + " = :eventId AND e." + PlayaItem.ID + " != :excludingId" +
            " ORDER BY e." + Event.START_TIME
    )
    fun findOtherOccurrences(eventId: Int, excludingId: Int): Flow<List<EventWithUserData>>


    //Event-related Queries
    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + Event.START_TIME + " >= :dayStart AND e." +
            Event.START_TIME + " < :dayEnd" +
            " ORDER BY " + Event.ALL_DAY + ", " + Event.START_TIME + " ASC"
    )
    fun findByDay(
        dayStart: Long,
        dayEnd: Long
    ): Flow<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE (e." + Event.START_TIME + " >= :dayStart AND e." +
            Event.START_TIME + " < :dayEnd AND e." + Event.END_TIME + " >= :now) ORDER BY " +
            Event.ALL_DAY + ", " + Event.START_TIME + " ASC"
    )
    fun findByDayNoExpired(
        dayStart: Long,
        dayEnd: Long,
        now: Long
    ): Flow<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE (e." + Event.START_TIME + " >= :dayStart AND e." +
            Event.START_TIME + " < :dayEnd AND e." + Event.TYPE + " IN (:types)) ORDER BY " +
            Event.ALL_DAY + ", " + Event.START_TIME + " ASC"
    )
    fun findByDayAndType(
        dayStart: Long,
        dayEnd: Long,
        types: List<String?>?
    ): Flow<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE (e." + Event.START_TIME + " >= :dayStart AND e." +
            Event.START_TIME + " < :dayEnd AND e." + Event.END_TIME +
            " >= :now AND e." + Event.TYPE + " IN (:types)) ORDER BY " +
            Event.ALL_DAY + ", " + Event.START_TIME + " ASC"
    )
    fun findByDayAndTypeNoExpired(
        dayStart: Long,
        dayEnd: Long,
        types: List<String?>?,
        now: Long
    ): Flow<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + Event.START_TIME + " BETWEEN :startDate AND :endDate AND e." + Event.ALL_DAY + " = 0 ORDER BY e." + Event.START_TIME
    )
    fun findInDateRange(startDate: Long, endDate: Long): Flow<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE (e." + PlayaItem.LATITUDE + " BETWEEN :minLat AND :maxLat) " +
            "AND (e." + PlayaItem.LONGITUDE + " BETWEEN :minLon AND :maxLon)"
    )
    fun findInRegion(
        maxLat: Float,
        minLat: Float,
        maxLon: Float,
        minLon: Float
    ): Flow<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
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
    ): Flow<List<EventWithUserData>>

    // All days queries (for showing events across all days)
    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " ORDER BY " + Event.ALL_DAY + ", " + Event.START_TIME + " ASC"
    )
    fun findAllDays(): Flow<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + Event.END_TIME + " >= :now ORDER BY " +
            Event.ALL_DAY + ", " + Event.START_TIME + " ASC"
    )
    fun findAllDaysNoExpired(now: Long): Flow<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + Event.TYPE + " IN (:types) ORDER BY " +
            Event.ALL_DAY + ", " + Event.START_TIME + " ASC"
    )
    fun findAllDaysAndType(types: List<String?>?): Flow<List<EventWithUserData>>

    @Query(
        "SELECT e.*, CASE WHEN f." + Favorite.PLAYA_ID +
            " IS NOT NULL THEN 1 ELSE 0 END AS " + UserData.FAVORITE +
            " FROM " + Event.VIEW_NAME + " e LEFT JOIN " + Favorite.TABLE_NAME +
            " f ON e." + Event.EVENT_UID + " = f." + Favorite.PLAYA_ID +
            " AND e." + Event.START_TIME + " = f." + Favorite.START_TIME +
            " WHERE e." + Event.END_TIME + " >= :now AND e." +
            Event.TYPE + " IN (:types) ORDER BY " +
            Event.ALL_DAY + ", " + Event.START_TIME + " ASC"
    )
    fun findAllDaysAndTypeNoExpired(
        types: List<String?>?,
        now: Long
    ): Flow<List<EventWithUserData>>

}
