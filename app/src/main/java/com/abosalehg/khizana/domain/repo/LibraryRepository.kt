package com.abosalehg.khizana.domain.repo

import com.abosalehg.khizana.data.db.BookDao
import com.abosalehg.khizana.data.db.BookEntity
import com.abosalehg.khizana.data.db.ExcludedFolderDao
import com.abosalehg.khizana.data.db.TopicDao
import com.abosalehg.khizana.data.db.TopicEntity
import com.abosalehg.khizana.data.scanner.FileFingerprint
import com.abosalehg.khizana.data.scanner.LibraryScanner
import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.BookFormat
import com.abosalehg.khizana.domain.model.BookStatus
import com.abosalehg.khizana.domain.model.ReadingDirection
import com.abosalehg.khizana.domain.model.Topic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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
    private val excludedFolderDao: ExcludedFolderDao,
    private val scanner: LibraryScanner
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
     * Full manual rescan. Never deletes rows: new fingerprints are inserted,
     * known fingerprints get their location refreshed (progress/hidden/topic
     * untouched), and vanished files are silently marked MISSING.
     */
    suspend fun rescan(
        deep: Boolean = false,
        onProgress: suspend (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): ScanReport = withContext(Dispatchers.IO) {
        val excluded = excludedFolderDao.getAllPaths()
        val found = scanner.scan(deep, excluded)
        val known = bookDao.getAll().associateBy { it.id }
        val seenIds = HashSet<String>(found.size)
        var added = 0
        var relocated = 0

        found.forEachIndexed { index, scanned ->
            val file = scanned.file
            val id = runCatching { FileFingerprint.compute(file) }.getOrNull()
            if (id != null && seenIds.add(id)) {
                val existing = known[id]
                if (existing == null) {
                    bookDao.insert(newEntity(id, scanned.format, file.absolutePath, file))
                    added++
                } else {
                    val restoredStatus =
                        if (existing.status == BookStatus.MISSING.name) BookStatus.OK.name
                        else existing.status
                    val moved = existing.path != file.absolutePath ||
                        existing.status != restoredStatus ||
                        existing.fileSize != file.length()
                    if (moved) {
                        bookDao.updateLocation(
                            id = id,
                            path = file.absolutePath,
                            fileName = file.name,
                            fileSize = file.length(),
                            status = restoredStatus
                        )
                        relocated++
                    }
                }
            }
            onProgress(index + 1, found.size)
        }

        val vanished = known.values
            .filter { it.id !in seenIds && it.status != BookStatus.MISSING.name }
            .map { it.id }
        vanished.chunked(500).forEach { bookDao.markMissing(it) }

        ScanReport(
            scanned = found.size,
            added = added,
            relocated = relocated,
            missing = vanished.size
        )
    }

    private fun newEntity(
        id: String,
        format: BookFormat,
        path: String,
        file: java.io.File
    ): BookEntity = BookEntity(
        id = id,
        path = path,
        fileName = file.name,
        format = format.name,
        title = file.nameWithoutExtension,
        fileSize = file.length(),
        addedAt = System.currentTimeMillis()
    )
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
