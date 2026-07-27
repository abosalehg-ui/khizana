package com.abosalehg.khizana

import com.abosalehg.khizana.data.repo.ReaderRepository
import com.abosalehg.khizana.fakes.FakeBookDao
import com.abosalehg.khizana.fakes.FakeBookmarkDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * One bookmark per page is the rule the reader's bookmark button is built on:
 * the button is a toggle, and saving again on a saved page is an edit. If the
 * repository ever stacked a second row, the button would start lying about
 * which note it is showing.
 */
class ReaderBookmarksTest {

    private lateinit var bookmarkDao: FakeBookmarkDao
    private lateinit var repository: ReaderRepository

    @Before
    fun setUp() {
        bookmarkDao = FakeBookmarkDao()
        repository = ReaderRepository(bookDao = FakeBookDao(), bookmarkDao = bookmarkDao)
    }

    @Test
    fun savingTwiceOnTheSamePageEditsTheNoteInsteadOfAddingARow() = runBlocking {
        repository.saveBookmark("book", page = 12, note = "أولى")
        repository.saveBookmark("book", page = 12, note = "ثانية")

        val bookmark = bookmarkDao.snapshot().single()
        assertEquals(12, bookmark.page)
        assertEquals("ثانية", bookmark.note)
    }

    @Test
    fun aBlankNoteIsStoredAsNoNote() = runBlocking {
        repository.saveBookmark("book", page = 1, note = "   ")

        assertNull(bookmarkDao.snapshot().single().note)
    }

    @Test
    fun clearingTheNoteKeepsTheBookmarkItself() = runBlocking {
        repository.saveBookmark("book", page = 1, note = "ملاحظة")
        repository.saveBookmark("book", page = 1, note = "")

        val bookmark = bookmarkDao.snapshot().single()
        assertNull("the note is gone", bookmark.note)
        assertEquals("the page is still bookmarked", 1, bookmark.page)
    }

    @Test
    fun differentPagesGetTheirOwnBookmarks() = runBlocking {
        repository.saveBookmark("book", page = 1, note = null)
        repository.saveBookmark("book", page = 2, note = "ب")
        repository.saveBookmark("other", page = 1, note = "كتاب آخر")

        assertEquals(listOf(1, 2), repository.bookmarks("book").first().map { it.page })
        assertEquals(listOf("كتاب آخر"), repository.bookmarks("other").first().map { it.note })
    }

    @Test
    fun deletingRemovesOnlyThatBookmark() = runBlocking {
        repository.saveBookmark("book", page = 1, note = "أ")
        repository.saveBookmark("book", page = 2, note = "ب")
        val first = repository.bookmarks("book").first().first { it.page == 1 }

        repository.deleteBookmark(first.id)

        val left = repository.bookmarks("book").first()
        assertEquals(listOf(2), left.map { it.page })
        assertTrue(bookmarkDao.snapshot().none { it.id == first.id })
    }
}
