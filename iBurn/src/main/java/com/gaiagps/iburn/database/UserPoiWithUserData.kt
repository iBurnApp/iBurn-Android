package com.gaiagps.iburn.database

import androidx.room.Embedded

data class UserPoiWithUserData(
    @Embedded override val item: UserPoi,
    @Embedded override val userData: UserData
) : PlayaItemWithUserData