package com.gaiagps.iburn.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gaiagps.iburn.BuildConfig
import com.gaiagps.iburn.PrefsHelper
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.Date


/**
 * If true, use a bundled pre-populated database. Else start with a fresh database.
 * The database file name is provided via BuildConfig.DATABASE_NAME.
 */
private const val USE_BUNDLED_DB = true

// Database file name in app's /data partition
// This database is often derived from the bundled database in assets but to avoid confusion with the
// bundled database and to avoid storing multiple versions of the bundled database in /data, we use a fixed name.
private const val APP_DATABASE_NAME = "playaDatabase2025.1.db"

private const val DATABASE_V1 = 1
// Add event artPlayaId and MapPin for pin deep links
private const val DATABASE_V2 = 2

// Tables that are read-only and copied from the bundled database when version is more recent than
// the database currently installed in app's /data partition. These should contain no user created
// data to avoid data loss.
private val READONLY_TABLES =  listOf(Art.TABLE_NAME, Camp.TABLE_NAME, Event.TABLE_NAME)

@Database(
    entities = arrayOf(
        Art::class,
        Camp::class,
        Event::class,
        ArtFts::class,
        CampFts::class,
        EventFts::class,
        UserPoi::class,
        Favorite::class,
        MapPin::class
    ),
    version = DATABASE_V2
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun artDao(): ArtDao
    abstract fun campDao(): CampDao
    abstract fun eventDao(): EventDao
    abstract fun userPoiDao(): UserPoiDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun mapPinDao(): MapPinDao
}

private var sharedDb: AppDatabase? = null

fun copyDatabaseFromAssets(context: Context, assetPath: String, destinationDbName: String) {
    val dbFile = context.getDatabasePath(destinationDbName)
    val dbPath = dbFile.absolutePath

    dbFile.parentFile?.mkdirs()

    // Copy the bundled database to a temporary file first
    val tmpAssetDb = File.createTempFile("iburn", ".db", context.cacheDir)
    context.assets.open(assetPath).use { input ->
        FileOutputStream(tmpAssetDb).use { output ->
            val buffer = ByteArray(1024)
            var length: Int
            while (input.read(buffer).also { length = it } > 0) {
                output.write(buffer, 0, length)
            }
        }
    }

    Timber.d("Copied bundled db to temp file ${tmpAssetDb.absolutePath} with size ${tmpAssetDb.length()}")

    if (!dbFile.exists()) {
        Timber.d("Copying bundled db $tmpAssetDb to $dbPath")
        tmpAssetDb.copyTo(dbFile, overwrite = true)
    } else {
        Timber.d("Updating db $dbPath from bundled assets")
        updateDatabaseTablesFromSource(
            sourceDbPath = tmpAssetDb.absolutePath,
            destDbPath = dbPath,
            tables = READONLY_TABLES
        )
    }

    tmpAssetDb.delete()
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
        val newDb = buildDatabase(context, APP_DATABASE_NAME, USE_BUNDLED_DB)
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
        .addMigrations(MIGRATION_1_2)

    if (copyBundled) {
        val prefs = PrefsHelper(context)
        if (prefs.ingestedDatabaseName != BuildConfig.BUNDLED_DATABASE_NAME) {
            Timber.d("Updating from bundled db. '${prefs.ingestedDatabaseName}' (Last ingested version) -> '${BuildConfig.BUNDLED_DATABASE_NAME}' (Bundled version)")
            // Always copy from the current bundled DB asset, but write to a fixed on-device name
            // to avoid multiple copies in /data as the bundled DB version changes.
            copyDatabaseFromAssets(
                context,
                assetPath = "databases/${BuildConfig.BUNDLED_DATABASE_NAME}",
                destinationDbName = name
            )
            prefs.ingestedDatabaseName = BuildConfig.BUNDLED_DATABASE_NAME;
        }
    }
    return builder.build()
}

fun newDatabase(context: Context, name: String): AppDatabase {
    return buildDatabase(context, name, false)
}


// Migration from version 1 to 2: Add map_pins table
val MIGRATION_1_2 = object : Migration(DATABASE_V1, DATABASE_V2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `${MapPin.TABLE_NAME}` (
                `${MapPin.ID}` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `${MapPin.UID}` TEXT NOT NULL,
                `${MapPin.TITLE}` TEXT NOT NULL,
                `${MapPin.DESCRIPTION}` TEXT,
                `${MapPin.LATITUDE}` REAL NOT NULL,
                `${MapPin.LONGITUDE}` REAL NOT NULL,
                `${MapPin.ADDRESS}` TEXT,
                `${MapPin.COLOR}` TEXT NOT NULL,
                `${MapPin.ICON}` TEXT,
                `${MapPin.CREATED_AT}` INTEGER NOT NULL,
                `${MapPin.NOTES}` TEXT
            )
        """)
        
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_map_pins_uid` ON `${MapPin.TABLE_NAME}` (`${MapPin.UID}`)")

        // Add a_id column to Event table
        database.execSQL("""
            ALTER TABLE `${Event.TABLE_NAME}`
            ADD COLUMN `${Event.ART_PLAYA_ID}` TEXT
        """)
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
