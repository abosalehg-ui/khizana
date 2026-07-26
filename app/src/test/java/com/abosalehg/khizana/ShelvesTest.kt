package com.abosalehg.khizana

import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.BookFormat
import com.abosalehg.khizana.domain.model.BookStatus
import com.abosalehg.khizana.domain.model.ReadingDirection
import com.abosalehg.khizana.domain.model.Topic
import com.abosalehg.khizana.ui.shelf.buildShelves
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelvesTest {

    private fun book(id: String, topicId: Long?) = Book(
        id = id,
        path = "/p/$id.pdf",
        fileName = "$id.pdf",
        format = BookFormat.PDF,
        title = id,
        author = null,
        topicId = topicId,
        pageCount = 0,
        locator = null,
        progress = 0f,
        readingDirection = ReadingDirection.AUTO,
        coverPath = null,
        coverFailed = false,
        status = BookStatus.OK,
        isHidden = false,
        manualOrder = 0,
        fileSize = 1,
        addedAt = 1,
        lastReadAt = null
    )

    @Test
    fun `new shelf comes first and holds topicless books`() {
        val shelves = buildShelves(
            topics = listOf(Topic(1, "تاريخ", 0)),
            books = listOf(book("a", null), book("b", 1))
        )

        assertNull(shelves.first().topicId)
        assertEquals(listOf("a"), shelves.first().books.map { it.id })
    }

    @Test
    fun `topics keep their order and receive their books`() {
        val shelves = buildShelves(
            topics = listOf(Topic(1, "تاريخ", 0), Topic(2, "روايات", 1)),
            books = listOf(book("a", 2), book("b", 1), book("c", 2))
        )

        assertEquals(listOf(null, 1L, 2L), shelves.map { it.topicId })
        assertEquals(listOf("b"), shelves[1].books.map { it.id })
        assertEquals(listOf("a", "c"), shelves[2].books.map { it.id })
    }

    @Test
    fun `empty topics still appear as drop targets`() {
        val shelves = buildShelves(
            topics = listOf(Topic(1, "فارغ", 0)),
            books = emptyList()
        )

        assertEquals(2, shelves.size)
        assertTrue(shelves.all { it.books.isEmpty() })
    }

    @Test
    fun `books pointing at an unknown topic fall back to the new shelf`() {
        val shelves = buildShelves(
            topics = listOf(Topic(1, "تاريخ", 0)),
            books = listOf(book("orphan", 99))
        )

        assertEquals(listOf("orphan"), shelves.first().books.map { it.id })
        assertTrue(shelves[1].books.isEmpty())
    }
}
