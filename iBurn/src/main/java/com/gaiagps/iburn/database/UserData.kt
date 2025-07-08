package com.gaiagps.iburn.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserData(
    @ColumnInfo(name = FAVORITE) val isFavorite: Boolean
) : Parcelable {
    companion object {
        const val FAVORITE: String = "user_fav"

    }
}
