package com.gaiagps.iburn.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.maplibre.android.geometry.LatLng

/**
 * Created by dbro on 6/8/17.
 */
@Parcelize
open class PlayaItem : Parcelable {
    @JvmField
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Int = 0

    @JvmField
    @ColumnInfo(name = NAME)
    var name: String? = null

    @JvmField
    @ColumnInfo(name = DESC)
    var description: String? = null

    @ColumnInfo(name = URL)
    var url: String? = null

    @ColumnInfo(name = CONTACT)
    var contact: String? = null

    @JvmField
    @ColumnInfo(name = PLAYA_ADDR)
    var playaAddress: String? = null

    @JvmField
    @ColumnInfo(name = PLAYA_ADDR_UNOFFICIAL)
    var playaAddressUnofficial: String? = null

    @JvmField
    @ColumnInfo(name = PLAYA_ID)
    var playaId: String? = null

    @JvmField
    @ColumnInfo(name = LATITUDE)
    var latitude: Float = 0f

    @JvmField
    @ColumnInfo(name = LONGITUDE)
    var longitude: Float = 0f

    @JvmField
    @ColumnInfo(name = LATITUDE_UNOFFICIAL)
    var latitudeUnofficial: Float = 0f

    @JvmField
    @ColumnInfo(name = LONGITUDE_UNOFFICIAL)
    var longitudeUnofficial: Float = 0f

    fun hasLocation(): Boolean {
        return latitude != 0f && longitude != 0f
    }

    fun hasUnofficialLocation(): Boolean {
        return latitudeUnofficial != 0f && longitudeUnofficial != 0f
    }

    val latLng: LatLng
        get() = LatLng(latitude.toDouble(), longitude.toDouble())

    val unofficialLatLng: LatLng
        get() = LatLng(latitudeUnofficial.toDouble(), longitudeUnofficial.toDouble())

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val playaItem = o as PlayaItem

        if (id != playaItem.id) return false
        return if (playaId != null) (playaId == playaItem.playaId) else playaItem.playaId == null
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (if (playaId != null) playaId.hashCode() else 0)
        return result
    }

    companion object {
        const val ID: String = "_id"
        const val NAME: String = "name"
        const val DESC: String = "desc"
        const val URL: String = "url"
        const val CONTACT: String = "contact"
        const val PLAYA_ADDR: String = "p_addr"
        const val PLAYA_ADDR_UNOFFICIAL: String = "p_addr_unof"
        const val PLAYA_ID: String = "p_id"
        const val LATITUDE: String = "lat"
        const val LONGITUDE: String = "lon"
        const val LATITUDE_UNOFFICIAL: String = "lat_unof"
        const val LONGITUDE_UNOFFICIAL: String = "lon_unof"
    }
}
