package com.abosalehg.khizana

import com.abosalehg.khizana.data.backup.BackupFormatException
import com.abosalehg.khizana.data.backup.BackupSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * A backup file is user-supplied input. These cover the checks that keep a
 * hand-edited or hostile file from reaching the database — in particular the
 * book id, which becomes a file name in the cover store.
 */
class BackupSerializerValidationTest {

    private fun payload(books: String = "[]", version: String = "1") =
        """{"version": $version, "books": $books, "topics": [], "tags": [], """ +
            """"bookTags": [], "excludedFolders": []}"""

    private val validId = "0123456789abcdef".repeat(4)

    private fun book(id: String, extra: String = "") =
        """[{"id": "$id", "fileName": "b.pdf", "format": "PDF", "title": "b"$extra}]"""

    @Test
    fun aFileWithoutAVersionIsRejected() {
        val json = """{"books": [], "topics": [], "tags": [], "bookTags": [], """ +
            """"excludedFolders": []}"""
        assertThrows(BackupFormatException::class.java) { BackupSerializer.fromJson(json) }
    }

    @Test
    fun aNewerFormatVersionIsRejectedRatherThanPartlyApplied() {
        assertThrows(BackupFormatException::class.java) {
            BackupSerializer.fromJson(payload(version = "${BackupSerializer.FORMAT_VERSION + 1}"))
        }
    }

    @Test
    fun aBookmarkIdThatIsNotAFingerprintIsRejected() {
        val json = """{"version": 2, "bookmarks": [{"bookId": "../x", "page": 1}]}"""
        assertThrows(BackupFormatException::class.java) { BackupSerializer.fromJson(json) }
    }

    @Test
    fun aNegativeBookmarkPageIsClampedInsteadOfStored() {
        val json = """{"version": 2, "bookmarks": """ +
            """[{"bookId": "$validId", "page": -4, "createdAt": -1}]}"""
        val bookmark = BackupSerializer.fromJson(json).bookmarks.single()
        assertEquals(0, bookmark.page)
        assertEquals(0L, bookmark.createdAt)
    }

    @Test
    fun nonJsonInputIsRejected() {
        assertThrows(BackupFormatException::class.java) {
            BackupSerializer.fromJson("this is not a backup")
        }
    }

    @Test
    fun aTraversalStyleBookIdIsRejected() {
        assertThrows(BackupFormatException::class.java) {
            BackupSerializer.fromJson(payload(book("../../databases/khizana")))
        }
    }

    @Test
    fun aShortOrNonHexBookIdIsRejected() {
        assertThrows(BackupFormatException::class.java) {
            BackupSerializer.fromJson(payload(book("deadbeef")))
        }
        assertThrows(BackupFormatException::class.java) {
            BackupSerializer.fromJson(payload(book("Z".repeat(64))))
        }
    }

    @Test
    fun aBareFingerprintIsAccepted() {
        val data = BackupSerializer.fromJson(payload(book(validId)))
        assertEquals(validId, data.books.single().id)
    }

    @Test
    fun outOfRangeNumbersAreClampedInsteadOfStored() {
        val data = BackupSerializer.fromJson(
            payload(book(validId, """, "progress": 7.5, "pageCount": -3, "fileSize": -1"""))
        )
        val book = data.books.single()
        assertEquals(1f, book.progress, 0f)
        assertEquals(0, book.pageCount)
        assertEquals(0L, book.fileSize)
    }

    @Test
    fun negativeProgressIsClampedToZero() {
        val data = BackupSerializer.fromJson(
            payload(book(validId, """, "progress": -2.0"""))
        )
        assertEquals(0f, data.books.single().progress, 0f)
    }

    @Test
    fun aTagReferenceWithoutAFingerprintIsRejected() {
        // book_tags has no foreign key, so such a row would sit there for
        // good — and keep a tag alive with no book behind it.
        val json = """
            {"version":2,"bookTags":[{"bookId":"../../etc/passwd","tagId":1}]}
        """.trimIndent()
        assertThrows(BackupFormatException::class.java) { BackupSerializer.fromJson(json) }
    }

    @Test
    fun aTagReferenceWithARealFingerprintIsKept() {
        val id = "c".repeat(64)
        val json = """
            {"version":2,"bookTags":[{"bookId":"$id","tagId":4}]}
        """.trimIndent()
        val data = BackupSerializer.fromJson(json)
        assertEquals(1, data.bookTags.size)
        assertEquals(id, data.bookTags.single().bookId)
    }

    @Test
    fun aBlankExcludedFolderIsDroppedRatherThanStored() {
        // Storing "" would make every absolute path excluded on the next scan.
        val json = """
            {"version":2,"excludedFolders":["","/","  ","/storage/emulated/0/Private"]}
        """.trimIndent()
        val data = BackupSerializer.fromJson(json)
        assertEquals(listOf("/storage/emulated/0/Private"), data.excludedFolders)
    }

    @Test
    fun aRelativeExcludedFolderIsDropped() {
        val json = """{"version":2,"excludedFolders":["Books","./Books"]}"""
        assertEquals(emptyList<String>(), BackupSerializer.fromJson(json).excludedFolders)
    }
}
