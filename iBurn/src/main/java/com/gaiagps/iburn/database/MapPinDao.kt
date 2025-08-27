package com.gaiagps.iburn.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MapPinDao {
    
    @Query("SELECT * FROM ${MapPin.TABLE_NAME} ORDER BY ${MapPin.CREATED_AT} DESC")
    fun getAllPins(): Flow<List<MapPin>>
    
    @Query("SELECT * FROM ${MapPin.TABLE_NAME} WHERE ${MapPin.UID} = :uid")
    suspend fun getByUid(uid: String): MapPin
    
    @Query("SELECT * FROM ${MapPin.TABLE_NAME} WHERE ${MapPin.ID} = :id")
    suspend fun getById(id: Int): MapPin
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuspend(pin: MapPin)
    
    @Update
    suspend fun updateSuspend(pin: MapPin)
    
    @Delete
    suspend fun deleteSuspend(pin: MapPin)
    
    @Query("DELETE FROM ${MapPin.TABLE_NAME} WHERE ${MapPin.UID} = :uid")
    suspend fun deleteByUidSuspend(uid: String)
    
    @Query("DELETE FROM ${MapPin.TABLE_NAME}")
    suspend fun deleteAllSuspend()
}
