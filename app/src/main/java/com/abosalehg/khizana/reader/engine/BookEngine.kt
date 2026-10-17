package com.abosalehg.khizana.reader.engine

import android.graphics.Bitmap
import com.abosalehg.khizana.domain.model.BookStatus
import java.io.Closeable

/**
 * Format-agnostic rendering engine. One instance per open book; always
 * [close] when done. EPUB support arrives later behind this same interface.
 */
interface BookEngine : Closeable {

    val pageCount: Int

    /**
     * Renders one page scaled to [targetWidth] (height follows the page's
     * aspect ratio). Returns null if this page cannot be rendered.
     */
    fun renderPage(index: Int, targetWidth: Int): Bitmap?
}

/** Outcome of trying to open a book file. */
sealed interface EngineOpenResult {
    data class Success(val engine: BookEngine) : EngineOpenResult

    /** The file demands a password we don't have. */
    data object Protected : EngineOpenResult

    /** The file exists but cannot be parsed. */
    data object Corrupt : EngineOpenResult

    /**
     * There is no file at the book's path: a removed SD card, a file deleted
     * from a file manager. Kept apart from [Corrupt] on purpose — a book whose
     * card is out is not damaged, and labelling it so stuck a red "corrupt"
     * ribbon on a healthy file that no rescan ever took off again.
     */
    data object Missing : EngineOpenResult
}

/**
 * The status a failed open leaves on the book, or null for a success. The
 * reader and the cover generator both open books, and both must agree on
 * what each failure means.
 */
fun EngineOpenResult.failureStatus(): BookStatus? = when (this) {
    is EngineOpenResult.Success -> null
    EngineOpenResult.Protected -> BookStatus.PROTECTED
    EngineOpenResult.Corrupt -> BookStatus.CORRUPT
    EngineOpenResult.Missing -> BookStatus.MISSING
}
