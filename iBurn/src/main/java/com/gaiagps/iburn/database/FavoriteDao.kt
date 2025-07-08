package com.gaiagps.iburn.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Flowable

@Dao
interface FavoriteDao {
    @get:Query("SELECT * FROM " + Favorite.TABLE_NAME)
    val all: Flowable<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg fav: Favorite)

    @Query("DELETE FROM " + Favorite.TABLE_NAME + " WHERE " + Favorite.PLAYA_ID + " = :playaId AND " + Favorite.START_TIME + " = :startTime")
    fun delete(playaId: String, startTime: String)

    @Query("DELETE FROM " + Favorite.TABLE_NAME + " WHERE " + Favorite.PLAYA_ID + " IN (:playaIds)")
    fun deleteByPlayaIds(playaIds: List<String>)

    @Query("SELECT COUNT(*) FROM " + Favorite.TABLE_NAME + " WHERE " + Favorite.PLAYA_ID + " = :playaId AND " + Favorite.START_TIME + " = :startTime")
    fun count(playaId: String, startTime: String): Int
}
