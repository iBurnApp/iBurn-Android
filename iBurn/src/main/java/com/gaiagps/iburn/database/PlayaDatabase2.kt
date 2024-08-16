package com.gaiagps.iburn.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.Date


// Changing this will trigger a force reload of the bundled database from assets
private const val DATABASE_NAME = "playaDatabase2024.3.db"

/**
 * If true, use a bundled pre-populated database. Else start with a fresh database.
 */
private const val USE_BUNDLED_DB = true

private const val DATABASE_V1 = 1

@Database(
    entities = arrayOf(Art::class, Camp::class, Event::class, UserPoi::class),
    version = DATABASE_V1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun artDao(): ArtDao
    abstract fun campDao(): CampDao
    abstract fun eventDao(): EventDao
    abstract fun userPoiDao(): UserPoiDao
}

private var sharedDb: AppDatabase? = null

fun copyDatabaseFromAssets(context: Context, assetPath: String, dbName: String) {
    val dbPath = context.getDatabasePath(dbName).absolutePath

    val dbFile = File(dbPath)
    if (!dbFile.exists()) {
        Timber.d("Copying bundled db $dbName from assets")
        dbFile.parentFile?.mkdirs()

        context.assets.open(assetPath).use { input ->
            FileOutputStream(dbPath).use { output ->
                val buffer = ByteArray(1024)
                var length: Int
                while (input.read(buffer).also { length = it } > 0) {
                    output.write(buffer, 0, length)
                }
            }
        }
    }
}

fun getSharedDb(context: Context): AppDatabase {

    val db = sharedDb
    if (db == null) {
        val builder = androidx.room.Room.databaseBuilder(
            context,
            AppDatabase::class.java, DATABASE_NAME
        )

        if (USE_BUNDLED_DB) {
            copyDatabaseFromAssets(context, "databases/$DATABASE_NAME", DATABASE_NAME)
        }
        val newDb = builder.build()

        sharedDb = newDb
        return newDb
    } else {
        return db
    }
}

object Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return (if (date == null) null else date!!.getTime())!!.toLong()
    }
}
