package com.gaiagps.iburn.database

/**
 * Created by dbro on 6/6/17.
 */
/*
Kotlin models on ice pending release of https://youtrack.jetbrains.com/issue/KT-18048
abstract class PlayaItem(
        @PrimaryKey         @ColumnInfo(name = ColId)           val id: Int = 0,
                            @ColumnInfo(name = ColName)         val name: String,
                            @ColumnInfo(name = ColDesc)         val description: String,
                            @ColumnInfo(name = ColUrl)          val url: String,
                            @ColumnInfo(name = ColContact)      val contact: String,
                            @ColumnInfo(name = ColPlayaAddress) val playaAddress: String?,
                            @ColumnInfo(name = ColPlayaId)      val playaId: String,
                            @Embedded                           val location: Location,
                            @ColumnInfo(name = ColFavorite)     val isFavorite: Boolean) : Serializable {

    companion object {
        const val ColId = "id"
        const val ColName = "name"
        const val ColDesc = "description"
        const val ColUrl = "url"
        const val ColContact = "contact"
        const val ColPlayaAddress = "playa_address"
        const val ColPlayaId = "playa_id"
        const val ColFavorite = "favorite"
    }
}

class Location(@ColumnInfo(name = ColLatitude)      val latitude: Float,
               @ColumnInfo(name = ColLongitude)     val longitude: Float) {

    companion object {
        const val ColLatitude = "latitude"
        const val ColLongitude = "longitude"
    }
}
        */