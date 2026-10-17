package com.abosalehg.khizana

import com.abosalehg.khizana.data.covers.CoverStore
import com.abosalehg.khizana.data.db.BookEntity
import com.abosalehg.khizana.data.repo.LibraryRepository
import com.abosalehg.khizana.domain.model.MAX_NAME_LENGTH
import com.abosalehg.khizana.domain.model.normalizeAuthor
import com.abosalehg.khizana.domain.model.normalizeTitle
import com.abosalehg.khizana.fakes.FakeBookDao
import com.abosalehg.khizana.fakes.FakeBookmarkDao
import com.abosalehg.khizana.fakes.FakeExcludedFolderDao
import com.abosalehg.khizana.fakes.FakeLibraryScanner
import com.abosalehg.khizana.fakes.FakeTagDao
import com.abosalehg.khizana.fakes.FakeTopicDao
import com.abosalehg.khizana.fakes.PassThroughTransactionRunner
import com.abosalehg.khizana.data.scanner.ScannedFile
import com.abosalehg.khizana.domain.model.BookFormat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** A book's title and author as the reader sets them, and what a rescan does to them. */
class BookDetailsTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun aBlankTitleFallsBackToTheFileName() {
        assertEquals("0123_tafseer", normalizeTitle("   ", "0123_tafseer"))
        assertEquals("تفسير الطبري", normalizeTitle("  تفسير الطبري ", "x"))
    }

    @Test
    fun namesAreBounded() {
        assertEquals(MAX_NAME_LENGTH, normalizeTitle("ط".repeat(5_000), "x").length)
        assertEquals(MAX_NAME_LENGTH, normalizeAuthor("م".repeat(5_000))!!.length)
    }

    @Test
    fun aBlankAuthorIsNone() {
        assertNull(normalizeAuthor("  "))
        assertNull(normalizeAuthor(null))
    }

    @Test
    fun anEditedTitleSurvivesARescan() = runBlocking {
        val bookDao = FakeBookDao()
        val scanner = FakeLibraryScanner()
        val repository = LibraryRepository(
            bookDao = bookDao,
            topicDao = FakeTopicDao(),
            tagDao = FakeTagDao(),
            bookmarkDao = FakeBookmarkDao(),
            excludedFolderDao = FakeExcludedFolderDao(),
            scanner = scanner,
            coverStore = CoverStore(temp.newFolder("covers")),
            transaction = PassThroughTransactionRunner
        )
        val file = temp.newFile("0123_tafseer-v2.pdf").apply { writeText("bytes") }
        scanner.files = listOf(ScannedFile(file, BookFormat.PDF))
        repository.rescan()
        val id = bookDao.snapshot().single().id

        repository.updateBookDetails(id, "تفسير الطبري", "الطبري")
        // Moved and touched: the slow path, which rewrites the location fields.
        val moved = temp.newFolder("moved").resolve("renamed.pdf")
        file.copyTo(moved)
        file.delete()
        scanner.files = listOf(ScannedFile(moved, BookFormat.PDF))
        repository.rescan(deep = true)

        val book: BookEntity = bookDao.snapshot().single()
        assertEquals("تفسير الطبري", book.title)
        assertEquals("الطبري", book.author)
        assertEquals(moved.absolutePath, book.path)
    }

    @Test
    fun clearingTheTitleRestoresTheFileName() = runBlocking {
        val bookDao = FakeBookDao()
        bookDao.insert(
            BookEntity(id = "c".repeat(64), path = "/p/kitab.pdf", fileName = "kitab.pdf",
                format = "PDF", title = "Old")
        )
        val repository = LibraryRepository(
            bookDao, FakeTopicDao(), FakeTagDao(), FakeBookmarkDao(), FakeExcludedFolderDao(),
            FakeLibraryScanner(), CoverStore(temp.newFolder("c")), PassThroughTransactionRunner
        )

        repository.updateBookDetails("c".repeat(64), "", "")

        assertEquals("kitab", bookDao.snapshot().single().title)
        assertNull(bookDao.snapshot().single().author)
    }
}
