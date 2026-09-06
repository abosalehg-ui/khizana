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

    @Test
    fun `an empty exclusion matches nothing`() {
        // "" trims to "", and the prefix test then reads startsWith("/") —
        // true of every absolute path on the device. One blank row would have
        // hidden the whole library behind a scan that reported success.
        assertFalse(ExcludedPathFilter.isExcluded("/storage/emulated/0/Books/a.pdf", listOf("")))
    }

    @Test
    fun `an exclusion of only slashes matches nothing`() {
        assertFalse(ExcludedPathFilter.isExcluded("/storage/emulated/0/a.pdf", listOf("/")))
        assertFalse(ExcludedPathFilter.isExcluded("/storage/emulated/0/a.pdf", listOf("///")))
    }

    @Test
    fun `a blank entry does not disarm the real exclusions beside it`() {
        val folders = listOf("", "/storage/emulated/0/Private")
        assertTrue(
            ExcludedPathFilter.isExcluded("/storage/emulated/0/Private/x.pdf", folders)
        )
        assertFalse(
            ExcludedPathFilter.isExcluded("/storage/emulated/0/Books/x.pdf", folders)
        )
    }
}
