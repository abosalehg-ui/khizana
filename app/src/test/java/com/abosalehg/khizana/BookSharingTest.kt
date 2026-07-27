package com.abosalehg.khizana

import com.abosalehg.khizana.domain.model.BookFormat
import com.abosalehg.khizana.ui.share.BookSharing
import org.junit.Assert.assertEquals
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
}
