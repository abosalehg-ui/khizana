package com.abosalehg.khizana.fakes

import com.abosalehg.khizana.data.db.BookDao
import com.abosalehg.khizana.data.db.BookEntity
import com.abosalehg.khizana.data.db.BookTagCrossRef
import com.abosalehg.khizana.data.db.BookmarkDao
import com.abosalehg.khizana.data.db.BookmarkEntity
import com.abosalehg.khizana.data.db.ExcludedFolderDao
import com.abosalehg.khizana.data.db.ExcludedFolderEntity
import com.abosalehg.khizana.data.db.TagDao
import com.abosalehg.khizana.data.db.TagEntity
import com.abosalehg.khizana.data.db.TopicDao
import com.abosalehg.khizana.data.db.TopicEntity
import com.abosalehg.khizana.data.db.TransactionRunner
import com.abosalehg.khizana.data.scanner.LibraryScanner
import com.abosalehg.khizana.data.scanner.ScannedFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory DAO doubles so the rescan and restore contracts — the two pieces
 * of logic the whole library depends on — can be tested on the JVM without an
 * emulator. Room's own SQL is covered separately by `KhizanaDatabaseTest`.
 */
class FakeBookDao : BookDao {

    private val rows = MutableStateFlow<Map<String, BookEntity>>(emptyMap())

    fun snapshot(): List<BookEntity> = rows.value.values.toList()

    private fun mutate(id: String, transform: (BookEntity) -> BookEntity) {
        rows.value = rows.value.toMutableMap().also { map ->
            map[id]?.let { map[id] = transform(it) }
        }
    }

    override suspend fun getAll(): List<BookEntity> = snapshot()

    override suspend fun getById(id: String): BookEntity? = rows.value[id]

    override fun observeVisible(): Flow<List<BookEntity>> = rows.map { map ->
        map.values.filter { !it.isHidden && it.status != "MISSING" }.sortedByDescending { it.addedAt }
    }

    override suspend fun booksOnShelf(topicId: Long?): List<BookEntity> =
        rows.value.values.filter {
            !it.isHidden && it.status != "MISSING" && it.topicId == topicId
        }

    override suspend fun insert(book: BookEntity) {
        check(book.id !in rows.value) { "duplicate insert for ${book.id}" }
        rows.value = rows.value + (book.id to book)
    }

    override suspend fun updateLocation(
        id: String,
        path: String,
        fileName: String,
        fileSize: Long,
        status: String
    ) = mutate(id) {
        it.copy(path = path, fileName = fileName, fileSize = fileSize, status = status)
    }

    override suspend fun markMissing(ids: List<String>) {
        ids.forEach { id -> mutate(id) { it.copy(status = "MISSING") } }
    }

    override suspend fun getNeedingCovers(): List<BookEntity> =
        rows.value.values.filter { it.status == "OK" && it.coverPath == null && !it.coverFailed }

    override suspend fun setCover(id: String, coverPath: String, pageCount: Int) = mutate(id) {
        it.copy(coverPath = coverPath, pageCount = pageCount, coverFailed = false)
    }

    override suspend fun setCoverFailed(id: String) = mutate(id) { it.copy(coverFailed = true) }

    override suspend fun setStatus(id: String, status: String) = mutate(id) {
        it.copy(status = status)
    }

    override suspend fun updateTopic(id: String, topicId: Long?) = mutate(id) {
        it.copy(topicId = topicId)
    }

    override suspend fun saveReadingPosition(
        id: String,
        locator: String,
        progress: Float,
        lastReadAt: Long
    ) = mutate(id) { it.copy(locator = locator, progress = progress, lastReadAt = lastReadAt) }

    override suspend fun updatePageCount(id: String, pageCount: Int) = mutate(id) {
        it.copy(pageCount = pageCount)
    }

    override suspend fun setHidden(id: String, hidden: Boolean) = mutate(id) {
        it.copy(isHidden = hidden)
    }

    override suspend fun updateManualOrder(id: String, order: Int) = mutate(id) {
        it.copy(manualOrder = order)
    }

    override fun observeHidden(): Flow<List<BookEntity>> = rows.map { map ->
        map.values.filter { it.isHidden && it.status != "MISSING" }.sortedBy { it.title }
    }

    override suspend fun clearTopic(topicId: Long) {
        rows.value.values.filter { it.topicId == topicId }
            .forEach { book -> mutate(book.id) { it.copy(topicId = null) } }
    }

    override suspend fun deleteById(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun deleteTagRefsForBook(bookId: String) = Unit

    override suspend fun applyRestoredMetadata(
        id: String,
        topicId: Long?,
        locator: String?,
        progress: Float,
        isHidden: Boolean,
        readingDirection: String,
        lastReadAt: Long?
    ) = mutate(id) {
        it.copy(
            topicId = topicId,
            locator = locator,
            progress = progress,
            isHidden = isHidden,
            readingDirection = readingDirection,
            lastReadAt = lastReadAt
        )
    }
}

class FakeTopicDao : TopicDao {
    private val rows = MutableStateFlow<List<TopicEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<TopicEntity>> = rows

    override suspend fun insert(topic: TopicEntity): Long {
        val id = nextId++
        rows.value = rows.value + topic.copy(id = id)
        return id
    }

    override suspend fun findByName(name: String): TopicEntity? =
        rows.value.firstOrNull { it.name == name }

    override suspend fun rename(id: Long, name: String) {
        rows.value = rows.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun delete(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}

class FakeTagDao : TagDao {
    private val tags = MutableStateFlow<List<TagEntity>>(emptyList())
    private val refs = MutableStateFlow<List<BookTagCrossRef>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<TagEntity>> = tags

    override suspend fun findByName(name: String): TagEntity? =
        tags.value.firstOrNull { it.name == name }

    override suspend fun insert(tag: TagEntity): Long {
        if (tags.value.any { it.name == tag.name }) return 0L // OnConflict IGNORE
        val id = nextId++
        tags.value = tags.value + tag.copy(id = id)
        return id
    }

    override fun observeAllRefs(): Flow<List<BookTagCrossRef>> = refs

    override suspend fun tagIdsForBook(bookId: String): List<Long> =
        refs.value.filter { it.bookId == bookId }.map { it.tagId }

    override suspend fun addRef(ref: BookTagCrossRef) {
        if (refs.value.none { it.bookId == ref.bookId && it.tagId == ref.tagId }) {
            refs.value = refs.value + ref
        }
    }

    override suspend fun removeRef(bookId: String, tagId: Long) {
        refs.value = refs.value.filterNot { it.bookId == bookId && it.tagId == tagId }
    }

    override suspend fun pruneUnused() {
        val used = refs.value.mapTo(HashSet()) { it.tagId }
        tags.value = tags.value.filter { it.id in used }
    }
}

class FakeBookmarkDao : BookmarkDao {
    private val rows = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    private var nextId = 1L

    fun snapshot(): List<BookmarkEntity> = rows.value

    override fun observeForBook(bookId: String): Flow<List<BookmarkEntity>> = rows.map { list ->
        list.filter { it.bookId == bookId }.sortedBy { it.page }
    }

    override suspend fun findAt(bookId: String, page: Int): BookmarkEntity? =
        rows.value.firstOrNull { it.bookId == bookId && it.page == page }

    override suspend fun getAll(): List<BookmarkEntity> =
        rows.value.sortedWith(compareBy({ it.bookId }, { it.page }))

    override suspend fun insert(bookmark: BookmarkEntity): Long {
        val id = nextId++
        rows.value = rows.value + bookmark.copy(id = id)
        return id
    }

    override suspend fun updateNote(id: Long, note: String?) {
        rows.value = rows.value.map { if (it.id == id) it.copy(note = note) else it }
    }

    override suspend fun deleteById(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun deleteForBook(bookId: String) {
        rows.value = rows.value.filterNot { it.bookId == bookId }
    }
}

class FakeExcludedFolderDao : ExcludedFolderDao {
    private val rows = MutableStateFlow<List<ExcludedFolderEntity>>(emptyList())

    override suspend fun getAllPaths(): List<String> = rows.value.map { it.path }

    override fun observeAll(): Flow<List<ExcludedFolderEntity>> = rows

    override suspend fun insert(folder: ExcludedFolderEntity) {
        if (rows.value.none { it.path == folder.path }) rows.value = rows.value + folder
    }

    override suspend fun delete(path: String) {
        rows.value = rows.value.filterNot { it.path == path }
    }
}

/** Runs the block straight through — Room's transaction has no JVM analogue. */
object PassThroughTransactionRunner : TransactionRunner {
    override suspend fun <R> invoke(block: suspend () -> R): R = block()
}

/** Returns whatever files the test hands it, honouring nothing else. */
class FakeLibraryScanner(var files: List<ScannedFile> = emptyList()) : LibraryScanner {
    var lastDeep: Boolean? = null
    var lastExcluded: List<String>? = null

    override fun scan(deep: Boolean, excludedFolders: List<String>): List<ScannedFile> {
        lastDeep = deep
        lastExcluded = excludedFolders
        return files
    }
}
