package com.gaiagps.iburn.database

import android.content.Context
import android.os.Parcelable
import android.text.TextUtils
import androidx.room.ColumnInfo
import androidx.room.Entity
import com.gaiagps.iburn.AudioTourManager
import kotlinx.parcelize.Parcelize

/**
 * Created by dbro on 6/8/17.
 */
@Parcelize
@Entity(tableName = Art.TABLE_NAME)
class Art : PlayaItem(), Parcelable {
    //    public static final String AUDIO_TOUR_URL = "a_url";
    @JvmField
    @ColumnInfo(name = ARTIST)
    var artist: String? = null

    @JvmField
    @ColumnInfo(name = ARTIST_LOCATION)
    var artistLocation: String? = null

    @JvmField
    @ColumnInfo(name = IMAGE_URL)
    var imageUrl: String? = null

    //    @ColumnInfo(name = AUDIO_TOUR_URL)
    //    public String audioTourUrl;
    fun hasAudioTour(context: Context): Boolean {
//        return !TextUtils.isEmpty(audioTourUrl);
        val pId = playaId ?: return false
        return AudioTourManager.hasAudioTour(context, pId)
    }

    fun hasImage(): Boolean {
        return !TextUtils.isEmpty(imageUrl)
    }

    companion object {
        const val TABLE_NAME: String = "arts"

        const val ARTIST: String = "artist"
        const val ARTIST_LOCATION: String = "a_loc"
        const val IMAGE_URL: String = "i_url"
    }
}