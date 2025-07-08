package com.gaiagps.iburn.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.gaiagps.iburn.BuildConfig
import io.reactivex.Single
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.Date


/**
 * If true, use a bundled pre-populated database. Else start with a fresh database.
 * The database file name is provided via BuildConfig.DATABASE_NAME.
 */
private const val USE_BUNDLED_DB = true

private const val DATABASE_V1 = 1

@Database(
    entities = arrayOf(
        Art::class,
        Camp::class,
        Event::class,
        ArtFts::class,
        CampFts::class,
        EventFts::class,
        UserPoi::class,
        Favorite::class
    ),
    version = DATABASE_V1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun artDao(): ArtDao
    abstract fun campDao(): CampDao
    abstract fun eventDao(): EventDao
    abstract fun userPoiDao(): UserPoiDao
    abstract fun favoriteDao(): FavoriteDao
}

private var sharedDb: AppDatabase? = null

fun copyDatabaseFromAssets(context: Context, assetPath: String, dbName: String) {
    val dbFile = context.getDatabasePath(dbName)
    val dbPath = dbFile.absolutePath

    dbFile.parentFile?.mkdirs()

    // Copy the bundled database to a temporary file first
    val tmpDb = File.createTempFile("iburn", ".db", context.cacheDir)
    context.assets.open(assetPath).use { input ->
        FileOutputStream(tmpDb).use { output ->
            val buffer = ByteArray(1024)
            var length: Int
            while (input.read(buffer).also { length = it } > 0) {
                output.write(buffer, 0, length)
            }
        }
    }

    if (!dbFile.exists()) {
        Timber.d("Copying bundled db $dbName from assets")
        tmpDb.copyTo(dbFile, overwrite = true)
    } else {
        Timber.d("Updating db $dbName from bundled assets")
        updateDatabaseTablesFromSource(
            sourceDbPath = tmpDb.absolutePath,
            destDbPath = dbPath,
            tables = listOf(Art.TABLE_NAME, Camp.TABLE_NAME, Event.TABLE_NAME)
        )
    }

    tmpDb.delete()
}

fun updateDatabaseTablesFromSource(sourceDbPath: String, destDbPath: String, tables: List<String>) {
    val db = SQLiteDatabase.openDatabase(destDbPath, null, SQLiteDatabase.OPEN_READWRITE)
    db.execSQL("ATTACH DATABASE '$sourceDbPath' AS newdb")
    try {
        db.beginTransaction()
        tables.forEach { table ->
            db.execSQL("DELETE FROM $table")
            db.execSQL("INSERT INTO $table SELECT * FROM newdb.$table")
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
        db.execSQL("DETACH DATABASE newdb")
        db.close()
    }
}

fun getSharedDb(context: Context): AppDatabase {

    val db = sharedDb
    if (db == null) {
        val newDb = buildDatabase(context, BuildConfig.DATABASE_NAME, USE_BUNDLED_DB)
        sharedDb = newDb
        return newDb
    } else {
        return db
    }
}

fun buildDatabase(context: Context, name: String, copyBundled: Boolean): AppDatabase {
    val builder = androidx.room.Room.databaseBuilder(
        context,
        AppDatabase::class.java, name
    )

    if (copyBundled) {
        copyDatabaseFromAssets(context, "databases/$name", name)
    }
    return builder.build()
}

fun newDatabase(context: Context, name: String): AppDatabase {
    return buildDatabase(context, name, false)
}

/**
 * Project SHAMEDD (Swift, Heroic, Action Mitigating Embarrassing Data Disaster)
 * Rescue favorites from any older 2024 database versions existing on device.
 * @return a Single containing the number of favorites and user pois rescued
 */
fun migrateOldFavorites(context: Context): Single<Int> {
    return Single.create<Int> { emitter ->
        try {
            val oldDatabaseNames = arrayOf("playaDatabase2024.1.db", "playaDatabase2024.2.db")

            val favPlayaIds = ArrayList<String>()
            val userPois = ArrayList<UserPoi>()

            oldDatabaseNames.forEach { oldDatabaseName ->
                val oldDatabasePath = context.getDatabasePath(oldDatabaseName)

                if (oldDatabasePath.exists()) {
                    val odb =
                        SQLiteDatabase.openDatabase(
                            oldDatabasePath.path,
                            null,
                            SQLiteDatabase.OPEN_READONLY
                        )

                    // Example raw SQL query to fetch data from the old database
                    val artFavsCursor = odb.rawQuery("SELECT p_id FROM arts WHERE fav == 1", null)
                    val campFavsCursor = odb.rawQuery("SELECT p_id FROM camps WHERE fav == 1", null)
                    val eventFavsCursor =
                        odb.rawQuery("SELECT p_id FROM events WHERE fav == 1", null)
                    val userPoisCursor = odb.rawQuery("SELECT * FROM user_pois", null)

                    favPlayaIds.apply {
                        addAll(extractStrings(artFavsCursor))
                        addAll(extractStrings(campFavsCursor))
                        addAll(extractStrings(eventFavsCursor))
                    }
                    userPois.addAll(extractUserPoi(userPoisCursor))
                }

            }
            DataProvider.getInstance(context)
                .subscribe(
                    {
                        it.updateFavorites(favPlayaIds, true)
                        it.insertUserPois(userPois)
                        Timber.d("Recovered {${favPlayaIds.size} favorites and ${userPois.size} user POIs")
                        val totalRescued = favPlayaIds.size + userPois.size
                        emitter.onSuccess(totalRescued)
                    },
                    { error ->
                        Timber.e(error, "Favorite recovery failed")
                        emitter.onError(error)
                    })
        } catch (e: Exception) {
            emitter.onError(e)
        }
    }
}

fun extractStrings(cursor: Cursor): List<String> {
    val ids = ArrayList<String>()
    if (cursor.moveToFirst()) {
        do {
            ids.add(cursor.getString(0))
        } while (cursor.moveToNext())
    }
    cursor.close()
    return ids
}

fun extractUserPoi(cursor: Cursor): List<UserPoi> {
    val userPois = ArrayList<UserPoi>()
    if (cursor.moveToFirst()) {
        do {
            val poi = UserPoi().apply {
                icon = cursor.getString(cursor.getColumnIndexOrThrow(UserPoi.ICON))
                name = cursor.getString(cursor.getColumnIndexOrThrow(PlayaItem.NAME))
                latitude = cursor.getFloat(cursor.getColumnIndexOrThrow(PlayaItem.LATITUDE))
                longitude = cursor.getFloat(cursor.getColumnIndexOrThrow(PlayaItem.LONGITUDE))
                playaId = cursor.getString(cursor.getColumnIndexOrThrow(PlayaItem.PLAYA_ID))
            }
            userPois.add(poi)
        } while (cursor.moveToNext())
    }
    cursor.close()
    return userPois
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
