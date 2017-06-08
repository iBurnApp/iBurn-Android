package com.gaiagps.iburn.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.content.Context


/**
 * Created by dbro on 6/6/17.
 */
private const val DATABASE_V1 = 1

@Database(entities = arrayOf(Art::class, Camp::class, Event::class), version = DATABASE_V1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun artDao(): ArtDao
    abstract fun campDao(): CampDao
    abstract fun eventDao(): EventDao
}

private var sharedDb: AppDatabase? = null

fun getSharedDb(context: Context): AppDatabase {

    val db = sharedDb
    if (db == null) {
        val builder = android.arch.persistence.room.Room.databaseBuilder(
                context,
                AppDatabase::class.java, "home-db")
        // TODO : Possible to optionally use bundled db?
        val newDb = builder.build()
        sharedDb = newDb
        return newDb
    } else {
        return db
    }
}