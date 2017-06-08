package com.gaiagps.iburn.database

import android.arch.persistence.room.*
import io.reactivex.Flowable
import android.arch.persistence.room.Update
import com.gaiagps.iburn.database.Camp.Companion.TableName
import com.gaiagps.iburn.database.PlayaItem.Companion.ColFavorite
import com.gaiagps.iburn.database.PlayaItem.Companion.ColName
import io.reactivex.Observable


/**
 * Created by dbro on 6/6/17.
 */
@Entity(tableName = TableName)
class Camp(
        id:             Int = 0,
        name:           String,
        description:    String,
        url:            String,
        contact:        String,
        playaAddress:   String,
        playaId:        String,
        location:       Location,
        isFavorite:     Boolean,

        @ColumnInfo(name = ColHometown)     val hometown: String)

    : PlayaItem(id, name, description, url, contact, playaAddress, playaId, location, isFavorite) {

    companion object {
        const val TableName = "camps"

        const val ColHometown = "hometown"
    }
}

@Dao
interface CampDao {

    @Query("SELECT * FROM $TableName")
    fun getAll(): Observable<List<Camp>>

    @Query("SELECT * FROM $TableName WHERE $ColFavorite = 1")
    fun getFavorites(): Observable<List<Camp>>

    @Query("SELECT * FROM $TableName WHERE $ColName LIKE :name")
    fun findByName(name: String): Observable<List<Camp>>

    @Insert
    fun insert(vararg camps: Camp)

    @Update
    fun update(vararg camps: Camp)
}