package com.gaiagps.iburn.database

import android.os.Parcelable
import android.text.TextUtils
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = MutantVehicle.TABLE_NAME,
    indices = [
        Index(value = [PlayaItem.PLAYA_ID], unique = true),
        Index(value = [PlayaItem.NAME])
    ]
)
class MutantVehicle : PlayaItem(), Parcelable {
    @JvmField
    @ColumnInfo(name = ARTIST)
    var artist: String? = null

    @JvmField
    @ColumnInfo(name = HOMETOWN)
    var hometown: String? = null

    @JvmField
    @ColumnInfo(name = IMAGE_URL)
    var imageUrl: String? = null

    @JvmField
    @ColumnInfo(name = TAGS)
    var tags: String? = null

    fun hasImage(): Boolean = !TextUtils.isEmpty(imageUrl)

    companion object {
        const val TABLE_NAME = "mutant_vehicles"
        const val ARTIST = "artist"
        const val HOMETOWN = "hometown"
        const val IMAGE_URL = "i_url"
        const val TAGS = "tags"
    }
}
