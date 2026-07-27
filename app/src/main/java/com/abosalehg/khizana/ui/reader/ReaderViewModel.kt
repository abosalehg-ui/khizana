package com.abosalehg.khizana.ui.reader

import android.graphics.Bitmap
import android.util.LruCache
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abosalehg.khizana.data.repo.ReaderRepository
import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.BookStatus
import com.abosalehg.khizana.domain.model.ReadingDirection
import com.abosalehg.khizana.reader.engine.BookEngine
import com.abosalehg.khizana.reader.engine.EngineOpenResult
import com.abosalehg.khizana.reader.resolveReadingDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
     * All engine work runs on one thread: neither PdfRenderer nor ZipFile is
     * thread-safe, and this also guarantees close() can never race an
     * in-flight render.
     */
    private val engineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val engineScope = CoroutineScope(SupervisorJob() + engineDispatcher)
    private var engine: BookEngine? = null
    private var pageCount: Int = 0

    @Volatile
    private var released = false

    /**
     * Rendered pages, keyed by page and target width. Without it every page
     * turn re-rendered from scratch, including pages the pager still held.
     * Bitmaps are never recycled here — a displayed bitmap may still be
     * referenced by composition, so eviction leaves them to the collector.
     */
    private val pageCache = object : LruCache<String, Bitmap>(cacheSizeKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    private val _state = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val state: StateFlow<ReaderUiState> = _state

    /** Page-jump requests from the slider; the active pager animates to them. */
    private val _seekRequests = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val seekRequests: SharedFlow<Int> = _seekRequests.asSharedFlow()

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

    suspend fun renderPage(index: Int, targetWidth: Int): Bitmap? {
        if (released) return null
        val key = "$index@$targetWidth"
        pageCache.get(key)?.let { return it }
        val rendered = withContext(engineDispatcher) {
            // Re-check inside the engine thread: onCleared may have run while
            // this call was queued.
            if (released) null else engine?.renderPage(index, targetWidth)
        }
        if (rendered != null) pageCache.put(key, rendered)
        return rendered
    }

    /** Called when the pager settles on a page — persists locator + progress. */
    fun onPageSettled(index: Int) {
        if (_state.value !is ReaderUiState.Ready) return
        currentPage = index
        viewModelScope.launch { repository.savePosition(bookId, index, pageCount) }
    }

    /** Slider handoff: ask whichever pager is active to scroll to [page]. */
    fun requestPage(page: Int) {
        _seekRequests.tryEmit(page.coerceIn(0, (pageCount - 1).coerceAtLeast(0)))
    }

    override fun onCleared() {
        released = true
        pageCache.evictAll()
        engineScope.launch {
            engine?.close()
            engine = null
            engineDispatcher.close()
        }
    }

    private companion object {
        /** A quarter of the heap, clamped so a big-heap device stays sane. */
        fun cacheSizeKb(): Int {
            val maxKb = (Runtime.getRuntime().maxMemory() / 1024).coerceAtMost(Int.MAX_VALUE.toLong())
            return (maxKb / 4).toInt().coerceIn(8 * 1024, 96 * 1024)
        }
    }
}
