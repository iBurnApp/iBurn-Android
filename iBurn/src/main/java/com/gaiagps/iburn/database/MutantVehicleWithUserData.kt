package com.gaiagps.iburn.database

import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize

@Parcelize
data class MutantVehicleWithUserData(
    @Embedded override val item: MutantVehicle,
    @Embedded override val userData: UserData
) : PlayaItemWithUserData, Parcelable
