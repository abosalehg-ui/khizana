package com.abosalehg.khizana.ui.reader

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.BookStatus
import com.abosalehg.khizana.domain.model.ReadingDirection
import com.abosalehg.khizana.domain.repo.ReaderRepository
import com.abosalehg.khizana.reader.engine.BookEngine
import com.abosalehg.khizana.reader.engine.EngineOpenResult
import com.abosalehg.khizana.reader.resolveReadingDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import javax.inject.Inject

sealed interface ReaderUiState {
    data object Loading : ReaderUiState

    data class Ready(
        val book: Book,
        val pageCount: Int,
        val initialPage: Int,
        val isRtl: Boolean
    ) : ReaderUiState

    data object Protected : ReaderUiState
    data object Corrupt : ReaderUiState
    data object Missing : ReaderUiState
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReaderRepository
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])

    /**
     * All engine work runs on one thread: Pdfium isn't thread-safe, and this
     * also guarantees close() can never race an in-flight render.
     */
    private val engineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val engineScope = CoroutineScope(SupervisorJob() + engineDispatcher)
    private var engine: BookEngine? = null
    private var pageCount: Int = 0

    private val _state = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val state: StateFlow<ReaderUiState> = _state

    /** Last settled page — survives rotation so the pager reopens in place. */
    var currentPage: Int = 0
        private set

    init {
        viewModelScope.launch {
            _state.value = withContext(engineDispatcher) { openBook() }
        }
    }

    private suspend fun openBook(): ReaderUiState {
        val book = repository.getBook(bookId) ?: return ReaderUiState.Missing
        if (book.status == BookStatus.MISSING) return ReaderUiState.Missing
        return when (val result = repository.openEngine(book)) {
            is EngineOpenResult.Success -> {
                val opened = result.engine
                if (opened.pageCount <= 0) {
                    opened.close()
                    repository.markStatus(bookId, BookStatus.CORRUPT)
                    ReaderUiState.Corrupt
                } else {
                    engine = opened
                    pageCount = opened.pageCount
                    if (book.status != BookStatus.OK) repository.markStatus(bookId, BookStatus.OK)
                    if (book.pageCount != pageCount) repository.updatePageCount(bookId, pageCount)
                    val initialPage = (book.locator?.toIntOrNull() ?: 0)
                        .coerceIn(0, pageCount - 1)
                    currentPage = initialPage
                    ReaderUiState.Ready(
                        book = book,
                        pageCount = pageCount,
                        initialPage = initialPage,
                        isRtl = resolveReadingDirection(
                            book.readingDirection,
                            book.title
                        ) == ReadingDirection.RTL
                    )
                }
            }
            EngineOpenResult.Protected -> {
                repository.markStatus(bookId, BookStatus.PROTECTED)
                ReaderUiState.Protected
            }
            EngineOpenResult.Corrupt -> {
                repository.markStatus(bookId, BookStatus.CORRUPT)
                ReaderUiState.Corrupt
            }
        }
    }

    suspend fun renderPage(index: Int, targetWidth: Int): Bitmap? =
        withContext(engineDispatcher) { engine?.renderPage(index, targetWidth) }

    /** Called when the pager settles on a page — persists locator + progress. */
    fun onPageSettled(index: Int) {
        if (_state.value !is ReaderUiState.Ready) return
        currentPage = index
        viewModelScope.launch { repository.savePosition(bookId, index, pageCount) }
    }

    override fun onCleared() {
        engineScope.launch {
            engine?.close()
            engine = null
            engineDispatcher.close()
        }
    }
}
