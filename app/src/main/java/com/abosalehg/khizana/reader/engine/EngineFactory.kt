package com.abosalehg.khizana.reader.engine

import com.abosalehg.khizana.domain.model.BookFormat
import java.io.File

/**
 * Opens a book file with the engine its format needs. An interface so the
 * reader and the cover generator can be tested on the JVM against a fake —
 * the platform engines need a device.
 */
interface BookEngineFactory {
    fun open(path: String, format: BookFormat): EngineOpenResult
}

/** The real factory: platform PdfRenderer for PDF, ZipFile for CBZ. */
object EngineFactory : BookEngineFactory {

    override fun open(path: String, format: BookFormat): EngineOpenResult {
        val file = File(path)
        if (path.isEmpty() || !file.isFile) return EngineOpenResult.Missing
        return when (format) {
            BookFormat.PDF -> PdfEngine.open(file)
            BookFormat.CBZ -> CbzEngine.open(file)
            BookFormat.EPUB -> EngineOpenResult.Corrupt // not supported yet
        }
    }
}
