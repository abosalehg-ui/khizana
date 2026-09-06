package com.abosalehg.khizana.data.repo

import android.util.Log
import com.abosalehg.khizana.data.covers.CoverStore
import com.abosalehg.khizana.data.db.BookDao
import com.abosalehg.khizana.data.db.BookEntity
import com.abosalehg.khizana.data.db.BookTagCrossRef
import com.abosalehg.khizana.data.db.BookmarkDao
import com.abosalehg.khizana.data.db.ExcludedFolderDao
import com.abosalehg.khizana.data.db.ExcludedFolderEntity
import com.abosalehg.khizana.data.db.TagDao
import com.abosalehg.khizana.data.db.TopicDao
import com.abosalehg.khizana.data.db.TopicEntity
import com.abosalehg.khizana.data.db.TransactionRunner
import com.abosalehg.khizana.data.db.getOrCreate
import com.abosalehg.khizana.data.scanner.FileFingerprint
import com.abosalehg.khizana.data.scanner.LibraryScanner
import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.BookFormat
import com.abosalehg.khizana.domain.model.BookStatus
import com.abosalehg.khizana.domain.model.ReadingDirection
import com.abosalehg.khizana.domain.model.Tag
import com.abosalehg.khizana.domain.model.Topic
import com.abosalehg.khizana.util.manualThenNaturalComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ScanReport(
    val scanned: Int,
    val added: Int,
    val relocated: Int,
    val missing: Int
)

@Singleton
class LibraryRepository @Inject constructor(
    private val bookDao: BookDao,
    private val topicDao: TopicDao,
    private val tagDao: TagDao,
    private val bookmarkDao: BookmarkDao,
    private val excludedFolderDao: ExcludedFolderDao,
    private val scanner: LibraryScanner,
    private val coverStore: CoverStore,
    private val transaction: TransactionRunner
) {

    /** Books the shelves can show (not hidden, present on disk). */
    val visibleBooks: Flow<List<Book>> =
        bookDao.observeVisible().map { entities -> entities.map { it.toDomain() } }

    val topics: Flow<List<Topic>> =
        topicDao.observeAll().map { entities ->
            entities.map { Topic(id = it.id, name = it.name, order = it.order) }
        }

    suspend fun addTopic(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) topicDao.insert(TopicEntity(name = trimmed))
    }

    suspend fun moveBookToTopic(bookId: String, topicId: Long?) =
        bookDao.updateTopic(bookId, topicId)

    /**
     * Drops [draggedId] onto [targetId]: the dragged book is inserted right
     * before the target within the target's shelf (moving shelves if
     * needed), and the whole shelf's manualOrder is rewritten 1..n.
     *
     * Only the target shelf is read, and the rewrite is one transaction, so an
     * interrupted drop can never leave a partially renumbered shelf.
     */
    suspend fun reorderBook(draggedId: String, targetId: String) {
        if (draggedId == targetId) return
        val target = bookDao.getById(targetId) ?: return
        val dragged = bookDao.getById(draggedId) ?: return
        val ordering = manualThenNaturalComparator<BookEntity>({ it.manualOrder }, { it.title })
        val shelfBooks = bookDao.booksOnShelf(target.topicId)
            .filter { it.id != draggedId }
            .sortedWith(ordering)
            .toMutableList()
        val insertAt = shelfBooks.indexOfFirst { it.id == targetId }.coerceAtLeast(0)
        shelfBooks.add(insertAt, dragged)
        transaction {
            if (dragged.topicId != target.topicId) bookDao.updateTopic(draggedId, target.topicId)
            shelfBooks.forEachIndexed { index, book ->
                if (book.manualOrder != index + 1) bookDao.updateManualOrder(book.id, index + 1)
            }
        }
    }

    suspend fun renameTopic(topicId: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) topicDao.rename(topicId, trimmed)
    }

    /** Deleting a shelf never deletes books — they return to the New shelf. */
    suspend fun deleteTopic(topicId: Long) = transaction {
        bookDao.clearTopic(topicId)
        topicDao.delete(topicId)
    }

    // ---- Hidden books ----

    val hiddenBooks: Flow<List<Book>> =
        bookDao.observeHidden().map { entities -> entities.map { it.toDomain() } }

    suspend fun setBookHidden(bookId: String, hidden: Boolean) =
        bookDao.setHidden(bookId, hidden)

    // ---- Excluded folders ----

    val excludedFolders: Flow<List<String>> =
        excludedFolderDao.observeAll().map { list -> list.map { it.path } }

    suspend fun addExcludedFolder(path: String) =
        excludedFolderDao.insert(ExcludedFolderEntity(path))

    suspend fun removeExcludedFolder(path: String) = excludedFolderDao.delete(path)

    // ---- Permanent deletion (explicit user action only) ----

    /**
     * Deletes the file from disk and the book's row + tag links. Returns
     * false (and keeps everything) if the file exists but can't be deleted.
     *
     * The file goes first on purpose: a filesystem delete cannot be rolled
     * back, so if it fails we still hold every row. The database cleanup that
     * follows is one transaction — worst case a crash between the two leaves a
     * row whose file is gone, which the next rescan marks MISSING.
     */
    suspend fun deleteBookPermanently(book: Book): Boolean = withContext(Dispatchers.IO) {
        val file = File(book.path)
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Refusing to drop ${book.id}: its file could not be deleted")
            return@withContext false
        }
        coverStore.delete(book.id)
        transaction {
            bookDao.deleteTagRefsForBook(book.id)
            // Bookmarks are not foreign-keyed to the book, so nothing removes
            // them for us — orphan notes would outlive the book otherwise.
            bookmarkDao.deleteForBook(book.id)
            bookDao.deleteById(book.id)
            tagDao.pruneUnused()
        }
        true
    }

    // ---- Tags ----

    val tags: Flow<List<Tag>> =
        tagDao.observeAll().map { entities -> entities.map { Tag(it.id, it.name) } }

    /** All book↔tag links; the UI derives filters and per-book selections. */
    val bookTagRefs: Flow<List<BookTagCrossRef>> = tagDao.observeAllRefs()

    suspend fun tagIdsForBook(bookId: String): Set<Long> =
        tagDao.tagIdsForBook(bookId).toSet()

    /** Replaces a book's tag set; creates tags by name as needed, prunes orphans. */
    suspend fun setTagsForBook(bookId: String, tagIds: Set<Long>, newTagNames: List<String>) =
        transaction {
            val resolvedIds = tagIds.toMutableSet()
            newTagNames.forEach { name ->
                tagDao.getOrCreate(name)?.let { resolvedIds.add(it) }
            }
            val current = tagDao.tagIdsForBook(bookId).toSet()
            (resolvedIds - current).forEach { tagDao.addRef(BookTagCrossRef(bookId, it)) }
            (current - resolvedIds).forEach { tagDao.removeRef(bookId, it) }
            tagDao.pruneUnused()
        }

    /**
     * Full manual rescan. Never deletes rows: new fingerprints are inserted,
     * known fingerprints get their location refreshed (progress/hidden/topic
     * untouched), and vanished files are silently marked MISSING.
     *
     * Fingerprinting is I/O bound and stays outside any transaction; the
     * resulting writes are applied in batches so a kill mid-scan leaves whole
     * batches applied rather than a half-written row.
     *
     * A file whose path, size and mtime all match the row already holding that
     * path keeps its id without being re-read. That is the difference between
     * a rescan that opens every book in the library and one that opens only
     * what actually changed: fingerprinting reads 64 KB per file, so a library
     * of a few thousand books used to pull a couple of hundred megabytes off
     * storage on every scan, all of it to arrive back at the ids it already
     * had. A deep scan skips the fast path entirely, so there is always a way
     * to force the full computation.
     */
    suspend fun rescan(
        deep: Boolean = false,
        onProgress: suspend (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): ScanReport = withContext(Dispatchers.IO) {
        val excluded = excludedFolderDao.getAllPaths()
        val found = scanner.scan(deep, excluded)
        val knownRows = bookDao.getAll()
        val known = knownRows.associateBy { it.id }
        // Only rows that carry a real mtime can serve the fast path: 0 is what
        // every row written before schema v3 holds, and no file's mtime is 0.
        val knownByPath = if (deep) {
            emptyMap()
        } else {
            knownRows.filter { it.lastModified != 0L }.associateBy { it.path }
        }
        val seenIds = HashSet<String>(found.size)
        val inserts = ArrayList<BookEntity>()
        val relocations = ArrayList<Relocation>()

        found.forEachIndexed { index, scanned ->
            val file = scanned.file
            val fileSize = file.length()
            val modified = file.lastModified()
            val unchanged = knownByPath[file.absolutePath]?.takeIf { row ->
                row.fileSize == fileSize &&
                    row.lastModified == modified &&
                    row.status != BookStatus.MISSING.name
            }
            val id = unchanged?.id
                ?: runCatching { FileFingerprint.compute(file) }
                    .onFailure { Log.w(TAG, "Cannot fingerprint ${file.name}", it) }
                    .getOrNull()
            if (id != null && seenIds.add(id)) {
                val existing = known[id]
                if (existing == null) {
                    inserts += newEntity(id, scanned.format, file)
                } else {
                    val restoredStatus =
                        if (existing.status == BookStatus.MISSING.name) BookStatus.OK.name
                        else existing.status
                    val moved = existing.path != file.absolutePath ||
                        existing.status != restoredStatus ||
                        existing.fileSize != fileSize ||
                        existing.lastModified != modified
                    if (moved) {
                        relocations += Relocation(
                            id = id,
                            path = file.absolutePath,
                            fileName = file.name,
                            fileSize = fileSize,
                            status = restoredStatus,
                            lastModified = modified
                        )
                    }
                }
            }
            // One WorkManager progress write per file would mean one database
            // write per file; report on a stride instead.
            if (index % PROGRESS_UPDATE_EVERY == 0 || index == found.lastIndex) {
                onProgress(index + 1, found.size)
            }
        }

        val vanished = known.values
            .filter { it.id !in seenIds && it.status != BookStatus.MISSING.name }
            .map { it.id }

        inserts.chunked(WRITE_BATCH).forEach { batch ->
            transaction { batch.forEach { bookDao.insert(it) } }
        }
        relocations.chunked(WRITE_BATCH).forEach { batch ->
            transaction {
                batch.forEach {
                    bookDao.updateLocation(
                        it.id, it.path, it.fileName, it.fileSize, it.status, it.lastModified
                    )
                }
            }
        }
        vanished.chunked(WRITE_BATCH).forEach { batch ->
            transaction { bookDao.markMissing(batch) }
        }

        ScanReport(
            scanned = found.size,
            added = inserts.size,
            relocated = relocations.size,
            missing = vanished.size
        )
    }

    private fun newEntity(id: String, format: BookFormat, file: File): BookEntity = BookEntity(
        id = id,
        path = file.absolutePath,
        fileName = file.name,
        format = format.name,
        title = file.nameWithoutExtension,
        fileSize = file.length(),
        addedAt = System.currentTimeMillis(),
        lastModified = file.lastModified()
    )

    private data class Relocation(
        val id: String,
        val path: String,
        val fileName: String,
        val fileSize: Long,
        val status: String,
        val lastModified: Long
    )

    private companion object {
        const val TAG = "LibraryRepository"
        const val PROGRESS_UPDATE_EVERY = 50
        const val WRITE_BATCH = 500
    }
}

internal fun BookEntity.toDomain(): Book = Book(
    id = id,
    path = path,
    fileName = fileName,
    format = BookFormat.fromName(format) ?: BookFormat.PDF,
    title = title,
    author = author,
    topicId = topicId,
    pageCount = pageCount,
    locator = locator,
    progress = progress,
    readingDirection = ReadingDirection.fromName(readingDirection),
    coverPath = coverPath,
    coverFailed = coverFailed,
    status = BookStatus.fromName(status),
    isHidden = isHidden,
    manualOrder = manualOrder,
    fileSize = fileSize,
    addedAt = addedAt,
    lastReadAt = lastReadAt
)
