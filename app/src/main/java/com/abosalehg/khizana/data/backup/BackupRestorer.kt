package com.abosalehg.khizana.data.backup

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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies a parsed backup to the database. Merge-based and fingerprint-keyed:
 * metadata re-attaches to matching books, and books whose files aren't on this
 * device yet are inserted as MISSING to come alive on the first rescan that
 * finds their fingerprint.
 *
 * Split out of [BackupManager] because it needs no Context: the manager owns
 * the file — picking it, capping it, parsing it — and this owns what the
 * contents mean. That is also what makes it directly testable, and the reason
 * this class exists at all: the restore test used to re-implement these rules
 * beside the real ones, so a field missing from both looked like a pass.
 */
@Singleton
class BackupRestorer @Inject constructor(
    private val bookDao: BookDao,
    private val topicDao: TopicDao,
    private val tagDao: TagDao,
    private val excludedFolderDao: ExcludedFolderDao,
    private val bookmarkDao: BookmarkDao,
    private val transaction: TransactionRunner
) {

    /** One transaction: a restore either lands whole or not at all. */
    suspend fun apply(data: BackupData) = transaction { applyWithin(data) }

    /**
     * The restore itself, without opening a transaction of its own — so it can
     * also run inside one the caller already holds.
     */
    suspend fun applyWithin(data: BackupData) {
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
                    lastReadAt = book.lastReadAt,
                    manualOrder = book.manualOrder
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

        // Second belt behind the serializer's fingerprint check: a reference is
        // only written for a book that exists after the loop above ran, so the
        // table cannot collect rows pointing at nothing — rows that no cleanup
        // reaches, and that keep a tag alive with no book behind it.
        data.bookTags.forEach { ref ->
            val localTagId = tagIdMap[ref.tagId] ?: return@forEach
            if (bookDao.getById(ref.bookId) == null) return@forEach
            tagDao.addRef(BookTagCrossRef(ref.bookId, localTagId))
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
}
