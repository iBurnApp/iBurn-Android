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
    @Query(
        "SELECT u.*, " +
                "CASE WHEN f.${Favorite.PLAYA_ID} IS NOT NULL THEN 1 ELSE 0 END AS ${UserData.FAVORITE} " +
                "FROM ${UserPoi.TABLE_NAME} u " +
                "LEFT JOIN ${Favorite.TABLE_NAME} f " +
                "ON u.${PlayaItem.PLAYA_ID} = f.${Favorite.PLAYA_ID}"
    )
    fun getAll(): Flowable<List<UserPoiWithUserData>>

    @Query(
        "SELECT u.*, " +
                "CASE WHEN f.${Favorite.PLAYA_ID} IS NOT NULL THEN 1 ELSE 0 END AS ${UserData.FAVORITE} " +
                "FROM ${UserPoi.TABLE_NAME} u " +
                "LEFT JOIN ${Favorite.TABLE_NAME} f " +
                "ON u.${PlayaItem.PLAYA_ID} = f.${Favorite.PLAYA_ID} " +
                "WHERE u.${PlayaItem.NAME} LIKE :name"
    )
    fun findByName(name: String?): Flowable<List<UserPoiWithUserData>>

    @Query(
        "SELECT u.*, " +
                "CASE WHEN f.${Favorite.PLAYA_ID} IS NOT NULL THEN 1 ELSE 0 END AS ${UserData.FAVORITE} " +
                "FROM ${UserPoi.TABLE_NAME} u " +
                "LEFT JOIN ${Favorite.TABLE_NAME} f " +
                "ON u.${PlayaItem.PLAYA_ID} = f.${Favorite.PLAYA_ID} " +
                "WHERE u.${PlayaItem.PLAYA_ID} LIKE :playaId"
    )
    fun findByPlayaId(playaId: String?): Flowable<UserPoiWithUserData>

    @Insert
    fun insert(vararg poi: UserPoi?)

    @Update
    fun update(vararg poi: UserPoi?)

    @Delete
    fun delete(vararg poi: UserPoi?)
}
