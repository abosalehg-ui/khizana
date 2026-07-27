package com.abosalehg.khizana

import com.abosalehg.khizana.data.covers.CoverStore
import com.abosalehg.khizana.data.repo.LibraryRepository
import com.abosalehg.khizana.data.scanner.ScannedFile
import com.abosalehg.khizana.domain.model.BookFormat
import com.abosalehg.khizana.fakes.FakeBookDao
import com.abosalehg.khizana.fakes.FakeBookmarkDao
import com.abosalehg.khizana.fakes.FakeExcludedFolderDao
import com.abosalehg.khizana.fakes.FakeLibraryScanner
import com.abosalehg.khizana.fakes.FakeTagDao
import com.abosalehg.khizana.fakes.FakeTopicDao
import com.abosalehg.khizana.fakes.PassThroughTransactionRunner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The rescan contract is the promise the whole app rests on: a file that moves
 * or is renamed keeps its row and its reading data, a file that disappears is
 * marked MISSING rather than deleted, and it comes back to life when found
 * again. None of it had a test before.
 */
class LibraryRescanTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var bookDao: FakeBookDao
    private lateinit var scanner: FakeLibraryScanner
    private lateinit var repository: LibraryRepository

    @Before
    fun setUp() {
        bookDao = FakeBookDao()
        scanner = FakeLibraryScanner()
        repository = LibraryRepository(
            bookDao = bookDao,
            topicDao = FakeTopicDao(),
            tagDao = FakeTagDao(),
            bookmarkDao = FakeBookmarkDao(),
            excludedFolderDao = FakeExcludedFolderDao(),
            scanner = scanner,
            coverStore = CoverStore(temp.newFolder("covers")),
            transaction = PassThroughTransactionRunner
        )
    }

    private fun pdf(name: String, content: String, dir: File = temp.root): File =
        File(dir, name).apply { parentFile?.mkdirs(); writeText(content) }

    private fun found(vararg files: File) {
        scanner.files = files.map { ScannedFile(it, BookFormat.PDF) }
    }

    @Test
    fun newFileLandsOnTheNewShelfWithItsFileNameAsTitle() = runBlocking {
        found(pdf("Tarikh al-Tabari.pdf", "content-a"))

        val report = repository.rescan()

        assertEquals(1, report.scanned)
        assertEquals(1, report.added)
        val book = bookDao.snapshot().single()
        assertEquals("Tarikh al-Tabari", book.title)
        assertNull("new books belong to the New shelf", book.topicId)
        assertEquals("OK", book.status)
    }

    @Test
    fun movingAndRenamingAFileUpdatesTheRowInsteadOfAddingOne() = runBlocking {
        val original = pdf("volume.pdf", "same-bytes")
        found(original)
        repository.rescan()

        val id = bookDao.snapshot().single().id
        // Simulate the user reading half of it and filing it on a shelf.
        bookDao.saveReadingPosition(id, "42", 0.5f, 1_000L)
        bookDao.updateTopic(id, 7L)
        bookDao.setHidden(id, true)

        original.delete()
        val moved = pdf("renamed.pdf", "same-bytes", temp.newFolder("elsewhere"))
        found(moved)
        val report = repository.rescan()

        assertEquals("no second row for the same content", 1, bookDao.snapshot().size)
        assertEquals(0, report.added)
        assertEquals(1, report.relocated)
        val book = bookDao.snapshot().single()
        assertEquals(moved.absolutePath, book.path)
        assertEquals("renamed.pdf", book.fileName)
        assertEquals("42", book.locator)
        assertEquals(0.5f, book.progress, 0f)
        assertEquals(7L, book.topicId)
        assertTrue(book.isHidden)
    }

    @Test
    fun vanishedFilesAreMarkedMissingAndNeverDeleted() = runBlocking {
        val file = pdf("gone.pdf", "bytes-b")
        found(file)
        repository.rescan()
        val id = bookDao.snapshot().single().id
        bookDao.saveReadingPosition(id, "9", 0.3f, 5L)

        file.delete()
        found()
        val report = repository.rescan()

        assertEquals(1, report.missing)
        val book = bookDao.snapshot().single()
        assertEquals("MISSING", book.status)
        assertEquals("9", book.locator)
    }

    @Test
    fun aMissingFileThatComesBackIsRestoredToOk() = runBlocking {
        val file = pdf("returning.pdf", "bytes-c")
        found(file)
        repository.rescan()
        file.delete()
        found()
        repository.rescan()
        assertEquals("MISSING", bookDao.snapshot().single().status)

        val restored = pdf("returning.pdf", "bytes-c")
        found(restored)
        val report = repository.rescan()

        assertEquals(0, report.added)
        assertEquals(1, report.relocated)
        assertEquals("OK", bookDao.snapshot().single().status)
    }

    @Test
    fun twoCopiesOfTheSameContentProduceOneRow() = runBlocking {
        found(
            pdf("copy-one.pdf", "identical"),
            pdf("copy-two.pdf", "identical", temp.newFolder("other"))
        )

        val report = repository.rescan()

        assertEquals(2, report.scanned)
        assertEquals(1, report.added)
        assertEquals(1, bookDao.snapshot().size)
    }

    @Test
    fun anUnreadableFileIsSkippedWithoutFailingTheWholeScan() = runBlocking {
        val good = pdf("good.pdf", "readable")
        val missing = File(temp.root, "never-existed.pdf")
        found(missing, good)

        val report = repository.rescan()

        assertEquals(1, report.added)
        assertEquals("good", bookDao.snapshot().single().title)
    }

    @Test
    fun progressIsReportedAndAlwaysEndsAtTheTotal() = runBlocking {
        found(*(1..120).map { pdf("book-$it.pdf", "content-$it") }.toTypedArray())
        val updates = mutableListOf<Pair<Int, Int>>()

        repository.rescan { processed, total -> updates += processed to total }

        // Throttled, not one write per file, but the final tick must be exact.
        assertTrue("progress should be throttled", updates.size < 120)
        assertEquals(120 to 120, updates.last())
    }
}
