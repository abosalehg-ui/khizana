package com.abosalehg.khizana

import com.abosalehg.khizana.data.covers.CbzCover
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CbzCoverTest {

    @Test
    fun `picks the alphabetically first image as cover`() {
        val entries = listOf("page_010.jpg", "page_002.png", "page_001.jpg", "info.txt")
        assertEquals("page_001.jpg", CbzCover.pickCoverEntry(entries))
    }

    @Test
    fun `ignores directories, hidden files and macos junk`() {
        val entries = listOf(
            "__MACOSX/._page1.jpg",
            "pages/",
            "pages/.hidden.jpg",
            "pages/cover.webp"
        )
        assertEquals("pages/cover.webp", CbzCover.pickCoverEntry(entries))
    }

    @Test
    fun `archive with no images has no cover`() {
        assertNull(CbzCover.pickCoverEntry(listOf("readme.txt", "meta/", "data.xml")))
    }

    @Test
    fun `image entry detection covers formats and rejects others`() {
        assertTrue(CbzCover.isImageEntry("a.JPG"))
        assertTrue(CbzCover.isImageEntry("x/y/b.webp"))
        assertFalse(CbzCover.isImageEntry("a.pdf"))
        assertFalse(CbzCover.isImageEntry("noextension"))
    }

    @Test
    fun `image count ignores non-image entries`() {
        val entries = listOf("1.jpg", "2.png", "sub/3.gif", "readme.txt", "sub/")
        assertEquals(3, CbzCover.countImages(entries))
    }

    @Test
    fun `sample size downsamples large sources but never below target`() {
        assertEquals(1, CbzCover.sampleSize(500, 480))
        assertEquals(2, CbzCover.sampleSize(1000, 480))
        assertEquals(4, CbzCover.sampleSize(2000, 480))
        assertEquals(1, CbzCover.sampleSize(300, 480))
    }
}
