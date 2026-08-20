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
private const val APP_DATABASE_NAME = "playaDatabase.db"

private const val DATABASE_V1 = 1
// Add event artPlayaId and MapPin for pin deep links
private const val DATABASE_V2 = 2
// Normalize event definitions and occurrences and index bundled data.
private const val DATABASE_V3 = 3
// Store occurrence timestamps as epochs and remove derived presentation columns.
private const val DATABASE_V4 = 4
// Add the browseable Mutant Vehicles feed.
private const val DATABASE_V5 = 5
// Add art hometown. Art URL and contact email are already stored by PlayaItem.
private const val DATABASE_V6 = 6

private const val EVENT_VIEW_QUERY_V3 =
    "SELECT o._id AS _id, d.name AS name, d.`desc` AS `desc`, d.url AS url, " +
        "d.contact AS contact, d.p_addr AS p_addr, d.p_addr_unof AS p_addr_unof, " +
        "o.p_id AS p_id, d.lat AS lat, d.lon AS lon, d.lat_unof AS lat_unof, " +
        "d.lon_unof AS lon_unof, d.e_type AS e_type, d.all_day AS all_day, " +
        "d.check_loc AS check_loc, d.c_id AS c_id, d.a_id AS a_id, " +
        "o.event_id AS event_id, d.p_id AS event_uid, o.s_time AS s_time, " +
        "o.s_time_p AS s_time_p, o.e_time AS e_time, o.e_time_p AS e_time_p " +
        "FROM events d JOIN event_occurrences o ON o.event_id = d._id"

// Tables that are read-only and copied from the bundled database when version is more recent than
// the database currently installed in app's /data partition. These should contain no user created
// data to avoid data loss.
private val READONLY_TABLES = listOf(
    Art.TABLE_NAME,
    Camp.TABLE_NAME,
    EventDefinition.TABLE_NAME,
    EventOccurrence.TABLE_NAME,
    MutantVehicle.TABLE_NAME
)

@Database(
    entities = arrayOf(
        Art::class,
        Camp::class,
        EventDefinition::class,
        EventOccurrence::class,
        ArtFts::class,
        CampFts::class,
        EventFts::class,
        MutantVehicle::class,
        MutantVehicleFts::class,
        UserPoi::class,
        Favorite::class,
        MapPin::class
    ),
    views = [Event::class],
    version = DATABASE_V6
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun artDao(): ArtDao
    abstract fun campDao(): CampDao
    abstract fun eventDao(): EventDao
    abstract fun mutantVehicleDao(): MutantVehicleDao
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

    check(dbFile.exists()) { "Destination Room database must exist before importing bundled tables" }
    Timber.d("Updating db $dbPath from bundled assets")
    updateDatabaseTablesFromSource(
        sourceDbPath = tmpAssetDb.absolutePath,
        destDbPath = dbPath,
        tables = READONLY_TABLES
    )

    tmpAssetDb.delete()
}

fun updateDatabaseTablesFromSource(sourceDbPath: String, destDbPath: String, tables: List<String>) {
    val db = SQLiteDatabase.openDatabase(destDbPath, null, SQLiteDatabase.OPEN_READWRITE)
    db.execSQL("ATTACH DATABASE '$sourceDbPath' AS newdb")
    try {
        db.beginTransaction()
        fun columns(pragma: String): List<String> = db.rawQuery(pragma, null).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }
        val columnsByTable = tables.associateWith { table ->
            val destinationColumns = columns("PRAGMA table_info(`$table`)")
            val sourceColumns = columns("PRAGMA newdb.table_info(`$table`)")
            // ALTER TABLE appends migrated columns, while a freshly generated bundled
            // database may declare those same columns in the entity's natural order.
            // The copy below names every column explicitly, so physical order is irrelevant.
            check(destinationColumns.toSet() == sourceColumns.toSet()) {
                "Bundled $table schema mismatch. destination=$destinationColumns source=$sourceColumns"
            }
            destinationColumns
        }
        tables.asReversed().forEach { table ->
            db.execSQL("DELETE FROM $table")
        }
        tables.forEach { table ->
            val columnList = columnsByTable.getValue(table).joinToString(",") { "`$it`" }
            db.execSQL("INSERT INTO $table ($columnList) SELECT $columnList FROM newdb.$table")
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
    if (copyBundled) {
        val prefs = PrefsHelper(context)
        val bundledDatabaseVersion = BuildConfig.BUNDLED_DATABASE_NAME +
            if (BuildConfig.LIVE_DATA_UPDATES_ENABLED) ":live" else ":bundled-only"
        if (prefs.ingestedDatabaseName != bundledDatabaseVersion) {
            Timber.d("Updating from bundled db. '${prefs.ingestedDatabaseName}' (Last ingested version) -> '$bundledDatabaseVersion' (Bundled version)")
            // Create/upgrade the complete Room schema first. The host-generated
            // bundle contains only the read-only tables imported below.
            newRoomDatabase(context, name).also { it.openHelper.writableDatabase }
            copyDatabaseFromAssets(
                context,
                assetPath = "databases/${BuildConfig.BUNDLED_DATABASE_NAME}",
                destinationDbName = name
            )
            prefs.ingestedDatabaseName = bundledDatabaseVersion
        }
    }
    return newRoomDatabase(context, name)
}

private fun newRoomDatabase(context: Context, name: String): AppDatabase =
    androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6
        )
        .build()

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
            ALTER TABLE `${EventDefinition.TABLE_NAME}`
            ADD COLUMN `${Event.ART_PLAYA_ID}` TEXT
        """)
    }
}

val MIGRATION_2_3 = object : Migration(DATABASE_V2, DATABASE_V3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        fun tableColumns(table: String): Set<String> =
            database.query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(nameIndex))
                }
            }

        val legacyEventColumns = tableColumns(EventDefinition.TABLE_NAME)
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${Favorite.TABLE_NAME}` (
                `${Favorite.PLAYA_ID}` TEXT NOT NULL,
                `${Favorite.START_TIME}` TEXT NOT NULL,
                PRIMARY KEY(`${Favorite.PLAYA_ID}`, `${Favorite.START_TIME}`)
            )
            """.trimIndent()
        )
        if ("fav" in legacyEventColumns) {
            database.execSQL(
                "INSERT OR IGNORE INTO `${Favorite.TABLE_NAME}` " +
                    "(`${Favorite.PLAYA_ID}`, `${Favorite.START_TIME}`) " +
                    "SELECT `${PlayaItem.PLAYA_ID}`, `${Event.START_TIME}` " +
                    "FROM `${EventDefinition.TABLE_NAME}` WHERE `fav` != 0"
            )
        }
        val legacyArtId = if (Event.ART_PLAYA_ID in legacyEventColumns) {
            "`${Event.ART_PLAYA_ID}`"
        } else {
            "NULL"
        }

        fun dropFts(table: String, ftsTable: String) {
            listOf("BEFORE_UPDATE", "BEFORE_DELETE", "AFTER_UPDATE", "AFTER_INSERT")
                .forEach { suffix ->
                    database.execSQL(
                        "DROP TRIGGER IF EXISTS " +
                            "`room_fts_content_sync_${ftsTable}_$suffix`"
                    )
                }
            database.execSQL("DROP TABLE IF EXISTS `$ftsTable`")
        }

        fun createFts(table: String, ftsTable: String, fields: List<String>) {
            val definitions = fields.joinToString(", ") { "`$it` TEXT" }
            val columns = fields.joinToString(", ") { "`$it`" }
            val newColumns = fields.joinToString(", ") { "NEW.`$it`" }
            database.execSQL(
                "CREATE VIRTUAL TABLE IF NOT EXISTS `$ftsTable` " +
                    "USING FTS4($definitions, content=`$table`)"
            )
            database.execSQL(
                "INSERT INTO `$ftsTable`(`docid`, $columns) " +
                    "SELECT `${PlayaItem.ID}`, $columns FROM `$table`"
            )
            database.execSQL(
                "CREATE TRIGGER IF NOT EXISTS " +
                    "room_fts_content_sync_${ftsTable}_BEFORE_UPDATE " +
                    "BEFORE UPDATE ON `$table` BEGIN DELETE FROM `$ftsTable` " +
                    "WHERE `docid`=OLD.`rowid`; END"
            )
            database.execSQL(
                "CREATE TRIGGER IF NOT EXISTS " +
                    "room_fts_content_sync_${ftsTable}_BEFORE_DELETE " +
                    "BEFORE DELETE ON `$table` BEGIN DELETE FROM `$ftsTable` " +
                    "WHERE `docid`=OLD.`rowid`; END"
            )
            database.execSQL(
                "CREATE TRIGGER IF NOT EXISTS " +
                    "room_fts_content_sync_${ftsTable}_AFTER_UPDATE " +
                    "AFTER UPDATE ON `$table` BEGIN INSERT INTO `$ftsTable`" +
                    "(`docid`, $columns) VALUES (NEW.`rowid`, $newColumns); END"
            )
            database.execSQL(
                "CREATE TRIGGER IF NOT EXISTS " +
                    "room_fts_content_sync_${ftsTable}_AFTER_INSERT " +
                    "AFTER INSERT ON `$table` BEGIN INSERT INTO `$ftsTable`" +
                    "(`docid`, $columns) VALUES (NEW.`rowid`, $newColumns); END"
            )
        }

        if ("fav" in tableColumns(Art.TABLE_NAME)) {
            database.execSQL(
                "INSERT OR IGNORE INTO `${Favorite.TABLE_NAME}` " +
                    "(`${Favorite.PLAYA_ID}`, `${Favorite.START_TIME}`) " +
                    "SELECT `${PlayaItem.PLAYA_ID}`, '' FROM `${Art.TABLE_NAME}` " +
                    "WHERE `fav` != 0"
            )
            dropFts(Art.TABLE_NAME, ArtFts.TABLE_NAME)
            database.execSQL("ALTER TABLE `${Art.TABLE_NAME}` RENAME TO `arts_legacy`")
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `${Art.TABLE_NAME}` (
                    `${Art.ARTIST}` TEXT, `${Art.ARTIST_LOCATION}` TEXT,
                    `${Art.IMAGE_URL}` TEXT,
                    `${PlayaItem.ID}` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `${PlayaItem.NAME}` TEXT, `${PlayaItem.DESC}` TEXT,
                    `${PlayaItem.URL}` TEXT, `${PlayaItem.CONTACT}` TEXT,
                    `${PlayaItem.PLAYA_ADDR}` TEXT,
                    `${PlayaItem.PLAYA_ADDR_UNOFFICIAL}` TEXT,
                    `${PlayaItem.PLAYA_ID}` TEXT,
                    `${PlayaItem.LATITUDE}` REAL NOT NULL,
                    `${PlayaItem.LONGITUDE}` REAL NOT NULL,
                    `${PlayaItem.LATITUDE_UNOFFICIAL}` REAL NOT NULL,
                    `${PlayaItem.LONGITUDE_UNOFFICIAL}` REAL NOT NULL
                )
                """.trimIndent()
            )
            val artColumns = listOf(
                Art.ARTIST, Art.ARTIST_LOCATION, Art.IMAGE_URL,
                PlayaItem.ID, PlayaItem.NAME, PlayaItem.DESC, PlayaItem.URL,
                PlayaItem.CONTACT, PlayaItem.PLAYA_ADDR,
                PlayaItem.PLAYA_ADDR_UNOFFICIAL, PlayaItem.PLAYA_ID,
                PlayaItem.LATITUDE, PlayaItem.LONGITUDE,
                PlayaItem.LATITUDE_UNOFFICIAL, PlayaItem.LONGITUDE_UNOFFICIAL
            ).joinToString(", ") { "`$it`" }
            database.execSQL(
                "INSERT INTO `${Art.TABLE_NAME}` ($artColumns) " +
                    "SELECT $artColumns FROM `arts_legacy`"
            )
            database.execSQL("DROP TABLE `arts_legacy`")
            createFts(
                Art.TABLE_NAME,
                ArtFts.TABLE_NAME,
                listOf(PlayaItem.NAME, PlayaItem.DESC, Art.ARTIST, Art.ARTIST_LOCATION)
            )
        }

        if ("fav" in tableColumns(Camp.TABLE_NAME)) {
            database.execSQL(
                "INSERT OR IGNORE INTO `${Favorite.TABLE_NAME}` " +
                    "(`${Favorite.PLAYA_ID}`, `${Favorite.START_TIME}`) " +
                    "SELECT `${PlayaItem.PLAYA_ID}`, '' FROM `${Camp.TABLE_NAME}` " +
                    "WHERE `fav` != 0"
            )
            dropFts(Camp.TABLE_NAME, CampFts.TABLE_NAME)
            database.execSQL("ALTER TABLE `${Camp.TABLE_NAME}` RENAME TO `camps_legacy`")
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `${Camp.TABLE_NAME}` (
                    `${Camp.HOMETOWN}` TEXT,
                    `${PlayaItem.ID}` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `${PlayaItem.NAME}` TEXT, `${PlayaItem.DESC}` TEXT,
                    `${PlayaItem.URL}` TEXT, `${PlayaItem.CONTACT}` TEXT,
                    `${PlayaItem.PLAYA_ADDR}` TEXT,
                    `${PlayaItem.PLAYA_ADDR_UNOFFICIAL}` TEXT,
                    `${PlayaItem.PLAYA_ID}` TEXT,
                    `${PlayaItem.LATITUDE}` REAL NOT NULL,
                    `${PlayaItem.LONGITUDE}` REAL NOT NULL,
                    `${PlayaItem.LATITUDE_UNOFFICIAL}` REAL NOT NULL,
                    `${PlayaItem.LONGITUDE_UNOFFICIAL}` REAL NOT NULL
                )
                """.trimIndent()
            )
            val campColumns = listOf(
                Camp.HOMETOWN, PlayaItem.ID, PlayaItem.NAME, PlayaItem.DESC,
                PlayaItem.URL, PlayaItem.CONTACT, PlayaItem.PLAYA_ADDR,
                PlayaItem.PLAYA_ADDR_UNOFFICIAL, PlayaItem.PLAYA_ID,
                PlayaItem.LATITUDE, PlayaItem.LONGITUDE,
                PlayaItem.LATITUDE_UNOFFICIAL, PlayaItem.LONGITUDE_UNOFFICIAL
            ).joinToString(", ") { "`$it`" }
            database.execSQL(
                "INSERT INTO `${Camp.TABLE_NAME}` ($campColumns) " +
                    "SELECT $campColumns FROM `camps_legacy`"
            )
            database.execSQL("DROP TABLE `camps_legacy`")
            createFts(
                Camp.TABLE_NAME,
                CampFts.TABLE_NAME,
                listOf(PlayaItem.NAME, PlayaItem.DESC, Camp.HOMETOWN)
            )
        }

        if ("fav" in tableColumns(UserPoi.TABLE_NAME)) {
            database.execSQL(
                "INSERT OR IGNORE INTO `${Favorite.TABLE_NAME}` " +
                    "(`${Favorite.PLAYA_ID}`, `${Favorite.START_TIME}`) " +
                    "SELECT `${PlayaItem.PLAYA_ID}`, '' FROM `${UserPoi.TABLE_NAME}` " +
                    "WHERE `fav` != 0"
            )
            database.execSQL(
                "ALTER TABLE `${UserPoi.TABLE_NAME}` RENAME TO `user_pois_legacy`"
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `${UserPoi.TABLE_NAME}` (
                    `${UserPoi.ICON}` TEXT,
                    `${PlayaItem.ID}` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `${PlayaItem.NAME}` TEXT, `${PlayaItem.DESC}` TEXT,
                    `${PlayaItem.URL}` TEXT, `${PlayaItem.CONTACT}` TEXT,
                    `${PlayaItem.PLAYA_ADDR}` TEXT,
                    `${PlayaItem.PLAYA_ADDR_UNOFFICIAL}` TEXT,
                    `${PlayaItem.PLAYA_ID}` TEXT,
                    `${PlayaItem.LATITUDE}` REAL NOT NULL,
                    `${PlayaItem.LONGITUDE}` REAL NOT NULL,
                    `${PlayaItem.LATITUDE_UNOFFICIAL}` REAL NOT NULL,
                    `${PlayaItem.LONGITUDE_UNOFFICIAL}` REAL NOT NULL
                )
                """.trimIndent()
            )
            val poiColumns = listOf(
                UserPoi.ICON, PlayaItem.ID, PlayaItem.NAME, PlayaItem.DESC,
                PlayaItem.URL, PlayaItem.CONTACT, PlayaItem.PLAYA_ADDR,
                PlayaItem.PLAYA_ADDR_UNOFFICIAL, PlayaItem.PLAYA_ID,
                PlayaItem.LATITUDE, PlayaItem.LONGITUDE,
                PlayaItem.LATITUDE_UNOFFICIAL, PlayaItem.LONGITUDE_UNOFFICIAL
            ).joinToString(", ") { "`$it`" }
            database.execSQL(
                "INSERT INTO `${UserPoi.TABLE_NAME}` ($poiColumns) " +
                    "SELECT $poiColumns FROM `user_pois_legacy`"
            )
            database.execSQL("DROP TABLE `user_pois_legacy`")
        }

        listOf(
            "room_fts_content_sync_events_fts_BEFORE_UPDATE",
            "room_fts_content_sync_events_fts_BEFORE_DELETE",
            "room_fts_content_sync_events_fts_AFTER_UPDATE",
            "room_fts_content_sync_events_fts_AFTER_INSERT"
        ).forEach { trigger ->
            database.execSQL("DROP TRIGGER IF EXISTS `$trigger`")
        }
        database.execSQL("DROP TABLE IF EXISTS `${EventFts.TABLE_NAME}`")
        database.execSQL(
            "ALTER TABLE `${EventDefinition.TABLE_NAME}` RENAME TO `events_legacy`"
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${EventDefinition.TABLE_NAME}` (
                `${Event.TYPE}` TEXT,
                `${Event.ALL_DAY}` INTEGER NOT NULL,
                `${Event.CHECK_LOC}` INTEGER NOT NULL,
                `${Event.CAMP_PLAYA_ID}` TEXT,
                `${Event.ART_PLAYA_ID}` TEXT,
                `${PlayaItem.ID}` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `${PlayaItem.NAME}` TEXT,
                `${PlayaItem.DESC}` TEXT,
                `${PlayaItem.URL}` TEXT,
                `${PlayaItem.CONTACT}` TEXT,
                `${PlayaItem.PLAYA_ADDR}` TEXT,
                `${PlayaItem.PLAYA_ADDR_UNOFFICIAL}` TEXT,
                `${PlayaItem.PLAYA_ID}` TEXT,
                `${PlayaItem.LATITUDE}` REAL NOT NULL,
                `${PlayaItem.LONGITUDE}` REAL NOT NULL,
                `${PlayaItem.LATITUDE_UNOFFICIAL}` REAL NOT NULL,
                `${PlayaItem.LONGITUDE_UNOFFICIAL}` REAL NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_events_p_id` " +
                "ON `${EventDefinition.TABLE_NAME}` (`${PlayaItem.PLAYA_ID}`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_events_c_id` " +
                "ON `${EventDefinition.TABLE_NAME}` (`${Event.CAMP_PLAYA_ID}`)"
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${EventOccurrence.TABLE_NAME}` (
                `${PlayaItem.ID}` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `${Event.EVENT_ID}` INTEGER NOT NULL,
                `${PlayaItem.PLAYA_ID}` TEXT NOT NULL,
                `${Event.START_TIME}` TEXT NOT NULL,
                `${Event.START_TIME_PRETTY}` TEXT NOT NULL,
                `${Event.END_TIME}` TEXT NOT NULL,
                `${Event.END_TIME_PRETTY}` TEXT NOT NULL,
                FOREIGN KEY(`${Event.EVENT_ID}`)
                    REFERENCES `${EventDefinition.TABLE_NAME}`(`${PlayaItem.ID}`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        val legacyEventUid =
            "substr(`${PlayaItem.PLAYA_ID}`, 1, " +
                "length(rtrim(`${PlayaItem.PLAYA_ID}`, '0123456789')) - 1)"
        database.execSQL(
            """
            INSERT INTO `${EventDefinition.TABLE_NAME}` (
                `${PlayaItem.ID}`, `${PlayaItem.NAME}`, `${PlayaItem.DESC}`,
                `${PlayaItem.URL}`, `${PlayaItem.CONTACT}`, `${PlayaItem.PLAYA_ADDR}`,
                `${PlayaItem.PLAYA_ADDR_UNOFFICIAL}`, `${PlayaItem.PLAYA_ID}`,
                `${PlayaItem.LATITUDE}`, `${PlayaItem.LONGITUDE}`,
                `${PlayaItem.LATITUDE_UNOFFICIAL}`, `${PlayaItem.LONGITUDE_UNOFFICIAL}`,
                `${Event.TYPE}`, `${Event.ALL_DAY}`, `${Event.CHECK_LOC}`,
                `${Event.CAMP_PLAYA_ID}`, `${Event.ART_PLAYA_ID}`
            )
            SELECT
                MIN(`${PlayaItem.ID}`), `${PlayaItem.NAME}`, `${PlayaItem.DESC}`,
                `${PlayaItem.URL}`, `${PlayaItem.CONTACT}`, `${PlayaItem.PLAYA_ADDR}`,
                `${PlayaItem.PLAYA_ADDR_UNOFFICIAL}`, $legacyEventUid,
                `${PlayaItem.LATITUDE}`, `${PlayaItem.LONGITUDE}`,
                `${PlayaItem.LATITUDE_UNOFFICIAL}`, `${PlayaItem.LONGITUDE_UNOFFICIAL}`,
                `${Event.TYPE}`, `${Event.ALL_DAY}`, `${Event.CHECK_LOC}`,
                `${Event.CAMP_PLAYA_ID}`, $legacyArtId
            FROM `events_legacy`
            GROUP BY $legacyEventUid
            """.trimIndent()
        )
        database.execSQL(
            """
            INSERT INTO `${EventOccurrence.TABLE_NAME}` (
                `${PlayaItem.ID}`, `${Event.EVENT_ID}`, `${PlayaItem.PLAYA_ID}`,
                `${Event.START_TIME}`, `${Event.START_TIME_PRETTY}`,
                `${Event.END_TIME}`, `${Event.END_TIME_PRETTY}`
            )
            SELECT legacy.`${PlayaItem.ID}`, definition.`${PlayaItem.ID}`,
                legacy.`${PlayaItem.PLAYA_ID}`, legacy.`${Event.START_TIME}`,
                legacy.`${Event.START_TIME_PRETTY}`, legacy.`${Event.END_TIME}`,
                legacy.`${Event.END_TIME_PRETTY}`
            FROM `events_legacy` legacy
            JOIN `${EventDefinition.TABLE_NAME}` definition
              ON definition.`${PlayaItem.PLAYA_ID}` =
                 substr(legacy.`${PlayaItem.PLAYA_ID}`, 1,
                    length(rtrim(legacy.`${PlayaItem.PLAYA_ID}`, '0123456789')) - 1)
            """.trimIndent()
        )
        database.execSQL("DROP TABLE `events_legacy`")

        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "`index_event_occurrences_event_id_s_time` ON " +
                "`${EventOccurrence.TABLE_NAME}` " +
                "(`${Event.EVENT_ID}`, `${Event.START_TIME}`)"
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_event_occurrences_p_id` ON " +
                "`${EventOccurrence.TABLE_NAME}` (`${PlayaItem.PLAYA_ID}`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_event_occurrences_s_time` ON " +
                "`${EventOccurrence.TABLE_NAME}` (`${Event.START_TIME}`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_event_occurrences_e_time` ON " +
                "`${EventOccurrence.TABLE_NAME}` (`${Event.END_TIME}`)"
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_arts_p_id` " +
                "ON `${Art.TABLE_NAME}` (`${PlayaItem.PLAYA_ID}`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_arts_name` " +
                "ON `${Art.TABLE_NAME}` (`${PlayaItem.NAME}`)"
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_camps_p_id` " +
                "ON `${Camp.TABLE_NAME}` (`${PlayaItem.PLAYA_ID}`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_camps_name` " +
                "ON `${Camp.TABLE_NAME}` (`${PlayaItem.NAME}`)"
        )
        database.execSQL(
            """
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
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_map_pins_uid` " +
                "ON `${MapPin.TABLE_NAME}` (`${MapPin.UID}`)"
        )

        database.execSQL(
            "CREATE VIRTUAL TABLE IF NOT EXISTS `${EventFts.TABLE_NAME}` " +
                "USING FTS4(`${PlayaItem.NAME}` TEXT, `${PlayaItem.DESC}` TEXT, " +
                "content=`${EventDefinition.TABLE_NAME}`)"
        )
        database.execSQL(
            "INSERT INTO `${EventFts.TABLE_NAME}`(`docid`, `${PlayaItem.NAME}`, " +
                "`${PlayaItem.DESC}`) SELECT `${PlayaItem.ID}`, `${PlayaItem.NAME}`, " +
                "`${PlayaItem.DESC}` FROM `${EventDefinition.TABLE_NAME}`"
        )
        database.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_events_fts_BEFORE_UPDATE " +
                "BEFORE UPDATE ON `${EventDefinition.TABLE_NAME}` BEGIN DELETE FROM " +
                "`${EventFts.TABLE_NAME}` WHERE `docid`=OLD.`rowid`; END"
        )
        database.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_events_fts_BEFORE_DELETE " +
                "BEFORE DELETE ON `${EventDefinition.TABLE_NAME}` BEGIN DELETE FROM " +
                "`${EventFts.TABLE_NAME}` WHERE `docid`=OLD.`rowid`; END"
        )
        database.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_events_fts_AFTER_UPDATE " +
                "AFTER UPDATE ON `${EventDefinition.TABLE_NAME}` BEGIN INSERT INTO " +
                "`${EventFts.TABLE_NAME}`(`docid`, `${PlayaItem.NAME}`, `${PlayaItem.DESC}`) " +
                "VALUES (NEW.`rowid`, NEW.`${PlayaItem.NAME}`, NEW.`${PlayaItem.DESC}`); END"
        )
        database.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_events_fts_AFTER_INSERT " +
                "AFTER INSERT ON `${EventDefinition.TABLE_NAME}` BEGIN INSERT INTO " +
                "`${EventFts.TABLE_NAME}`(`docid`, `${PlayaItem.NAME}`, `${PlayaItem.DESC}`) " +
                "VALUES (NEW.`rowid`, NEW.`${PlayaItem.NAME}`, NEW.`${PlayaItem.DESC}`); END"
        )
        database.execSQL(
            "CREATE VIEW `${Event.VIEW_NAME}` AS $EVENT_VIEW_QUERY_V3"
        )
    }
}

val MIGRATION_3_4 = object : Migration(DATABASE_V3, DATABASE_V4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP VIEW IF EXISTS `${Event.VIEW_NAME}`")
        database.execSQL(
            "ALTER TABLE `${EventOccurrence.TABLE_NAME}` " +
                "RENAME TO `event_occurrences_legacy`"
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${EventOccurrence.TABLE_NAME}` (
                `${PlayaItem.ID}` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `${Event.EVENT_ID}` INTEGER NOT NULL,
                `${PlayaItem.PLAYA_ID}` TEXT NOT NULL,
                `${Event.START_TIME}` INTEGER NOT NULL,
                `${Event.END_TIME}` INTEGER NOT NULL,
                FOREIGN KEY(`${Event.EVENT_ID}`)
                    REFERENCES `${EventDefinition.TABLE_NAME}`(`${PlayaItem.ID}`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        fun epochMillis(column: String): String =
            "CAST(strftime('%s', substr(`$column`, 1, 22) || ':' || " +
                "substr(`$column`, 23, 2)) AS INTEGER) * 1000"
        database.execSQL(
            """
            INSERT INTO `${EventOccurrence.TABLE_NAME}` (
                `${PlayaItem.ID}`, `${Event.EVENT_ID}`, `${PlayaItem.PLAYA_ID}`,
                `${Event.START_TIME}`, `${Event.END_TIME}`
            )
            SELECT `${PlayaItem.ID}`, `${Event.EVENT_ID}`, `${PlayaItem.PLAYA_ID}`,
                ${epochMillis(Event.START_TIME)}, ${epochMillis(Event.END_TIME)}
            FROM `event_occurrences_legacy`
            """.trimIndent()
        )
        database.execSQL("DROP TABLE `event_occurrences_legacy`")
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "`index_event_occurrences_event_id_s_time` ON " +
                "`${EventOccurrence.TABLE_NAME}` " +
                "(`${Event.EVENT_ID}`, `${Event.START_TIME}`)"
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_event_occurrences_p_id` ON " +
                "`${EventOccurrence.TABLE_NAME}` (`${PlayaItem.PLAYA_ID}`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_event_occurrences_s_time` ON " +
                "`${EventOccurrence.TABLE_NAME}` (`${Event.START_TIME}`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_event_occurrences_e_time` ON " +
                "`${EventOccurrence.TABLE_NAME}` (`${Event.END_TIME}`)"
        )

        database.execSQL(
            "ALTER TABLE `${Favorite.TABLE_NAME}` RENAME TO `favorites_legacy`"
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${Favorite.TABLE_NAME}` (
                `${Favorite.PLAYA_ID}` TEXT NOT NULL,
                `${Favorite.START_TIME}` INTEGER NOT NULL,
                PRIMARY KEY(`${Favorite.PLAYA_ID}`, `${Favorite.START_TIME}`)
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            INSERT OR IGNORE INTO `${Favorite.TABLE_NAME}` (
                `${Favorite.PLAYA_ID}`, `${Favorite.START_TIME}`
            )
            SELECT
                CASE WHEN definition.`${PlayaItem.PLAYA_ID}` IS NOT NULL
                    THEN definition.`${PlayaItem.PLAYA_ID}`
                    ELSE favorite.`${Favorite.PLAYA_ID}` END,
                CASE WHEN occurrence.`${PlayaItem.ID}` IS NOT NULL
                    THEN occurrence.`${Event.START_TIME}`
                    ELSE 0 END
            FROM `favorites_legacy` favorite
            LEFT JOIN `${EventOccurrence.TABLE_NAME}` occurrence
              ON occurrence.`${PlayaItem.PLAYA_ID}` =
                 favorite.`${Favorite.PLAYA_ID}`
            LEFT JOIN `${EventDefinition.TABLE_NAME}` definition
              ON definition.`${PlayaItem.ID}` = occurrence.`${Event.EVENT_ID}`
            """.trimIndent()
        )
        database.execSQL("DROP TABLE `favorites_legacy`")
        database.execSQL(
            "CREATE VIEW `${Event.VIEW_NAME}` AS ${Event.VIEW_QUERY}"
        )
    }
}

val MIGRATION_4_5 = object : Migration(DATABASE_V4, DATABASE_V5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${MutantVehicle.TABLE_NAME}` (
                `${MutantVehicle.ARTIST}` TEXT,
                `${MutantVehicle.HOMETOWN}` TEXT,
                `${MutantVehicle.IMAGE_URL}` TEXT,
                `${MutantVehicle.TAGS}` TEXT,
                `${PlayaItem.ID}` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `${PlayaItem.NAME}` TEXT, `${PlayaItem.DESC}` TEXT,
                `${PlayaItem.URL}` TEXT, `${PlayaItem.CONTACT}` TEXT,
                `${PlayaItem.PLAYA_ADDR}` TEXT, `${PlayaItem.PLAYA_ADDR_UNOFFICIAL}` TEXT,
                `${PlayaItem.PLAYA_ID}` TEXT,
                `${PlayaItem.LATITUDE}` REAL NOT NULL,
                `${PlayaItem.LONGITUDE}` REAL NOT NULL,
                `${PlayaItem.LATITUDE_UNOFFICIAL}` REAL NOT NULL,
                `${PlayaItem.LONGITUDE_UNOFFICIAL}` REAL NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_mutant_vehicles_p_id` " +
                "ON `${MutantVehicle.TABLE_NAME}` (`${PlayaItem.PLAYA_ID}`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_mutant_vehicles_name` " +
                "ON `${MutantVehicle.TABLE_NAME}` (`${PlayaItem.NAME}`)"
        )

        val ftsColumns = listOf(
            PlayaItem.NAME,
            PlayaItem.DESC,
            MutantVehicle.ARTIST,
            MutantVehicle.HOMETOWN,
            MutantVehicle.TAGS
        )
        val definitions = ftsColumns.joinToString(", ") { "`$it` TEXT" }
        val columns = ftsColumns.joinToString(", ") { "`$it`" }
        val newColumns = ftsColumns.joinToString(", ") { "NEW.`$it`" }
        database.execSQL(
            "CREATE VIRTUAL TABLE IF NOT EXISTS `${MutantVehicleFts.TABLE_NAME}` " +
                "USING FTS4($definitions, content=`${MutantVehicle.TABLE_NAME}`)"
        )
        database.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_mutant_vehicles_fts_BEFORE_UPDATE " +
                "BEFORE UPDATE ON `${MutantVehicle.TABLE_NAME}` BEGIN DELETE FROM " +
                "`${MutantVehicleFts.TABLE_NAME}` WHERE `docid`=OLD.`rowid`; END"
        )
        database.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_mutant_vehicles_fts_BEFORE_DELETE " +
                "BEFORE DELETE ON `${MutantVehicle.TABLE_NAME}` BEGIN DELETE FROM " +
                "`${MutantVehicleFts.TABLE_NAME}` WHERE `docid`=OLD.`rowid`; END"
        )
        database.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_mutant_vehicles_fts_AFTER_UPDATE " +
                "AFTER UPDATE ON `${MutantVehicle.TABLE_NAME}` BEGIN INSERT INTO " +
                "`${MutantVehicleFts.TABLE_NAME}`(`docid`, $columns) VALUES " +
                "(NEW.`rowid`, $newColumns); END"
        )
        database.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_mutant_vehicles_fts_AFTER_INSERT " +
                "AFTER INSERT ON `${MutantVehicle.TABLE_NAME}` BEGIN INSERT INTO " +
                "`${MutantVehicleFts.TABLE_NAME}`(`docid`, $columns) VALUES " +
                "(NEW.`rowid`, $newColumns); END"
        )
    }
}

val MIGRATION_5_6 = object : Migration(DATABASE_V5, DATABASE_V6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE `${Art.TABLE_NAME}` ADD COLUMN `${Art.HOMETOWN}` TEXT"
        )
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
