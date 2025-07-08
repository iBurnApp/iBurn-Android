package com.gaiagps.iburn.database

import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize

@Parcelize
data class EventWithUserData(
    @Embedded override val item: Event,
    @Embedded override val userData: UserData
) : PlayaItemWithUserData, Parcelable