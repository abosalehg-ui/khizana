package com.abosalehg.khizana

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abosalehg.khizana.data.db.BookEntity
import com.abosalehg.khizana.data.db.KhizanaDatabase
import com.abosalehg.khizana.domain.model.BookStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Real Room SQL, run on the JVM through Robolectric. This used to be an
 * instrumented test, which meant CI never executed it — the workflow has no
 * emulator step.
 *
 * Beyond the rescan contract it also pins the status string literals baked
 * into `BookDao`'s queries to the `BookStatus` enum: rename a constant without
 * touching the SQL and these fail instead of the app silently misbehaving.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class KhizanaDatabaseTest {

    private lateinit var db: KhizanaDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KhizanaDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun book(
        id: String,
        topicId: Long? = null,
        hidden: Boolean = false,
        status: BookStatus = BookStatus.OK
    ) = BookEntity(
        id = id,
        path = "/p/$id.pdf",
        fileName = "$id.pdf",
        format = "PDF",
        title = id,
        topicId = topicId,
        isHidden = hidden,
        status = status.name,
        fileSize = 10,
        addedAt = 1
    )

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

        dao.updateLocation("fp1", "/new/place/renamed.pdf", "renamed.pdf", 1000, "OK")

        val book = dao.getById("fp1")!!
        assertEquals("/new/place/renamed.pdf", book.path)
        assertTrue(book.isHidden)
        assertEquals("42", book.locator)
        assertEquals(0.5f, book.progress, 0f)
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
        assertEquals(BookStatus.MISSING.name, dao.getById("fp2")!!.status)

        dao.updateLocation("fp2", "/p/b.cbz", "b.cbz", 10, BookStatus.OK.name)

        val book = dao.getById("fp2")!!
        assertEquals(BookStatus.OK.name, book.status)
        assertEquals("7", book.locator)
        assertEquals(0.25f, book.progress, 0f)
    }

    @Test
    fun missingBooksAreInvisibleButNeverDeleted() = runBlocking {
        val dao = db.bookDao()
        dao.insert(book("fp3"))
        dao.markMissing(listOf("fp3"))

        assertEquals("the row must survive", 1, dao.getAll().size)
        // Also pins the 'MISSING' literal inside observeVisible() to the enum.
        assertTrue(dao.observeVisible().first().isEmpty())
    }

    @Test
    fun observeVisibleHidesHiddenBooksAndShowsTheRest() = runBlocking {
        val dao = db.bookDao()
        dao.insert(book("visible"))
        dao.insert(book("hidden", hidden = true))

        assertEquals(listOf("visible"), dao.observeVisible().first().map { it.id })
        assertEquals(listOf("hidden"), dao.observeHidden().first().map { it.id })
    }

    @Test
    fun booksOnShelfReadsOnlyThatShelfIncludingTheNullNewShelf() = runBlocking {
        val dao = db.bookDao()
        dao.insert(book("newShelf", topicId = null))
        dao.insert(book("history", topicId = 5L))
        dao.insert(book("hiddenHistory", topicId = 5L, hidden = true))
        dao.insert(book("goneHistory", topicId = 5L, status = BookStatus.MISSING))

        assertEquals(listOf("newShelf"), dao.booksOnShelf(null).map { it.id })
        assertEquals(listOf("history"), dao.booksOnShelf(5L).map { it.id })
    }

    @Test
    fun getNeedingCoversSkipsFailedAndNonOkBooks() = runBlocking {
        val dao = db.bookDao()
        dao.insert(book("wants"))
        dao.insert(book("damaged", status = BookStatus.CORRUPT))
        dao.insert(book("triedAndFailed"))
        dao.setCoverFailed("triedAndFailed")

        assertEquals(listOf("wants"), dao.getNeedingCovers().map { it.id })
    }

    @Test
    fun deletingAShelfReturnsItsBooksToTheNewShelf() = runBlocking {
        val dao = db.bookDao()
        dao.insert(book("a", topicId = 3L))
        dao.insert(book("b", topicId = 4L))

        dao.clearTopic(3L)

        assertEquals(null, dao.getById("a")!!.topicId)
        assertEquals(4L, dao.getById("b")!!.topicId)
    }
}
