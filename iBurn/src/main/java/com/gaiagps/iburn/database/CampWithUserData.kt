package com.gaiagps.iburn.database

import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize

@Parcelize
data class CampWithUserData(
    @Embedded override val item: Camp,
    @Embedded override val userData: UserData
) : PlayaItemWithUserData, Parcelable