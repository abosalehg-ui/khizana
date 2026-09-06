package com.abosalehg.khizana.ui.shelf

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.abosalehg.khizana.data.repo.LibraryRepository
import com.abosalehg.khizana.data.scanner.StoragePermission
import com.abosalehg.khizana.data.settings.SettingsRepository
import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.ShelfSort
import com.abosalehg.khizana.util.normalizeForSearch
import com.abosalehg.khizana.work.CoverWorker
import com.abosalehg.khizana.work.ScanWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanUiState(
    val running: Boolean = false,
    val processed: Int = 0,
    val total: Int = 0,
    val lastScanned: Int? = null,
    val lastAdded: Int = 0,
    val lastRelocated: Int = 0,
    val lastMissing: Int = 0,
    /**
     * The last scan ended in [WorkInfo.State.FAILED].
     *
     * It needs its own flag: without one, a failed scan collapsed onto the
     * default state, which is also "no scan has ever run" — so the reader was
     * shown an empty library and no reason for it.
     */
    val failed: Boolean = false
)

/** Cover generation, which runs on its own after every scan. */
data class CoverUiState(
    val running: Boolean = false,
    val processed: Int = 0,
    val total: Int = 0
)

/**
 * [loading] stays true until the first database emission, so the shelves no
 * longer flash an "empty library" message on every cold start. [hasAnyBook]
 * separates a genuinely empty library from a filter that matched nothing.
 */
data class LibraryUiState(
    val loading: Boolean = true,
    val shelves: List<Shelf> = emptyList(),
    /**
     * Distinct books after filtering. Summing shelf sizes double-counts:
     * anything on "Continue reading" also stands on its own shelf.
     */
    val bookCount: Int = 0,
    val hasAnyBook: Boolean = false
)

/** A book plus its pre-normalized search text, computed once per DB emission. */
private data class IndexedBook(val book: Book, val searchBlob: String)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LibraryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    /** Selected tag filter; null shows everything. */
    private val _selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagId: StateFlow<Long?> = _selectedTagId

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val tags = repository.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Search text is normalized once per book per database emission instead of
     * three times per book per keystroke.
     */
    private val indexedBooks: Flow<List<IndexedBook>> = repository.visibleBooks
        .map { books ->
            books.map { book ->
                IndexedBook(
                    book = book,
                    searchBlob = normalizeForSearch(
                        buildString {
                            append(book.title).append('\n')
                            append(book.fileName)
                            book.author?.let { append('\n').append(it) }
                        }
                    )
                )
            }
        }
        .flowOn(Dispatchers.Default)

    @OptIn(FlowPreview::class)
    private val debouncedQuery: Flow<String> = _searchQuery
        .map { it.trim() }
        .distinctUntilChanged()
        // Clearing the field must feel instant; typing waits for a pause.
        .debounce { query -> if (query.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }

    val shelfSort: StateFlow<ShelfSort> = settingsRepository.shelfSort
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShelfSort.MANUAL)

    /** Everything the reader controls from the header, in one flow. */
    private data class ViewOptions(val tagId: Long?, val query: String, val sort: ShelfSort)

    // Folded into one flow because `combine` tops out at five sources and the
    // library state already reads three from the database.
    private val viewOptions: Flow<ViewOptions> =
        combine(_selectedTagId, debouncedQuery, settingsRepository.shelfSort) { tagId, query, sort ->
            ViewOptions(tagId, query, sort)
        }

    /**
     * Grouping, sorting and filtering all run on [Dispatchers.Default]. They
     * used to run on the main thread for every keystroke and every database
     * emission, which is an ANR waiting to happen on a large library.
     */
    val libraryState: StateFlow<LibraryUiState> =
        combine(
            repository.topics,
            indexedBooks,
            repository.bookTagRefs,
            viewOptions
        ) { topics, indexed, refs, options ->
            val (tagId, query, sort) = options
            var shelves = buildShelves(topics, indexed.map { it.book }, sort)
            tagId?.let { id ->
                val tagged = refs.filter { it.tagId == id }.mapTo(HashSet()) { it.bookId }
                shelves = filterShelvesByBookIds(shelves, tagged)
            }
            if (query.isNotEmpty()) {
                val normalized = normalizeForSearch(query)
                val matching = indexed
                    .filter { it.searchBlob.contains(normalized) }
                    .mapTo(HashSet()) { it.book.id }
                shelves = filterShelvesByBookIds(shelves, matching)
            }
            LibraryUiState(
                loading = false,
                shelves = shelves,
                bookCount = shelves
                    .filter { it.kind != ShelfKind.CONTINUE_READING }
                    .sumOf { it.books.size },
                hasAnyBook = indexed.isNotEmpty()
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    private val _permissionGranted = MutableStateFlow(StoragePermission.isGranted(context))
    val permissionGranted: StateFlow<Boolean> = _permissionGranted

    val scanState: StateFlow<ScanUiState> = workManager
        .getWorkInfosForUniqueWorkFlow(ScanWorker.UNIQUE_NAME)
        .map { infos -> infos.firstOrNull().toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanUiState())

    /**
     * Cover generation reports progress the same way the scan does, and until
     * something collected it nobody saw it: after a scan the shelves filled
     * with grey placeholders that turned into covers minutes later, with no
     * indication that anything was working.
     */
    val coverState: StateFlow<CoverUiState> = workManager
        .getWorkInfosForUniqueWorkFlow(CoverWorker.UNIQUE_NAME)
        .map { infos -> infos.firstOrNull().toCoverUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoverUiState())

    /** Called from the UI on resume — the grant happens in system settings. */
    fun refreshPermission() {
        _permissionGranted.value = StoragePermission.isGranted(context)
    }

    fun startScan() {
        viewModelScope.launch {
            val deep = settingsRepository.deepScanEnabled.first()
            workManager.enqueueUniqueWork(
                ScanWorker.UNIQUE_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<ScanWorker>()
                    .setInputData(workDataOf(ScanWorker.KEY_DEEP to deep))
                    .build()
            )
        }
    }

    fun deleteBook(book: Book, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repository.deleteBookPermanently(book)) }
    }

    fun addTopic(name: String) {
        viewModelScope.launch { repository.addTopic(name) }
    }

    fun moveBook(bookId: String, topicId: Long?) {
        viewModelScope.launch { repository.moveBookToTopic(bookId, topicId) }
    }

    fun renameTopic(topicId: Long, name: String) {
        viewModelScope.launch { repository.renameTopic(topicId, name) }
    }

    fun deleteTopic(topicId: Long) {
        viewModelScope.launch { repository.deleteTopic(topicId) }
    }

    fun hideBook(bookId: String) {
        viewModelScope.launch { repository.setBookHidden(bookId, true) }
    }

    /** Backs a hide out again from the snackbar action. */
    fun unhideBook(bookId: String) {
        viewModelScope.launch { repository.setBookHidden(bookId, false) }
    }

    fun selectTag(tagId: Long?) {
        _selectedTagId.value = tagId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setShelfSort(sort: ShelfSort) {
        viewModelScope.launch { settingsRepository.setShelfSort(sort) }
    }

    /**
     * Drop of a dragged book onto another book: insert before it.
     *
     * Under an automatic sort there is no "before" to insert into — rewriting
     * `manualOrder` would change nothing on screen and quietly scramble the
     * hand-made arrangement waiting under it — so the drop only moves the book
     * to the target's shelf.
     */
    fun dropOnBook(draggedId: String, target: Book) {
        if (draggedId == target.id) return
        viewModelScope.launch {
            // Read the stored value, not the cached StateFlow: the cache holds
            // the default until something is collecting it.
            if (settingsRepository.shelfSort.first() == ShelfSort.MANUAL) {
                repository.reorderBook(draggedId, target.id)
            } else {
                repository.moveBookToTopic(draggedId, target.topicId)
            }
        }
    }

    /** Loads the book's current tag ids for the tag dialog. */
    suspend fun tagIdsForBook(bookId: String): Set<Long> = repository.tagIdsForBook(bookId)

    fun saveTags(bookId: String, tagIds: Set<Long>, newTagNames: List<String>) {
        viewModelScope.launch { repository.setTagsForBook(bookId, tagIds, newTagNames) }
    }

    private fun WorkInfo?.toUiState(): ScanUiState {
        if (this == null) return ScanUiState()
        return when (state) {
            WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> ScanUiState(
                running = true,
                processed = progress.getInt(ScanWorker.KEY_PROCESSED, 0),
                total = progress.getInt(ScanWorker.KEY_TOTAL, 0)
            )
            WorkInfo.State.SUCCEEDED -> ScanUiState(
                running = false,
                lastScanned = outputData.getInt(ScanWorker.KEY_SCANNED, 0),
                lastAdded = outputData.getInt(ScanWorker.KEY_ADDED, 0),
                lastRelocated = outputData.getInt(ScanWorker.KEY_RELOCATED, 0),
                lastMissing = outputData.getInt(ScanWorker.KEY_MISSING, 0)
            )
            WorkInfo.State.FAILED -> ScanUiState(failed = true)
            else -> ScanUiState()
        }
    }

    private fun WorkInfo?.toCoverUiState(): CoverUiState {
        if (this == null) return CoverUiState()
        return when (state) {
            WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> CoverUiState(
                running = true,
                processed = progress.getInt(CoverWorker.KEY_PROCESSED, 0),
                total = progress.getInt(CoverWorker.KEY_TOTAL, 0)
            )
            else -> CoverUiState()
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 200L
    }
}
