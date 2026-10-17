package com.abosalehg.khizana

import androidx.lifecycle.SavedStateHandle
import com.abosalehg.khizana.data.db.BookEntity
import com.abosalehg.khizana.data.repo.ReaderRepository
import com.abosalehg.khizana.domain.model.ReadingDirection
import com.abosalehg.khizana.fakes.FakeBookDao
import com.abosalehg.khizana.fakes.FakeBookEngine
import com.abosalehg.khizana.fakes.FakeBookmarkDao
import com.abosalehg.khizana.fakes.FakeEngineFactory
import com.abosalehg.khizana.reader.engine.EngineOpenResult
import com.abosalehg.khizana.ui.reader.ReaderUiState
import com.abosalehg.khizana.ui.reader.ReaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * What opening a book does to its status. This is the decision the shelves
 * show as a ribbon, and it used to be untestable: the engine factory was a
 * static object, so a missing file could only be checked on a device — which
 * is how a removed SD card came to label healthy books "corrupt".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    private val bookId = "a".repeat(64)
    private lateinit var bookDao: FakeBookDao

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        bookDao = FakeBookDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun open(
        result: EngineOpenResult,
        status: String = "OK",
        title: String = "Muqaddimah",
        direction: String = "AUTO"
    ): ReaderViewModel = runBlocking {
        bookDao.insert(
            BookEntity(
                id = bookId,
                path = "/storage/emulated/0/Books/muqaddimah.pdf",
                fileName = "muqaddimah.pdf",
                format = "PDF",
                title = title,
                status = status,
                readingDirection = direction
            )
        )
        ReaderViewModel(
            SavedStateHandle(mapOf("bookId" to bookId)),
            ReaderRepository(bookDao, FakeBookmarkDao(), FakeEngineFactory(result))
        )
    }

    private fun ReaderViewModel.settled(): ReaderUiState = runBlocking {
        withTimeout(5_000) { state.first { it !is ReaderUiState.Loading } }
    }

    private fun status(): String = runBlocking { bookDao.getById(bookId)!!.status }

    @Test
    fun aMissingFileIsFiledAsMissingNotCorrupt() {
        val state = open(EngineOpenResult.Missing).settled()

        assertEquals(ReaderUiState.Missing, state)
        assertEquals("MISSING", status())
    }

    @Test
    fun anUnparseableFileIsCorrupt() {
        assertEquals(ReaderUiState.Corrupt, open(EngineOpenResult.Corrupt).settled())
        assertEquals("CORRUPT", status())
    }

    @Test
    fun aPasswordProtectedFileIsProtected() {
        assertEquals(ReaderUiState.Protected, open(EngineOpenResult.Protected).settled())
        assertEquals("PROTECTED", status())
    }

    @Test
    fun aBookThatOpensClearsAStaleCorruptLabel() {
        val engine = FakeBookEngine(pageCount = 12)
        val state = open(EngineOpenResult.Success(engine), status = "CORRUPT").settled()

        assertTrue(state is ReaderUiState.Ready)
        assertEquals("OK", status())
        assertEquals(12, runBlocking { bookDao.getById(bookId)!!.pageCount })
    }

    @Test
    fun anEmptyDocumentIsCorruptAndItsEngineIsClosed() {
        val engine = FakeBookEngine(pageCount = 0)
        assertEquals(ReaderUiState.Corrupt, open(EngineOpenResult.Success(engine)).settled())
        assertTrue(engine.closed)
        assertEquals("CORRUPT", status())
    }

    @Test
    fun theReaderCanTurnALatinNamedArabicBookRightToLeft() {
        val viewModel = open(EngineOpenResult.Success(FakeBookEngine(10)), title = "scan_0042")
        val before = viewModel.settled() as ReaderUiState.Ready
        assertFalse("a Latin title guesses left-to-right", before.isRtl)

        viewModel.setReadingDirection(ReadingDirection.RTL)

        val after = viewModel.state.value as ReaderUiState.Ready
        assertTrue(after.isRtl)
        assertEquals(ReadingDirection.RTL, after.direction)
        assertEquals("RTL", runBlocking { bookDao.getById(bookId)!!.readingDirection })
    }

    @Test
    fun aStoredDirectionWinsOverTheTitleGuess() {
        val state = open(
            EngineOpenResult.Success(FakeBookEngine(10)),
            title = "تاريخ الطبري",
            direction = "LTR"
        ).settled() as ReaderUiState.Ready

        assertFalse(state.isRtl)
        assertEquals(ReadingDirection.LTR, state.direction)
    }

    @Test
    fun backToAutomaticReturnsToTheTitleGuess() {
        val viewModel = open(
            EngineOpenResult.Success(FakeBookEngine(10)),
            title = "تاريخ الطبري",
            direction = "LTR"
        )
        viewModel.settled()

        viewModel.setReadingDirection(ReadingDirection.AUTO)

        assertTrue((viewModel.state.value as ReaderUiState.Ready).isRtl)
        assertEquals("AUTO", runBlocking { bookDao.getById(bookId)!!.readingDirection })
    }
}
