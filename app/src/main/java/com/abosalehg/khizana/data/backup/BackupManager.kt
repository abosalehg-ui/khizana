package com.abosalehg.khizana.data.backup

import android.content.Context
import android.net.Uri
import com.abosalehg.khizana.data.db.BookDao
import com.abosalehg.khizana.data.db.BookEntity
import com.abosalehg.khizana.data.db.BookTagCrossRef
import com.abosalehg.khizana.data.db.ExcludedFolderDao
import com.abosalehg.khizana.data.db.ExcludedFolderEntity
import com.abosalehg.khizana.data.db.TagDao
import com.abosalehg.khizana.data.db.TagEntity
import com.abosalehg.khizana.data.db.TopicDao
import com.abosalehg.khizana.data.db.TopicEntity
import com.abosalehg.khizana.domain.model.BookStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports/imports all user-authored data as JSON. Restore is merge-based and
 * fingerprint-keyed: metadata re-attaches to matching books; books whose files
 * aren't on this device yet are inserted as MISSING and come alive on the
 * first rescan that finds their fingerprint.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val topicDao: TopicDao,
    private val tagDao: TagDao,
    private val excludedFolderDao: ExcludedFolderDao
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
                excludedFolders = excludedFolderDao.getAllPaths()
            )
            val json = BackupSerializer.toJson(data)
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: return@withContext false
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun importFrom(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: return@withContext false
            val data = BackupSerializer.fromJson(json)
            applyRestore(data)
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun applyRestore(data: BackupData) {
        // Topics and tags are matched by name; old ids are remapped.
        val topicIdMap = HashMap<Long, Long>()
        data.topics.forEach { topic ->
            val existing = topicDao.findByName(topic.name)
            topicIdMap[topic.id] = existing?.id
                ?: topicDao.insert(TopicEntity(name = topic.name, order = topic.order))
        }
        val tagIdMap = HashMap<Long, Long>()
        data.tags.forEach { tag ->
            val existing = tagDao.findByName(tag.name)
            val id = existing?.id ?: tagDao.insert(TagEntity(name = tag.name))
                .takeIf { it > 0 } ?: tagDao.findByName(tag.name)?.id
            id?.let { tagIdMap[tag.id] = it }
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
}
