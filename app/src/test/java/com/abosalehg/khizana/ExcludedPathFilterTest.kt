package com.abosalehg.khizana

import com.abosalehg.khizana.data.scanner.ExcludedPathFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcludedPathFilterTest {

    private val excluded = listOf("/storage/emulated/0/Books")

    @Test
    fun `direct child of an excluded folder is excluded`() {
        assertTrue(
            ExcludedPathFilter.isExcluded("/storage/emulated/0/Books/novel.pdf", excluded)
        )
    }

    @Test
    fun `nested descendant of an excluded folder is excluded`() {
        assertTrue(
            ExcludedPathFilter.isExcluded("/storage/emulated/0/Books/old/x/y.cbz", excluded)
        )
    }

    @Test
    fun `similar-named sibling folder is NOT excluded`() {
        assertFalse(
            ExcludedPathFilter.isExcluded("/storage/emulated/0/BooksOld/novel.pdf", excluded)
        )
    }

    @Test
    fun `the folder path itself is excluded`() {
        assertTrue(ExcludedPathFilter.isExcluded("/storage/emulated/0/Books", excluded))
    }

    @Test
    fun `trailing slash in the stored exclusion still matches`() {
        assertTrue(
            ExcludedPathFilter.isExcluded(
                "/storage/emulated/0/Books/a.pdf",
                listOf("/storage/emulated/0/Books/")
            )
        )
    }

    @Test
    fun `unrelated paths are not excluded`() {
        assertFalse(
            ExcludedPathFilter.isExcluded("/storage/emulated/0/Download/a.pdf", excluded)
        )
    }
}
