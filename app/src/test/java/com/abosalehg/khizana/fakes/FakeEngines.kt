package com.abosalehg.khizana.fakes

import android.graphics.Bitmap
import com.abosalehg.khizana.domain.model.BookFormat
import com.abosalehg.khizana.reader.engine.BookEngine
import com.abosalehg.khizana.reader.engine.BookEngineFactory
import com.abosalehg.khizana.reader.engine.EngineOpenResult

/** An open book with [pageCount] pages that renders nothing — no device needed. */
class FakeBookEngine(override val pageCount: Int) : BookEngine {
    var closed = false
    override fun renderPage(index: Int, targetWidth: Int): Bitmap? = null
    override fun close() {
        closed = true
    }
}

/** Answers every open with [result], and remembers what it was asked to open. */
class FakeEngineFactory(var result: EngineOpenResult) : BookEngineFactory {
    val opened = mutableListOf<Pair<String, BookFormat>>()
    override fun open(path: String, format: BookFormat): EngineOpenResult {
        opened += path to format
        return result
    }
}
