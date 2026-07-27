package com.abosalehg.khizana.data.repo

import com.abosalehg.khizana.data.db.BookDao
import com.abosalehg.khizana.data.db.BookmarkDao
import com.abosalehg.khizana.data.db.BookmarkEntity
import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.BookStatus
import com.abosalehg.khizana.domain.model.Bookmark
import com.abosalehg.khizana.domain.model.normalizeNote
import com.abosalehg.khizana.reader.engine.EngineFactory
import com.abosalehg.khizana.reader.engine.EngineOpenResult
import com.abosalehg.khizana.reader.readingProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderRepository @Inject constructor(
    private val bookDao: BookDao,
    private val bookmarkDao: BookmarkDao
) {

    suspend fun getBook(id: String): Book? =
        withContext(Dispatchers.IO) { bookDao.getById(id)?.toDomain() }

    fun openEngine(book: Book): EngineOpenResult =
        EngineFactory.open(book.path, book.format)

    suspend fun markStatus(bookId: String, status: BookStatus) =
        bookDao.setStatus(bookId, status.name)

    suspend fun updatePageCount(bookId: String, pageCount: Int) =
        bookDao.updatePageCount(bookId, pageCount)

    /** Locator is the page index as a String — EPUB CFIs reuse the same column later. */
    suspend fun savePosition(bookId: String, pageIndex: Int, pageCount: Int) =
        bookDao.saveReadingPosition(
            id = bookId,
            locator = pageIndex.toString(),
            progress = readingProgress(pageIndex, pageCount),
            lastReadAt = System.currentTimeMillis()
        )

    // ---- Bookmarks ----

    fun bookmarks(bookId: String): Flow<List<Bookmark>> =
        bookmarkDao.observeForBook(bookId).map { rows -> rows.map { it.toDomain() } }

    /**
     * Saves the note for [page], creating the bookmark if the page has none.
     *
     * A page carries at most one bookmark: saving again on a bookmarked page
     * is an edit, not a second row, so the reader's bookmark button stays a
     * plain toggle-and-annotate instead of accumulating duplicates.
     */
    suspend fun saveBookmark(bookId: String, page: Int, note: String?) {
        val normalized = normalizeNote(note)
        val existing = bookmarkDao.findAt(bookId, page)
        if (existing == null) {
            bookmarkDao.insert(
                BookmarkEntity(
                    bookId = bookId,
                    page = page,
                    note = normalized,
                    createdAt = System.currentTimeMillis()
                )
            )
        } else {
            bookmarkDao.updateNote(existing.id, normalized)
        }
    }

    suspend fun deleteBookmark(id: Long) = bookmarkDao.deleteById(id)
}

internal fun BookmarkEntity.toDomain(): Bookmark = Bookmark(
    id = id,
    bookId = bookId,
    page = page,
    note = note,
    createdAt = createdAt
)
