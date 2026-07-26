package com.abosalehg.khizana.ui.shelf

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.abosalehg.khizana.data.scanner.StoragePermission
import com.abosalehg.khizana.domain.repo.LibraryRepository
import com.abosalehg.khizana.work.ScanWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val lastMissing: Int = 0
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LibraryRepository
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    /** Selected tag filter; null shows everything. */
    private val _selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagId: StateFlow<Long?> = _selectedTagId

    val tags = repository.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val shelves: StateFlow<List<Shelf>> =
        combine(
            repository.topics,
            repository.visibleBooks,
            repository.bookTagRefs,
            _selectedTagId
        ) { topics, books, refs, tagId ->
            val filterIds = tagId?.let { id ->
                refs.filter { it.tagId == id }.mapTo(HashSet()) { it.bookId }
            }
            filterShelvesByBookIds(buildShelves(topics, books), filterIds)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _permissionGranted = MutableStateFlow(StoragePermission.isGranted(context))
    val permissionGranted: StateFlow<Boolean> = _permissionGranted

    val scanState: StateFlow<ScanUiState> = workManager
        .getWorkInfosForUniqueWorkFlow(ScanWorker.UNIQUE_NAME)
        .map { infos -> infos.firstOrNull().toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanUiState())

    /** Called from the UI on resume — the grant happens in system settings. */
    fun refreshPermission() {
        _permissionGranted.value = StoragePermission.isGranted(context)
    }

    fun startScan() {
        workManager.enqueueUniqueWork(
            ScanWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ScanWorker>().build()
        )
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

    fun selectTag(tagId: Long?) {
        _selectedTagId.value = tagId
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
            else -> ScanUiState()
        }
    }
}
