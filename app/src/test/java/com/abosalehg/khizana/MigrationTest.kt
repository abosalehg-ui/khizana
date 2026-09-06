package com.abosalehg.khizana

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abosalehg.khizana.data.db.KhizanaDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Losing a reader's place in a book is the worst thing this app can do, and a
 * migration is where it would happen. There is no `fallbackToDestructiveMigration`
 * to catch a mistake, so every version step is exercised here against the
 * schema JSON Room itself generated.
 *
 * The v1 schema was reconstructed by re-running the annotation processor over
 * the v1 entity shape — `exportSchema` was off when v1 shipped — so its hash
 * and its DDL are Room's own, not hand-written.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KhizanaDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migratesFromV1ToLatestKeepingEveryReadingPosition() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO books (
                    id, path, fileName, format, title, author, topicId, pageCount,
                    locator, progress, readingDirection, coverPath, coverFailed,
                    status, isHidden, manualOrder, fileSize, addedAt, lastReadAt
                ) VALUES (
                    '$BOOK_ID', '/storage/emulated/0/Books/tarikh.pdf', 'tarikh.pdf',
                    'PDF', 'تاريخ الطبري', NULL, NULL, 640,
                    '317', 0.5, 'RTL', NULL, 0,
                    'OK', 1, 7, 1024, 99, 12345
                )
                """.trimIndent()
            )
            db.execSQL(
                "INSERT INTO bookmarks (bookId, page, note, createdAt) " +
                    "VALUES ('$BOOK_ID', 317, 'هنا وقفت', 999)"
            )
        }

        // Both steps at once: this is the upgrade path a reader who installed
        // the very first build actually takes.
        helper.runMigrationsAndValidate(
            TEST_DB,
            LATEST_VERSION,
            true,
            KhizanaDatabase.MIGRATION_1_2,
            KhizanaDatabase.MIGRATION_2_3
        ).close()

        withMigrated { db ->
            val book = runBlocking { db.bookDao().getById(BOOK_ID) }!!
            assertEquals("317", book.locator)
            assertEquals(0.5f, book.progress, 0f)
            assertEquals("تاريخ الطبري", book.title)
            assertEquals(7, book.manualOrder)
            assertTrue(book.isHidden)
            // v3's new column exists and defaults to "unknown", which is what
            // makes the rescan fast path re-fingerprint this row exactly once.
            assertEquals(0L, book.lastModified)

            val bookmarks = runBlocking { db.bookmarkDao().getAll() }
            assertEquals(1, bookmarks.size)
            assertEquals("هنا وقفت", bookmarks.single().note)
        }
    }

    @Test
    fun migrationFromV2AddsTheColumnWithoutTouchingAnythingElse() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO books (
                    id, path, fileName, format, title, author, topicId, pageCount,
                    locator, progress, readingDirection, coverPath, coverFailed,
                    status, isHidden, manualOrder, fileSize, addedAt, lastReadAt
                ) VALUES (
                    '$BOOK_ID', '/storage/emulated/0/Books/a.cbz', 'a.cbz',
                    'CBZ', 'المجلد 2', NULL, NULL, 12,
                    '3', 0.25, 'AUTO', NULL, 0,
                    'OK', 0, 0, 2048, 5, NULL
                )
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            LATEST_VERSION,
            true,
            KhizanaDatabase.MIGRATION_2_3
        ).close()

        withMigrated { db ->
            val book = runBlocking { db.bookDao().getById(BOOK_ID) }!!
            assertEquals("3", book.locator)
            assertEquals(0.25f, book.progress, 0f)
            assertEquals(0L, book.lastModified)
        }
    }

    /**
     * Reopens the migrated file through Room proper, so the DAOs and Room's own
     * schema validation both run against what the migrations produced.
     *
     * `RoomDatabase` is not `Closeable`, hence the explicit finally rather than
     * `use`.
     */
    private fun withMigrated(block: (KhizanaDatabase) -> Unit) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db = Room.databaseBuilder(context, KhizanaDatabase::class.java, TEST_DB)
            .addMigrations(KhizanaDatabase.MIGRATION_1_2, KhizanaDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
            block(db)
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
        const val LATEST_VERSION = 3
        val BOOK_ID = "a".repeat(64)
    }
}
