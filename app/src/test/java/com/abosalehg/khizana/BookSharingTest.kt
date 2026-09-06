package com.abosalehg.khizana

import com.abosalehg.khizana.domain.model.BookFormat
import com.abosalehg.khizana.ui.share.BookSharing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The MIME type decides which apps the share sheet offers. Sending a CBZ as
 * `application/octet-stream` (the old default when a type is missing) hides
 * every comic reader on the device from the chooser.
 */
class BookSharingTest {

    @Test
    fun `each format shares under its own media type`() {
        assertEquals("application/pdf", BookSharing.mimeTypeFor(BookFormat.PDF))
        assertEquals("application/vnd.comicbook+zip", BookSharing.mimeTypeFor(BookFormat.CBZ))
        assertEquals("application/epub+zip", BookSharing.mimeTypeFor(BookFormat.EPUB))
    }

    @Test
    fun `every supported format has a type, including formats added later`() {
        BookFormat.entries.forEach { format ->
            assert(BookSharing.mimeTypeFor(format).contains('/')) { "no media type for $format" }
        }
    }

    @Test
    fun `books on shared storage volumes are shareable`() {
        assertTrue(BookSharing.isOnSharedStoragePath("/storage/emulated/0/Books/a.pdf"))
        assertTrue(BookSharing.isOnSharedStoragePath("/storage/1A2B-3C4D/Books/a.cbz"))
    }

    @Test
    fun `app-private and system paths are not shareable`() {
        // The FileProvider is scoped to storage volumes; this is the same
        // boundary in code, so widening the XML alone cannot hand out the
        // library database.
        assertFalse(
            BookSharing.isOnSharedStoragePath(
                "/data/data/com.abosalehg.khizana/databases/khizana.db"
            )
        )
        assertFalse(BookSharing.isOnSharedStoragePath("/system/etc/hosts"))
        assertFalse(BookSharing.isOnSharedStoragePath(""))
    }

    @Test
    fun `a traversal that escapes the storage tree is not shareable`() {
        // Three levels up from /storage/emulated/0 lands at the filesystem
        // root, so this really does leave shared storage. The check normalises
        // before comparing, which is the point: a prefix test on the raw string
        // would have said yes.
        assertFalse(
            BookSharing.isOnSharedStoragePath(
                "/storage/emulated/0/../../../data/data/com.abosalehg.khizana/databases/k.db"
            )
        )
    }

    @Test
    fun `a traversal that stays inside the storage tree is still shareable`() {
        // Two levels up only reaches /storage, so this is an awkwardly written
        // path to a real book, not an escape — and it must not be refused.
        assertTrue(
            BookSharing.isOnSharedStoragePath("/storage/emulated/0/../../emulated/0/a.pdf")
        )
    }

    @Test
    fun `a similar-named sibling of storage is not shareable`() {
        assertFalse(BookSharing.isOnSharedStoragePath("/storageX/a.pdf"))
    }
}
