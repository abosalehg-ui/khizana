package com.abosalehg.khizana

import com.abosalehg.khizana.data.backup.BackupBook
import com.abosalehg.khizana.data.backup.BackupData
import com.abosalehg.khizana.data.backup.BackupRef
import com.abosalehg.khizana.data.backup.BackupSerializer
import com.abosalehg.khizana.data.backup.BackupTag
import com.abosalehg.khizana.data.backup.BackupTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializerTest {

    // Book ids are content fingerprints; the parser now rejects anything else.
    private val idTarikh = "1".repeat(64)
    private val idComic = "2".repeat(64)

    private val sample = BackupData(
        books = listOf(
            BackupBook(
                id = idTarikh,
                fileName = "تاريخ الطبري.pdf",
                format = "PDF",
                title = "تاريخ الطبري",
                author = null,
                topicId = 7,
                pageCount = 500,
                locator = "42",
                progress = 0.25f,
                readingDirection = "RTL",
                isHidden = true,
                manualOrder = 3,
                fileSize = 1024,
                addedAt = 111,
                lastReadAt = 222
            ),
            BackupBook(
                id = idComic,
                fileName = "comic.cbz",
                format = "CBZ",
                title = "comic",
                author = "Someone",
                topicId = null,
                pageCount = 0,
                locator = null,
                progress = 0f,
                readingDirection = "AUTO",
                isHidden = false,
                manualOrder = 0,
                fileSize = 5,
                addedAt = 333,
                lastReadAt = null
            )
        ),
        topics = listOf(BackupTopic(7, "تاريخ", 0)),
        tags = listOf(BackupTag(1, "مفضلة")),
        bookTags = listOf(BackupRef(idTarikh, 1)),
        excludedFolders = listOf("/storage/emulated/0/Recordings")
    )

    @Test
    fun `round trip preserves everything`() {
        val restored = BackupSerializer.fromJson(BackupSerializer.toJson(sample))
        assertEquals(sample, restored)
    }

    @Test
    fun `nullable fields survive as real nulls`() {
        val restored = BackupSerializer.fromJson(BackupSerializer.toJson(sample))
        val comic = restored.books.first { it.id == idComic }
        assertNull(comic.topicId)
        assertNull(comic.locator)
        assertNull(comic.lastReadAt)
        assertNull(restored.books.first { it.id == idTarikh }.author)
    }

    @Test
    fun `arabic text survives the round trip`() {
        val restored = BackupSerializer.fromJson(BackupSerializer.toJson(sample))
        assertEquals("تاريخ الطبري", restored.books.first().title)
        assertEquals("مفضلة", restored.tags.first().name)
    }

    @Test
    fun `empty backup parses cleanly`() {
        val empty = BackupData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(empty, BackupSerializer.fromJson(BackupSerializer.toJson(empty)))
    }

    @Test
    fun `missing sections in the json default to empty lists`() {
        val restored = BackupSerializer.fromJson("""{"version":1}""")
        assertTrue(restored.books.isEmpty())
        assertTrue(restored.excludedFolders.isEmpty())
    }
}
