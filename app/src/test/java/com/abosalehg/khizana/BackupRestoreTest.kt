package com.abosalehg.khizana

import com.abosalehg.khizana.data.backup.BackupBook
import com.abosalehg.khizana.data.backup.BackupBookmark
import com.abosalehg.khizana.data.backup.BackupData
import com.abosalehg.khizana.data.backup.BackupRef
import com.abosalehg.khizana.data.backup.BackupRestorer
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
import com.abosalehg.khizana.fakes.PassThroughTransactionRunner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Restore is merge-based and fingerprint-keyed. `BackupManager` itself needs a
 * ContentResolver; `BackupRestorer` — which holds every rule that can silently
 * corrupt a library — does not, so these run the real thing.
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

    /**
     * The real restore path, not a copy of it: `BackupRestorer` needs no
     * Context, so the test drives production code. The previous version of
     * this test re-implemented the same rules alongside it, which is how a
     * field the restore silently dropped still looked like a passing contract.
     */
    private suspend fun applyRestore(data: BackupData) =
        BackupRestorer(
            bookDao = bookDao,
            topicDao = topicDao,
            tagDao = tagDao,
            excludedFolderDao = excludedDao,
            bookmarkDao = bookmarkDao,
            transaction = PassThroughTransactionRunner
        ).apply(data)

    private fun backupBook(
        id: String,
        topicId: Long? = null,
        locator: String? = null,
        progress: Float = 0f,
        isHidden: Boolean = false,
        manualOrder: Int = 0
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
        manualOrder = manualOrder,
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

    @Test
    fun restoringReinstatesTheHandMadeShelfOrderOfABookThisDeviceAlreadyHas() = runBlocking {
        // The file carries manualOrder and the insert path applied it, but the
        // update path did not — so restoring onto a device that already had the
        // book quietly flattened the order the reader had arranged by hand.
        bookDao.insert(
            BookEntity(
                id = idKnown,
                path = "/books/a.pdf",
                fileName = "a.pdf",
                format = "PDF",
                title = "a",
                manualOrder = 1
            )
        )

        applyRestore(
            BackupData(
                books = listOf(backupBook(idKnown, manualOrder = 9)),
                topics = emptyList(),
                tags = emptyList(),
                bookTags = emptyList(),
                excludedFolders = emptyList()
            )
        )

        assertEquals(9, bookDao.getById(idKnown)!!.manualOrder)
    }

    @Test
    fun aTagReferenceForABookNobodyHasIsNotWritten() = runBlocking {
        // Belt behind the serializer: nothing may create a book_tags row that
        // points at a book neither on the device nor in the file, because no
        // cleanup path would ever reach it again.
        applyRestore(
            BackupData(
                books = emptyList(),
                topics = emptyList(),
                tags = listOf(BackupTag(1, "فقه")),
                bookTags = listOf(BackupRef(idUnknown, 1)),
                excludedFolders = emptyList()
            )
        )

        assertEquals(emptyList<BookTagCrossRef>(), tagDao.observeAllRefs().first())
    }
}
