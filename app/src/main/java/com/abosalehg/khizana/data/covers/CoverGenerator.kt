package com.abosalehg.khizana.data.covers

import android.content.Context
import com.abosalehg.khizana.data.db.BookDao
import com.abosalehg.khizana.domain.model.BookFormat
import com.abosalehg.khizana.domain.model.BookStatus
import com.abosalehg.khizana.reader.engine.EngineOpenResult
import com.abosalehg.khizana.reader.engine.PdfEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates missing cover thumbnails and, as a side effect of opening each
 * file, discovers page counts and PROTECTED/CORRUPT statuses.
 * A failed cover attempt is recorded (coverFailed) and not retried until reset.
 */
@Singleton
class CoverGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val coverStore: CoverStore
) {

    suspend fun generateMissing(
        onProgress: suspend (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): Unit = withContext(Dispatchers.IO) {
        val pending = bookDao.getNeedingCovers()
        pending.forEachIndexed { index, book ->
            when (BookFormat.fromName(book.format)) {
                BookFormat.PDF -> generatePdfCover(book.id, File(book.path))
                BookFormat.CBZ -> generateCbzCover(book.id, File(book.path))
                else -> bookDao.setCoverFailed(book.id)
            }
            onProgress(index + 1, pending.size)
        }
    }

    private suspend fun generatePdfCover(bookId: String, file: File) {
        when (val result = PdfEngine.open(context, file)) {
            is EngineOpenResult.Success -> result.engine.use { engine ->
                val cover = engine.renderPage(0, COVER_WIDTH_PX)
                if (cover != null) {
                    val saved = coverStore.save(bookId, cover)
                    cover.recycle()
                    bookDao.setCover(bookId, saved.absolutePath, engine.pageCount)
                } else {
                    bookDao.setCoverFailed(bookId)
                }
            }
            EngineOpenResult.Protected -> bookDao.setStatus(bookId, BookStatus.PROTECTED.name)
            EngineOpenResult.Corrupt -> bookDao.setStatus(bookId, BookStatus.CORRUPT.name)
        }
    }

    private suspend fun generateCbzCover(bookId: String, file: File) {
        val extraction = try {
            CbzCover.extract(file, COVER_WIDTH_PX)
        } catch (e: Exception) {
            bookDao.setStatus(bookId, BookStatus.CORRUPT.name)
            return
        }
        if (extraction != null) {
            val saved = coverStore.save(bookId, extraction.cover)
            extraction.cover.recycle()
            bookDao.setCover(bookId, saved.absolutePath, extraction.imageCount)
        } else {
            bookDao.setCoverFailed(bookId)
        }
    }

    companion object {
        const val COVER_WIDTH_PX = 480
    }
}
