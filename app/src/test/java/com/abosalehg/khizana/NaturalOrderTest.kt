package com.abosalehg.khizana

import com.abosalehg.khizana.util.NaturalOrderComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalOrderTest {

    private fun assertBefore(a: String, b: String) {
        assertTrue("expected \"$a\" < \"$b\"", NaturalOrderComparator.compare(a, b) < 0)
    }

    @Test
    fun `volume 2 comes before volume 10 in arabic`() {
        assertBefore("المجلد 2", "المجلد 10")
    }

    @Test
    fun `unpadded numbers sort numerically in latin`() {
        assertBefore("vol2", "vol10")
        assertBefore("page_2.jpg", "page_10.jpg")
    }

    @Test
    fun `arabic-indic digits compare numerically`() {
        assertBefore("صفحة ٢", "صفحة ١٠")
    }

    @Test
    fun `comparison is case-insensitive`() {
        assertBefore("Vol2", "vol10")
        assertEquals(0, NaturalOrderComparator.compare("ABC", "abc"))
    }

    @Test
    fun `equal numbers with fewer leading zeros come first`() {
        assertBefore("chapter 1", "chapter 01")
        assertEquals(0, NaturalOrderComparator.compare("chapter 01", "chapter 01"))
    }

    @Test
    fun `zero-padded and unpadded still order numerically`() {
        assertBefore("img007", "img10")
    }

    @Test
    fun `plain text falls back to lexicographic`() {
        assertBefore("apple", "banana")
        assertBefore("كتاب", "مجلة")
    }

    @Test
    fun `shorter prefix comes first`() {
        assertBefore("vol", "vol2")
    }

    @Test
    fun `sorting a list gives natural order`() {
        val sorted = listOf("المجلد 10", "المجلد 3", "المجلد 1")
            .sortedWith(NaturalOrderComparator)
        assertEquals(listOf("المجلد 1", "المجلد 3", "المجلد 10"), sorted)
    }
}
