package com.gaiagps.iburn.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = MapPin.TABLE_NAME,
    indices = [Index(value = [MapPin.UID], name = "index_map_pins_uid", unique = false)]
)
@Parcelize
data class MapPin(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    val id: Int = 0,
    
    @ColumnInfo(name = UID)
    val uid: String,
    
    @ColumnInfo(name = TITLE)
    val title: String,
    
    @ColumnInfo(name = DESCRIPTION)
    val description: String? = null,
    
    @ColumnInfo(name = LATITUDE)
    val latitude: Float,
    
    @ColumnInfo(name = LONGITUDE)
    val longitude: Float,
    
    @ColumnInfo(name = ADDRESS)
    val address: String? = null,
    
    @ColumnInfo(name = COLOR)
    val color: String = "red",
    
    @ColumnInfo(name = ICON)
    val icon: String? = null,
    
    @ColumnInfo(name = CREATED_AT)
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = NOTES)
    val notes: String? = null
) : Parcelable {
    
    companion object {
        const val TABLE_NAME = "map_pins"
        const val ID = "id"
        const val UID = "uid"
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val ADDRESS = "address"
        const val COLOR = "color"
        const val ICON = "icon"
        const val CREATED_AT = "created_at"
        const val NOTES = "notes"
    }
}