package com.gaiagps.iburn.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Ignore
import com.gaiagps.iburn.DateUtil
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Created by dbro on 6/8/17.
 */
@Parcelize
@DatabaseView(value = Event.VIEW_QUERY, viewName = Event.VIEW_NAME)
class Event : PlayaItem(), Parcelable {
    @JvmField
    @ColumnInfo(name = EVENT_ID)
    var eventId: Int = 0

    @JvmField
    @ColumnInfo(name = EVENT_UID)
    var eventUid: String? = null

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
    var startTime: Long = 0

    @JvmField
    @ColumnInfo(name = END_TIME)
    var endTime: Long = 0

    @get:Ignore
    val startDate: Date
        get() = Date(startTime)

    @get:Ignore
    val endDate: Date
        get() = Date(endTime)

    @get:Ignore
    val startTimePretty: String
        get() = DateUtil.getPlayaTimeFormat(
            if (allDay) "EE M/d" else "EE M/d h:mm a"
        ).format(startDate)

    @get:Ignore
    val endTimePretty: String
        get() = DateUtil.getPlayaTimeFormat(
            if (allDay) "EE M/d" else "EE M/d h:mm a"
        ).format(endDate)

    fun hasCampHost(): Boolean {
        return campPlayaId.isNullOrEmpty().not()
    }

    fun hasArtHost(): Boolean {
        return artPlayaId.isNullOrEmpty().not()
    }

    companion object {
        const val VIEW_NAME: String = "event_occurrence_rows"
        const val EVENT_ID: String = "event_id"
        const val EVENT_UID: String = "event_uid"

        const val TYPE: String = "e_type"
        const val ALL_DAY: String = "all_day"
        const val CHECK_LOC: String = "check_loc"
        const val CAMP_PLAYA_ID: String = "c_id"
        const val ART_PLAYA_ID: String = "a_id"
        const val START_TIME: String = "s_time"
        const val START_TIME_PRETTY: String = "s_time_p"
        const val END_TIME: String = "e_time"
        const val END_TIME_PRETTY: String = "e_time_p"

        const val VIEW_QUERY: String =
            "SELECT o._id AS _id, d.name AS name, d.`desc` AS `desc`, d.url AS url, " +
                "d.contact AS contact, d.p_addr AS p_addr, d.p_addr_unof AS p_addr_unof, " +
                "o.p_id AS p_id, d.lat AS lat, d.lon AS lon, d.lat_unof AS lat_unof, " +
                "d.lon_unof AS lon_unof, d.e_type AS e_type, d.all_day AS all_day, " +
                "d.check_loc AS check_loc, d.c_id AS c_id, d.a_id AS a_id, " +
                "o.event_id AS event_id, d.p_id AS event_uid, o.s_time AS s_time, " +
                "o.e_time AS e_time " +
                "FROM events d JOIN event_occurrences o ON o.event_id = d._id"
    }
}
