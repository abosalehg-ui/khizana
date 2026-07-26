package com.abosalehg.khizana.reader.engine

import android.graphics.Bitmap
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
}
