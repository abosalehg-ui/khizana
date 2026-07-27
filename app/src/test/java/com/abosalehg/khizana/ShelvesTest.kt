package com.abosalehg.khizana

import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.BookFormat
import com.abosalehg.khizana.domain.model.BookStatus
import com.abosalehg.khizana.domain.model.ReadingDirection
import com.abosalehg.khizana.domain.model.ShelfSort
import com.abosalehg.khizana.domain.model.Topic
import com.abosalehg.khizana.ui.shelf.buildShelves
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelvesTest {

    private fun book(
        id: String,
        topicId: Long?,
        title: String = id,
        progress: Float = 0f,
        lastReadAt: Long? = null,
        manualOrder: Int = 0,
        fileSize: Long = 1,
        addedAt: Long = 1
    ) = Book(
        id = id,
        path = "/p/$id.pdf",
        fileName = "$id.pdf",
        format = BookFormat.PDF,
        title = title,
        author = null,
        topicId = topicId,
        pageCount = 0,
        locator = null,
        progress = progress,
        readingDirection = ReadingDirection.AUTO,
        coverPath = null,
        coverFailed = false,
        status = BookStatus.OK,
        isHidden = false,
        manualOrder = manualOrder,
        fileSize = fileSize,
        addedAt = addedAt,
        lastReadAt = lastReadAt
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
    fun `continue reading shelf appears first with in-progress books newest-read first`() {
        val shelves = buildShelves(
            topics = emptyList(),
            books = listOf(
                book("done", null, progress = 1f, lastReadAt = 50),
                book("old", null, progress = 0.5f, lastReadAt = 10),
                book("recent", null, progress = 0.2f, lastReadAt = 99),
                book("untouched", null)
            )
        )

        assertEquals(
            com.abosalehg.khizana.ui.shelf.ShelfKind.CONTINUE_READING,
            shelves.first().kind
        )
        assertEquals(listOf("recent", "old"), shelves.first().books.map { it.id })
    }

    @Test
    fun `continue reading shelf is absent when nothing is in progress`() {
        val shelves = buildShelves(topics = emptyList(), books = listOf(book("a", null)))

        assertEquals(com.abosalehg.khizana.ui.shelf.ShelfKind.NEW, shelves.first().kind)
    }

    @Test
    fun `continue reading is capped`() {
        val many = (1..15).map {
            book("b$it", null, progress = 0.5f, lastReadAt = it.toLong())
        }
        val shelves = buildShelves(topics = emptyList(), books = many)

        assertEquals(
            com.abosalehg.khizana.ui.shelf.CONTINUE_READING_LIMIT,
            shelves.first().books.size
        )
    }

    @Test
    fun `books within a shelf follow natural title order`() {
        val shelves = buildShelves(
            topics = emptyList(),
            books = listOf(
                book("x", null, title = "المجلد 10"),
                book("y", null, title = "المجلد 2"),
                book("z", null, title = "المجلد 1")
            )
        )

        assertEquals(
            listOf("المجلد 1", "المجلد 2", "المجلد 10"),
            shelves.first().books.map { it.title }
        )
    }

    @Test
    fun `manually ordered books come before unordered ones`() {
        val shelves = buildShelves(
            topics = emptyList(),
            books = listOf(
                book("auto", null, title = "آلف"),
                book("second", null, title = "ياء", manualOrder = 2),
                book("first", null, title = "واو", manualOrder = 1)
            )
        )

        assertEquals(
            listOf("first", "second", "auto"),
            shelves.first().books.map { it.id }
        )
    }

    @Test
    fun `sorting by name ignores the manual order entirely`() {
        val shelves = buildShelves(
            topics = emptyList(),
            books = listOf(
                book("last", null, title = "ياء", manualOrder = 1),
                book("first", null, title = "ألف", manualOrder = 9)
            ),
            sort = ShelfSort.TITLE
        )

        assertEquals(listOf("first", "last"), shelves.first().books.map { it.id })
    }

    @Test
    fun `sorting by date puts the newest addition at the front of the shelf`() {
        val shelves = buildShelves(
            topics = emptyList(),
            books = listOf(
                book("old", null, addedAt = 10),
                book("newest", null, addedAt = 300),
                book("middle", null, addedAt = 50)
            ),
            sort = ShelfSort.DATE_ADDED
        )

        assertEquals(listOf("newest", "middle", "old"), shelves.first().books.map { it.id })
    }

    @Test
    fun `sorting by size puts the largest file first`() {
        val shelves = buildShelves(
            topics = emptyList(),
            books = listOf(
                book("small", null, fileSize = 1_000),
                book("huge", null, fileSize = 900_000_000),
                book("medium", null, fileSize = 40_000)
            ),
            sort = ShelfSort.SIZE
        )

        assertEquals(listOf("huge", "medium", "small"), shelves.first().books.map { it.id })
    }

    @Test
    fun `books that tie on date or size fall back to title order`() {
        // A single scan stamps every new book with the same addedAt, so without
        // the tie-break the shelf would reshuffle on every emission.
        val sameScan = listOf(
            book("b", null, title = "المجلد 10", addedAt = 7, fileSize = 5),
            book("a", null, title = "المجلد 2", addedAt = 7, fileSize = 5)
        )

        assertEquals(
            listOf("a", "b"),
            buildShelves(emptyList(), sameScan, ShelfSort.DATE_ADDED).first().books.map { it.id }
        )
        assertEquals(
            listOf("a", "b"),
            buildShelves(emptyList(), sameScan, ShelfSort.SIZE).first().books.map { it.id }
        )
    }

    @Test
    fun `continue reading stays newest-read first whatever the shelf sort is`() {
        val shelves = buildShelves(
            topics = emptyList(),
            books = listOf(
                book("bigOldRead", null, progress = 0.5f, lastReadAt = 5, fileSize = 999),
                book("smallJustRead", null, progress = 0.5f, lastReadAt = 500, fileSize = 1)
            ),
            sort = ShelfSort.SIZE
        )

        assertEquals(
            listOf("smallJustRead", "bigOldRead"),
            shelves.first().books.map { it.id }
        )
    }

    @Test
    fun `the default sort is the hand-made order`() {
        val books = listOf(
            book("auto", null, title = "ألف"),
            book("placed", null, title = "ياء", manualOrder = 1)
        )

        assertEquals(
            buildShelves(emptyList(), books, ShelfSort.MANUAL),
            buildShelves(emptyList(), books)
        )
    }

    @Test
    fun `tag filter keeps shelf structure but drops unmatched books`() {
        val shelves = buildShelves(
            topics = listOf(Topic(1, "تاريخ", 0)),
            books = listOf(book("a", null), book("b", 1), book("c", 1))
        )

        val filtered = com.abosalehg.khizana.ui.shelf.filterShelvesByBookIds(
            shelves,
            setOf("b")
        )

        assertEquals(shelves.map { it.topicId }, filtered.map { it.topicId })
        assertTrue(filtered.first().books.isEmpty())
        assertEquals(listOf("b"), filtered[1].books.map { it.id })
    }

    @Test
    fun `null tag filter is a no-op`() {
        val shelves = buildShelves(
            topics = emptyList(),
            books = listOf(book("a", null))
        )

        assertEquals(
            shelves,
            com.abosalehg.khizana.ui.shelf.filterShelvesByBookIds(shelves, null)
        )
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
