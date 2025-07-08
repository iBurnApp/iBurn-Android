package com.gaiagps.iburn.database

import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize

@Parcelize
data class ArtWithUserData(
    @Embedded override val item: Art,
    @Embedded override val userData: UserData
) : PlayaItemWithUserData, Parcelable