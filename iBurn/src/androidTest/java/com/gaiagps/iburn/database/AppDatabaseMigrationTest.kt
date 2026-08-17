package com.gaiagps.iburn.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate2To3NormalizesOccurrencesAndPreservesFavorites() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            execSQL(
                """
                INSERT INTO events (
                    _id, name, `desc`, p_id, lat, lon, lat_unof, lon_unof, fav,
                    e_type, all_day, check_loc, c_id, s_time, s_time_p, e_time, e_time_p
                ) VALUES
                    (10, 'Repeated Event', 'Description', 'eventuid-0',
                     40.7, -119.2, 0, 0, 0, 'work', 0, 0, 'campuid',
                     '2026-08-31T10:00:00-0700', 'Mon 8/31 10:00 AM',
                     '2026-08-31T11:00:00-0700', 'Mon 8/31 11:00 AM'),
                    (11, 'Repeated Event', 'Description', 'eventuid-1',
                     40.7, -119.2, 0, 0, 1, 'work', 0, 0, 'campuid',
                     '2026-09-01T10:00:00-0700', 'Tue 9/1 10:00 AM',
                     '2026-09-01T11:00:00-0700', 'Tue 9/1 11:00 AM')
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            3,
            true,
            MIGRATION_2_3
        ).use { database ->
            database.query("SELECT COUNT(*) FROM events").use {
                it.moveToFirst()
                assertEquals(1, it.getInt(0))
            }
            database.query("SELECT COUNT(*) FROM event_occurrences").use {
                it.moveToFirst()
                assertEquals(2, it.getInt(0))
            }
            database.query(
                "SELECT COUNT(*) FROM event_occurrence_rows WHERE event_uid = 'eventuid'"
            ).use {
                it.moveToFirst()
                assertEquals(2, it.getInt(0))
            }
            database.query(
                "SELECT COUNT(*) FROM events_fts WHERE events_fts MATCH 'Repeated'"
            ).use {
                it.moveToFirst()
                assertEquals(1, it.getInt(0))
            }
            database.query("SELECT COUNT(*) FROM favorites").use {
                it.moveToFirst()
                assertEquals(1, it.getInt(0))
            }
        }
    }

    @Test
    fun migrate3To4StoresEpochsAndUsesStableEventFavoriteKeys() {
        helper.createDatabase("app-database-migration-v3", 3).apply {
            execSQL(
                """
                INSERT INTO events (
                    _id, name, `desc`, p_id, lat, lon, lat_unof, lon_unof,
                    e_type, all_day, check_loc
                ) VALUES (
                    10, 'Epoch Event', 'Description', 'eventuid',
                    40.7, -119.2, 0, 0, 'work', 0, 0
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO event_occurrences (
                    _id, event_id, p_id, s_time, s_time_p, e_time, e_time_p
                ) VALUES (
                    20, 10, 'eventuid-0',
                    '2026-08-31T10:00:00-0700', 'Mon 8/31 10:00 AM',
                    '2026-08-31T11:00:00-0700', 'Mon 8/31 11:00 AM'
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO favorites (p_id, s_time)
                VALUES ('eventuid-0', '2026-08-31T10:00:00-0700')
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            "app-database-migration-v3",
            4,
            true,
            MIGRATION_3_4
        ).use { database ->
            database.query(
                "SELECT s_time, e_time FROM event_occurrences WHERE _id = 20"
            ).use {
                it.moveToFirst()
                assertEquals(1_788_195_600_000L, it.getLong(0))
                assertEquals(1_788_199_200_000L, it.getLong(1))
            }
            database.query("SELECT p_id, s_time FROM favorites").use {
                it.moveToFirst()
                assertEquals("eventuid", it.getString(0))
                assertEquals(1_788_195_600_000L, it.getLong(1))
            }
            database.query(
                "SELECT event_uid, s_time FROM event_occurrence_rows WHERE _id = 20"
            ).use {
                it.moveToFirst()
                assertEquals("eventuid", it.getString(0))
                assertEquals(1_788_195_600_000L, it.getLong(1))
            }
        }
    }

    @Test
    fun migrate4To5AddsSearchableMutantVehicles() {
        helper.createDatabase("app-database-migration-v4", 4).close()

        helper.runMigrationsAndValidate(
            "app-database-migration-v4",
            5,
            true,
            MIGRATION_4_5
        ).use { database ->
            database.execSQL(
                """
                INSERT INTO mutant_vehicles (
                    name, `desc`, p_id, artist, hometown, tags,
                    lat, lon, lat_unof, lon_unof
                ) VALUES (
                    'Test Vehicle', 'A rolling test', 'mv-1', 'Test Artist',
                    'Reno, NV', 'Rolling, Lights', 0, 0, 0, 0
                )
                """.trimIndent()
            )
            database.query(
                "SELECT COUNT(*) FROM mutant_vehicles_fts " +
                    "WHERE mutant_vehicles_fts MATCH 'Lights'"
            ).use {
                it.moveToFirst()
                assertEquals(1, it.getInt(0))
            }
        }
    }

    companion object {
        private const val TEST_DATABASE = "app-database-migration"
    }
}
