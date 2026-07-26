package com.abosalehg.khizana.domain.repo

import android.content.Context
import com.abosalehg.khizana.data.db.BookDao
import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.BookStatus
import com.abosalehg.khizana.reader.engine.EngineFactory
import com.abosalehg.khizana.reader.engine.EngineOpenResult
import com.abosalehg.khizana.reader.readingProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao
) {

    suspend fun getBook(id: String): Book? =
        withContext(Dispatchers.IO) { bookDao.getById(id)?.toDomain() }

    fun openEngine(book: Book): EngineOpenResult =
        EngineFactory.open(context, book.path, book.format)

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
}
