package com.abosalehg.khizana.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.abosalehg.khizana.data.db.BookDao
import com.abosalehg.khizana.data.db.BookEntity
import com.abosalehg.khizana.data.db.BookTagCrossRef
import com.abosalehg.khizana.data.db.BookmarkDao
import com.abosalehg.khizana.data.db.BookmarkEntity
import com.abosalehg.khizana.data.db.ExcludedFolderDao
import com.abosalehg.khizana.data.db.ExcludedFolderEntity
import com.abosalehg.khizana.data.db.TagDao
import com.abosalehg.khizana.data.db.TopicDao
import com.abosalehg.khizana.data.db.TopicEntity
import com.abosalehg.khizana.data.db.TransactionRunner
import com.abosalehg.khizana.data.db.getOrCreate
import com.abosalehg.khizana.domain.model.BookStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports/imports all user-authored data as JSON. Restore is merge-based and
 * fingerprint-keyed: metadata re-attaches to matching books; books whose files
 * aren't on this device yet are inserted as MISSING and come alive on the
 * first rescan that finds their fingerprint.
 *
 * Restoring **overwrites** the reading position, hidden flag and shelf of every
 * book the file knows about — the Settings screen warns about this before the
 * file picker opens.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val topicDao: TopicDao,
    private val tagDao: TagDao,
    private val excludedFolderDao: ExcludedFolderDao,
    private val bookmarkDao: BookmarkDao,
    private val transaction: TransactionRunner
) {

    suspend fun exportTo(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val data = BackupData(
                books = bookDao.getAll().map { it.toBackup() },
                topics = topicDao.observeAll().first()
                    .map { BackupTopic(it.id, it.name, it.order) },
                tags = tagDao.observeAll().first().map { BackupTag(it.id, it.name) },
                bookTags = tagDao.observeAllRefs().first()
                    .map { BackupRef(it.bookId, it.tagId) },
                excludedFolders = excludedFolderDao.getAllPaths(),
                bookmarks = bookmarkDao.getAll()
                    .map { BackupBookmark(it.bookId, it.page, it.note, it.createdAt) }
            )
            val json = BackupSerializer.toJson(data)
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: run {
                Log.w(TAG, "Export target could not be opened for writing")
                return@withContext false
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Export failed", e)
            false
        }
    }

    suspend fun importFrom(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { readCapped(it) }
                ?: run {
                    Log.w(TAG, "Backup file could not be opened for reading")
                    return@withContext false
                }
            val data = BackupSerializer.fromJson(json)
            applyRestore(data)
            true
        } catch (e: BackupFormatException) {
            Log.w(TAG, "Rejected backup file: ${e.message}")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Restore failed", e)
            false
        }
    }

    /**
     * Reads at most [BackupSerializer.MAX_BACKUP_BYTES]. A plain `readBytes()`
     * on a picked file lets any multi-gigabyte document take the process down
     * with an OutOfMemoryError, which `catch (Exception)` would not even catch.
     */
    private fun readCapped(input: InputStream): String {
        val buffer = ByteArray(64 * 1024)
        val collected = ByteArrayOutputStream()
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > BackupSerializer.MAX_BACKUP_BYTES) {
                throw BackupFormatException("Backup file exceeds the size limit")
            }
            collected.write(buffer, 0, read)
        }
        return String(collected.toByteArray(), Charsets.UTF_8)
    }

    /** One transaction: a restore either lands whole or not at all. */
    private suspend fun applyRestore(data: BackupData) = transaction {
        // Topics and tags are matched by name; old ids are remapped.
        val topicIdMap = HashMap<Long, Long>()
        data.topics.forEach { topic ->
            val existing = topicDao.findByName(topic.name)
            topicIdMap[topic.id] = existing?.id
                ?: topicDao.insert(TopicEntity(name = topic.name, order = topic.order))
        }
        val tagIdMap = HashMap<Long, Long>()
        data.tags.forEach { tag ->
            tagDao.getOrCreate(tag.name)?.let { tagIdMap[tag.id] = it }
        }

        data.books.forEach { book ->
            val mappedTopic = book.topicId?.let { topicIdMap[it] }
            val existing = bookDao.getById(book.id)
            if (existing != null) {
                bookDao.applyRestoredMetadata(
                    id = book.id,
                    topicId = mappedTopic,
                    locator = book.locator,
                    progress = book.progress,
                    isHidden = book.isHidden,
                    readingDirection = book.readingDirection,
                    lastReadAt = book.lastReadAt
                )
            } else {
                // Not on this device (yet): keep as MISSING until a scan
                // finds the same fingerprint and fills in the real path.
                bookDao.insert(
                    BookEntity(
                        id = book.id,
                        path = "",
                        fileName = book.fileName,
                        format = book.format,
                        title = book.title,
                        author = book.author,
                        topicId = mappedTopic,
                        pageCount = book.pageCount,
                        locator = book.locator,
                        progress = book.progress,
                        readingDirection = book.readingDirection,
                        status = BookStatus.MISSING.name,
                        isHidden = book.isHidden,
                        manualOrder = book.manualOrder,
                        fileSize = book.fileSize,
                        addedAt = book.addedAt,
                        lastReadAt = book.lastReadAt
                    )
                )
            }
        }

        data.bookTags.forEach { ref ->
            tagIdMap[ref.tagId]?.let { tagDao.addRef(BookTagCrossRef(ref.bookId, it)) }
        }

        // One bookmark per page, here as everywhere: restoring twice, or over a
        // device that already bookmarked the page, updates the note instead of
        // stacking a duplicate.
        data.bookmarks.forEach { bookmark ->
            val existing = bookmarkDao.findAt(bookmark.bookId, bookmark.page)
            if (existing == null) {
                bookmarkDao.insert(
                    BookmarkEntity(
                        bookId = bookmark.bookId,
                        page = bookmark.page,
                        note = bookmark.note,
                        createdAt = bookmark.createdAt
                    )
                )
            } else if (existing.note != bookmark.note) {
                bookmarkDao.updateNote(existing.id, bookmark.note)
            }
        }

        data.excludedFolders.forEach { excludedFolderDao.insert(ExcludedFolderEntity(it)) }
        tagDao.pruneUnused()
    }

    private fun BookEntity.toBackup() = BackupBook(
        id = id,
        fileName = fileName,
        format = format,
        title = title,
        author = author,
        topicId = topicId,
        pageCount = pageCount,
        locator = locator,
        progress = progress,
        readingDirection = readingDirection,
        isHidden = isHidden,
        manualOrder = manualOrder,
        fileSize = fileSize,
        addedAt = addedAt,
        lastReadAt = lastReadAt
    )

    private companion object {
        const val TAG = "BackupManager"
    }
}
