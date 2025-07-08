package com.gaiagps.iburn.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import kotlinx.parcelize.Parcelize

/**
 * Created by dbro on 6/8/17.
 */
@Parcelize
@Entity(tableName = Camp.TABLE_NAME)
class Camp : PlayaItem(), Parcelable {
    @JvmField
    @ColumnInfo(name = HOMETOWN)
    var hometown: String? = null

    companion object {
        const val TABLE_NAME: String = "camps"

        const val HOMETOWN: String = "hometown"
    }
}