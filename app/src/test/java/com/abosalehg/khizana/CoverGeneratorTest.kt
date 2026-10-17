package com.abosalehg.khizana

import com.abosalehg.khizana.data.covers.CoverGenerator
import com.abosalehg.khizana.data.covers.CoverStore
import com.abosalehg.khizana.data.db.BookEntity
import com.abosalehg.khizana.fakes.FakeBookDao
import com.abosalehg.khizana.fakes.FakeEngineFactory
import com.abosalehg.khizana.reader.engine.EngineOpenResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The statuses cover generation files a book under when it cannot draw one. */
class CoverGeneratorTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val bookDao = FakeBookDao()

    private fun book(path: String) = BookEntity(
        id = "b".repeat(64),
        path = path,
        fileName = "book.pdf",
        format = "PDF",
        title = "book"
    )

    private fun generate(path: String, result: EngineOpenResult): FakeEngineFactory = runBlocking {
        bookDao.insert(book(path))
        val engines = FakeEngineFactory(result)
        CoverGenerator(bookDao, CoverStore(temp.newFolder("covers")), engines).generateMissing()
        engines
    }

    private fun status() = bookDao.snapshot().single().status

    @Test
    fun aFileGoneSinceTheScanIsMissingAndNeverOpened() {
        val engines = generate("${temp.root}/gone.pdf", EngineOpenResult.Corrupt)

        assertEquals("MISSING", status())
        assertTrue("no engine is opened for a file that is not there", engines.opened.isEmpty())
    }

    @Test
    fun anUnparseablePdfIsCorrupt() {
        val file = temp.newFile("bad.pdf").apply { writeText("not a pdf") }
        generate(file.absolutePath, EngineOpenResult.Corrupt)
        assertEquals("CORRUPT", status())
    }

    @Test
    fun aPasswordProtectedPdfIsProtected() {
        val file = temp.newFile("locked.pdf").apply { writeText("%PDF") }
        generate(file.absolutePath, EngineOpenResult.Protected)
        assertEquals("PROTECTED", status())
    }
}
