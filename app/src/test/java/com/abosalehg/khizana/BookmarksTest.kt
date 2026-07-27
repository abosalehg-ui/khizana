package com.abosalehg.khizana

import com.abosalehg.khizana.domain.model.Bookmark
import com.abosalehg.khizana.domain.model.MAX_NOTE_LENGTH
import com.abosalehg.khizana.domain.model.bookmarkAt
import com.abosalehg.khizana.domain.model.normalizeNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The two rules the bookmark UI leans on: "no note" has exactly one
 * representation, and a page carries at most one bookmark.
 */
class BookmarksTest {

    private fun bookmark(page: Int, note: String? = null) =
        Bookmark(id = page.toLong(), bookId = "b", page = page, note = note, createdAt = 0)

    @Test
    fun `a blank note is stored as no note at all`() {
        assertNull(normalizeNote(""))
        assertNull(normalizeNote("   "))
        assertNull(normalizeNote("\n\t "))
        assertNull(normalizeNote(null))
    }

    @Test
    fun `notes are trimmed rather than stored with their padding`() {
        assertEquals("موضع الشاهد", normalizeNote("  موضع الشاهد \n"))
    }

    @Test
    fun `an over-long note is capped instead of rejected`() {
        val note = normalizeNote("ا".repeat(MAX_NOTE_LENGTH + 50))
        assertEquals(MAX_NOTE_LENGTH, note!!.length)
    }

    @Test
    fun `the bookmark of a page is found by page, not by position`() {
        val bookmarks = listOf(bookmark(2, "أ"), bookmark(40), bookmark(7, "ب"))

        assertEquals("ب", bookmarkAt(bookmarks, 7)?.note)
        assertNull(bookmarkAt(bookmarks, 3))
    }

    @Test
    fun `a page with no bookmarks resolves to null on an empty book`() {
        assertNull(bookmarkAt(emptyList(), 0))
    }
}
