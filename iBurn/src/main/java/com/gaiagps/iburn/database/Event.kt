package com.gaiagps.iburn.database

import android.arch.persistence.room.*
import com.gaiagps.iburn.database.Event.Companion.ColCampPlayaId
import com.gaiagps.iburn.database.Event.Companion.ColStartTime
import com.gaiagps.iburn.database.Event.Companion.ColStartTimePretty
import com.gaiagps.iburn.database.Event.Companion.ColType
import com.gaiagps.iburn.database.Event.Companion.TableName
import io.reactivex.Observable
import java.util.*

/**
 * Created by dbro on 6/6/17.
 */

@Entity(tableName = TableName)
class Event(
        id:             Int = 0,
        name:           String,
        description:    String,
        url:            String,
        contact:        String,
        playaAddress:   String,
        playaId:        String,
        location:       Location,
        isFavorite:     Boolean,

        @ColumnInfo(name = ColType)              val type: String,
        @ColumnInfo(name = ColAllDay)            val allDay: Boolean,
        @ColumnInfo(name = ColCheckLocation)     val checkLocation: Boolean,
        @ColumnInfo(name = ColCampPlayaId)       val campPlayaId: Int,
        @ColumnInfo(name = ColStartTime)         val startTime: Date,
        @ColumnInfo(name = ColStartTimePretty)   val startTimePretty: String,
        @ColumnInfo(name = ColEndTime)           val endTime: Date,
        @ColumnInfo(name = ColEndTimePretty)     val endTimePretty: String)

    : PlayaItem(id, name, description, url, contact, playaAddress, playaId, location, isFavorite) {

    companion object {
        const val TableName = "events"

        const val ColType = "type"
        const val ColAllDay = "all_day"
        const val ColCheckLocation = "check_location"
        const val ColCampPlayaId = "camp_playa_id"
        const val ColStartTime = "start_time"
        const val ColStartTimePretty = "start_time_pretty"
        const val ColEndTime = "end_time"
        const val ColEndTimePretty = "end_time_pretty"
    }
}

@Dao
interface EventDao {

    @Query("SELECT * FROM $TableName")
    fun getAll(): Observable<List<Event>>

    @Query("SELECT * FROM $TableName")
    fun getFavorites(): Observable<List<Event>>

    @Query("SELECT * FROM $TableName WHERE $ColCampPlayaId = :campPlayaId ORDER BY $ColStartTime")
    fun findByCampPlayaId(campPlayaId: Int): Observable<List<Event>>

    @Query("SELECT * FROM $TableName WHERE $ColStartTimePretty LIKE :day% ORDER BY $ColStartTime")
    fun findByDay(day: String): Observable<List<Event>>

    @Query("SELECT * FROM $TableName WHERE ( start_time_pretty LIKE :day% AND $ColType IN (:types) )  ORDER BY $ColStartTime")
    fun findByDayAndType(day: String, types: List<String>): Observable<List<Event>>

    @Query("SELECT * FROM $TableName WHERE name LIKE :name GROUP BY $") // GROUP_BY name eliminates duplicate entries for separate occurrences
    fun findByName(name: String): Observable<List<Event>>

    @Insert
    fun insert(vararg event: Event)

    @Update
    fun update(vararg events: Event)

    @Delete
    fun delete(event: Event)
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}