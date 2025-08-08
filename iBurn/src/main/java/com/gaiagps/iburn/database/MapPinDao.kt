package com.gaiagps.iburn.database

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface MapPinDao {
    
    @Query("SELECT * FROM ${MapPin.TABLE_NAME} ORDER BY ${MapPin.CREATED_AT} DESC")
    fun getAllPins(): Flowable<List<MapPin>>
    
    @Query("SELECT * FROM ${MapPin.TABLE_NAME} WHERE ${MapPin.UID} = :uid")
    fun getByUid(uid: String): Single<MapPin>
    
    @Query("SELECT * FROM ${MapPin.TABLE_NAME} WHERE ${MapPin.ID} = :id")
    fun getById(id: Int): Single<MapPin>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(pin: MapPin): Completable
    
    @Update
    fun update(pin: MapPin): Completable
    
    @Delete
    fun delete(pin: MapPin): Completable
    
    @Query("DELETE FROM ${MapPin.TABLE_NAME} WHERE ${MapPin.UID} = :uid")
    fun deleteByUid(uid: String): Completable
    
    @Query("DELETE FROM ${MapPin.TABLE_NAME}")
    fun deleteAll(): Completable
}