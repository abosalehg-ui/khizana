package com.abosalehg.khizana

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abosalehg.khizana.data.db.BookEntity
import com.abosalehg.khizana.data.db.KhizanaDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the rescan contract at the DAO level: hidden state and reading
 * progress must survive location updates and MISSING round-trips.
 */
@RunWith(AndroidJUnit4::class)
class BookDaoRescanTest {

    private lateinit var db: KhizanaDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KhizanaDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun hiddenStateAndProgressSurviveRelocation() = runBlocking {
        val dao = db.bookDao()
        dao.insert(
            BookEntity(
                id = "fp1",
                path = "/old/place/book.pdf",
                fileName = "book.pdf",
                format = "PDF",
                title = "book",
                locator = "42",
                progress = 0.5f,
                isHidden = true,
                fileSize = 1000,
                addedAt = 1
            )
        )

        // Simulates a rescan that found the same fingerprint elsewhere.
        dao.updateLocation("fp1", "/new/place/renamed.pdf", "renamed.pdf", 1000, "OK")

        val book = dao.getById("fp1")!!
        assertEquals("/new/place/renamed.pdf", book.path)
        assertTrue(book.isHidden)
        assertEquals("42", book.locator)
        assertEquals(0.5f, book.progress)
    }

    @Test
    fun missingRoundTripKeepsAllData() = runBlocking {
        val dao = db.bookDao()
        dao.insert(
            BookEntity(
                id = "fp2",
                path = "/p/b.cbz",
                fileName = "b.cbz",
                format = "CBZ",
                title = "b",
                locator = "7",
                progress = 0.25f,
                fileSize = 10,
                addedAt = 1
            )
        )

        dao.markMissing(listOf("fp2"))
        assertEquals("MISSING", dao.getById("fp2")!!.status)

        // The file came back on a later rescan.
        dao.updateLocation("fp2", "/p/b.cbz", "b.cbz", 10, "OK")

        val book = dao.getById("fp2")!!
        assertEquals("OK", book.status)
        assertEquals("7", book.locator)
        assertEquals(0.25f, book.progress)
    }

    @Test
    fun missingBooksAreInvisibleButNeverDeleted() = runBlocking {
        val dao = db.bookDao()
        dao.insert(
            BookEntity(
                id = "fp3",
                path = "/p/c.pdf",
                fileName = "c.pdf",
                format = "PDF",
                title = "c",
                fileSize = 10,
                addedAt = 1
            )
        )

        dao.markMissing(listOf("fp3"))

        assertEquals(1, dao.getAll().size)
    }
}
