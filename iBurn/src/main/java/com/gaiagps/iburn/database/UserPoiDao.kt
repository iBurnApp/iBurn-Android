package com.gaiagps.iburn.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.reactivex.Flowable

/**
 * Created by dbro on 6/12/17.
 */
@Dao
interface UserPoiDao {
    @get:Query("SELECT * FROM " + UserPoi.TABLE_NAME)
    val all: Flowable<List<UserPoi>>

    @Query("SELECT * FROM " + UserPoi.TABLE_NAME + " WHERE " + PlayaItem.NAME + " LIKE :name")
    fun findByName(name: String?): Flowable<List<UserPoi>>

    @Query("SELECT * FROM " + UserPoi.TABLE_NAME + " WHERE " + PlayaItem.PLAYA_ID + " LIKE :playaId")
    fun findByPlayaId(playaId: String?): Flowable<UserPoi>

    @Insert
    fun insert(vararg poi: UserPoi?)

    @Update
    fun update(vararg poi: UserPoi?)

    @Delete
    fun delete(vararg poi: UserPoi?)
}
