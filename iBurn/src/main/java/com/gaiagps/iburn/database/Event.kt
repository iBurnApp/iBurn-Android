package com.gaiagps.iburn.database

import android.os.Parcelable
import android.text.TextUtils
import androidx.room.ColumnInfo
import androidx.room.Entity
import kotlinx.parcelize.Parcelize

/**
 * Created by dbro on 6/8/17.
 */
@Parcelize
@Entity(tableName = Event.TABLE_NAME)
class Event : PlayaItem(), Parcelable {
    @ColumnInfo(name = TYPE)
    var type: String? = null

    @ColumnInfo(name = ALL_DAY)
    var allDay: Boolean = false

    @ColumnInfo(name = CHECK_LOC)
    var checkLocation: Boolean = false

    @JvmField
    @ColumnInfo(name = CAMP_PLAYA_ID)
    var campPlayaId: String? = null

    @JvmField
    @ColumnInfo(name = ART_PLAYA_ID)
    var artPlayaId: String? = null

    @JvmField
    @ColumnInfo(name = START_TIME)
    var startTime: String? = null

    @JvmField
    @ColumnInfo(name = START_TIME_PRETTY)
    var startTimePretty: String? = null

    @JvmField
    @ColumnInfo(name = END_TIME)
    var endTime: String? = null

    @JvmField
    @ColumnInfo(name = END_TIME_PRETTY)
    var endTimePretty: String? = null

    fun hasCampHost(): Boolean {
        return campPlayaId.isNullOrEmpty().not()
    }

    fun hasArtHost(): Boolean {
        return artPlayaId.isNullOrEmpty().not()
    }

    companion object {
        const val TABLE_NAME: String = "events"

        const val TYPE: String = "e_type"
        const val ALL_DAY: String = "all_day"
        const val CHECK_LOC: String = "check_loc"
        const val CAMP_PLAYA_ID: String = "c_id"
        const val ART_PLAYA_ID: String = "a_id"
        const val START_TIME: String = "s_time"
        const val START_TIME_PRETTY: String = "s_time_p"
        const val END_TIME: String = "e_time"
        const val END_TIME_PRETTY: String = "e_time_p"
    }
}