package com.abosalehg.khizana.reader.engine

import android.content.Context
import com.abosalehg.khizana.domain.model.BookFormat
import java.io.File

/** Picks the right engine for a book's format. */
object EngineFactory {

    fun open(context: Context, path: String, format: BookFormat): EngineOpenResult {
        val file = File(path)
        if (!file.isFile) return EngineOpenResult.Corrupt
        return when (format) {
            BookFormat.PDF -> PdfEngine.open(context, file)
            BookFormat.CBZ -> CbzEngine.open(file)
            BookFormat.EPUB -> EngineOpenResult.Corrupt // not supported yet
        }
    }
}
