package com.abosalehg.khizana

import com.abosalehg.khizana.data.backup.BackupBook
import com.abosalehg.khizana.data.backup.BackupBookmark
import com.abosalehg.khizana.data.backup.BackupData
import com.abosalehg.khizana.data.backup.BackupRef
import com.abosalehg.khizana.data.backup.BackupSerializer
import com.abosalehg.khizana.data.backup.BackupTag
import com.abosalehg.khizana.data.backup.BackupTopic
import com.abosalehg.khizana.data.db.BookEntity
import com.abosalehg.khizana.data.db.BookTagCrossRef
import com.abosalehg.khizana.data.db.BookmarkEntity
import com.abosalehg.khizana.data.db.ExcludedFolderEntity
import com.abosalehg.khizana.data.db.TagEntity
import com.abosalehg.khizana.data.db.TopicEntity
import com.abosalehg.khizana.data.db.getOrCreate
import com.abosalehg.khizana.domain.model.BookStatus
import com.abosalehg.khizana.fakes.FakeBookDao
import com.abosalehg.khizana.fakes.FakeBookmarkDao
import com.abosalehg.khizana.fakes.FakeExcludedFolderDao
import com.abosalehg.khizana.fakes.FakeTagDao
import com.abosalehg.khizana.fakes.FakeTopicDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Restore is merge-based and fingerprint-keyed. `BackupManager` itself needs a
 * ContentResolver, so the merge logic is exercised through the same DAO calls
 * it makes — the part that can silently corrupt a library.
 */
class BackupRestoreTest {

    private val idKnown = "a".repeat(64)
    private val idUnknown = "b".repeat(64)

    private lateinit var bookDao: FakeBookDao
    private lateinit var topicDao: FakeTopicDao
    private lateinit var tagDao: FakeTagDao
    private lateinit var excludedDao: FakeExcludedFolderDao
    private lateinit var bookmarkDao: FakeBookmarkDao

    @Before
    fun setUp() {
        bookDao = FakeBookDao()
        topicDao = FakeTopicDao()
        tagDao = FakeTagDao()
        excludedDao = FakeExcludedFolderDao()
        bookmarkDao = FakeBookmarkDao()
    }

    /** Mirrors BackupManager.applyRestore without its ContentResolver plumbing. */
    private suspend fun applyRestore(data: BackupData) {
        val topicIdMap = HashMap<Long, Long>()
        data.topics.forEach { topic ->
            val existing = topicDao.findByName(topic.name)
            topicIdMap[topic.id] = existing?.id
                ?: topicDao.insert(TopicEntity(name = topic.name, order = topic.order))
        }
        val tagIdMap = HashMap<Long, Long>()
        data.tags.forEach { tag -> tagDao.getOrCreate(tag.name)?.let { tagIdMap[tag.id] = it } }

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
                bookDao.insert(
                    BookEntity(
                        id = book.id,
                        path = "",
                        fileName = book.fileName,
                        format = book.format,
                        title = book.title,
                        topicId = mappedTopic,
                        locator = book.locator,
                        progress = book.progress,
                        status = BookStatus.MISSING.name,
                        isHidden = book.isHidden,
                        manualOrder = book.manualOrder
                    )
                )
            }
        }
        data.bookTags.forEach { ref ->
            tagIdMap[ref.tagId]?.let {
                tagDao.addRef(BookTagCrossRef(ref.bookId, it))
            }
        }
        data.excludedFolders.forEach {
            excludedDao.insert(ExcludedFolderEntity(it))
        }
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
        tagDao.pruneUnused()
    }

    private fun backupBook(
        id: String,
        topicId: Long? = null,
        locator: String? = null,
        progress: Float = 0f,
        isHidden: Boolean = false
    ) = BackupBook(
        id = id,
        fileName = "$id.pdf",
        format = "PDF",
        title = "title-$id",
        author = null,
        topicId = topicId,
        pageCount = 0,
        locator = locator,
        progress = progress,
        readingDirection = "AUTO",
        isHidden = isHidden,
        manualOrder = 0,
        fileSize = 0,
        addedAt = 0,
        lastReadAt = null
    )

    @Test
    fun aBookThisDeviceDoesNotHaveIsInsertedAsMissingWithNoPath() = runBlocking {
        applyRestore(
            BackupData(
                books = listOf(backupBook(idUnknown, locator = "12", progress = 0.4f)),
                topics = emptyList(),
                tags = emptyList(),
                bookTags = emptyList(),
                excludedFolders = emptyList()
            )
        )

        val book = bookDao.snapshot().single()
        assertEquals(BookStatus.MISSING.name, book.status)
        assertEquals("", book.path)
        assertEquals("12", book.locator)
        assertEquals(0.4f, book.progress, 0f)
    }

    @Test
    fun restoringOverwritesTheCurrentReadingPositionOfAKnownBook() = runBlocking {
        bookDao.insert(
            BookEntity(id = idKnown, path = "/p/a.pdf", fileName = "a.pdf", format = "PDF", title = "a")
        )
        bookDao.saveReadingPosition(idKnown, "300", 0.9f, 999L)

        applyRestore(
            BackupData(
                books = listOf(backupBook(idKnown, locator = "10", progress = 0.1f)),
                topics = emptyList(),
                tags = emptyList(),
                bookTags = emptyList(),
                excludedFolders = emptyList()
            )
        )

        // Documented, warned-about behaviour: an older backup rewinds progress.
        val book = bookDao.snapshot().single()
        assertEquals("10", book.locator)
        assertEquals(0.1f, book.progress, 0f)
        assertEquals("the file itself is untouched", "/p/a.pdf", book.path)
    }

    @Test
    fun topicsAreMergedByNameRatherThanDuplicated() = runBlocking {
        val existingId = topicDao.insert(TopicEntity(name = "تاريخ"))

        applyRestore(
            BackupData(
                books = listOf(backupBook(idUnknown, topicId = 99L)),
                topics = listOf(BackupTopic(99L, "تاريخ", 0)),
                tags = emptyList(),
                bookTags = emptyList(),
                excludedFolders = emptyList()
            )
        )

        assertEquals(1, topicDao.observeAll().first().size)
        assertEquals(existingId, bookDao.snapshot().single().topicId)
    }

    @Test
    fun tagIdsAreRemappedOntoTheLocalTagRows() = runBlocking {
        tagDao.insert(TagEntity(name = "مفضلة"))

        applyRestore(
            BackupData(
                books = listOf(backupBook(idUnknown)),
                topics = emptyList(),
                tags = listOf(BackupTag(500L, "مفضلة")),
                bookTags = listOf(BackupRef(idUnknown, 500L)),
                excludedFolders = emptyList()
            )
        )

        val localTag = tagDao.findByName("مفضلة")
        assertNotNull(localTag)
        assertEquals(listOf(localTag!!.id), tagDao.tagIdsForBook(idUnknown))
    }

    @Test
    fun excludedFoldersFromTheFileAreAdded() = runBlocking {
        applyRestore(
            BackupData(
                books = emptyList(),
                topics = emptyList(),
                tags = emptyList(),
                bookTags = emptyList(),
                excludedFolders = listOf("/storage/emulated/0/Recordings")
            )
        )
        assertTrue("/storage/emulated/0/Recordings" in excludedDao.getAllPaths())
    }

    @Test
    fun aBackupWrittenByThisBuildReadsBackIdentically() {
        val data = BackupData(
            books = listOf(backupBook(idKnown, topicId = 1L, locator = "5", progress = 0.5f)),
            topics = listOf(BackupTopic(1L, "رف", 0)),
            tags = listOf(BackupTag(1L, "وسم")),
            bookTags = listOf(BackupRef(idKnown, 1L)),
            excludedFolders = listOf("/a/b"),
            bookmarks = listOf(BackupBookmark(idKnown, 12, "ملاحظة", 99L))
        )
        assertEquals(data, BackupSerializer.fromJson(BackupSerializer.toJson(data)))
    }

    @Test
    fun bookmarksAreRestoredWithTheirNotes() = runBlocking {
        applyRestore(
            BackupData(
                books = listOf(backupBook(idUnknown)),
                topics = emptyList(),
                tags = emptyList(),
                bookTags = emptyList(),
                excludedFolders = emptyList(),
                bookmarks = listOf(BackupBookmark(idUnknown, 41, "موضع مهم", 7L))
            )
        )

        val bookmark = bookmarkDao.snapshot().single()
        assertEquals(41, bookmark.page)
        assertEquals("موضع مهم", bookmark.note)
        assertEquals(7L, bookmark.createdAt)
    }

    @Test
    fun restoringTwiceEditsTheNoteInsteadOfDuplicatingTheBookmark() = runBlocking {
        val first = BackupData(
            books = emptyList(),
            topics = emptyList(),
            tags = emptyList(),
            bookTags = emptyList(),
            excludedFolders = emptyList(),
            bookmarks = listOf(BackupBookmark(idKnown, 3, "قديم", 1L))
        )
        applyRestore(first)
        applyRestore(
            first.copy(bookmarks = listOf(BackupBookmark(idKnown, 3, "جديد", 2L)))
        )

        val bookmark = bookmarkDao.snapshot().single()
        assertEquals("جديد", bookmark.note)
        // The row is the same one: only its note moved.
        assertEquals(1L, bookmark.createdAt)
    }
}
