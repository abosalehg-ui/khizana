package com.abosalehg.khizana

import com.abosalehg.khizana.util.matchesSearch
import com.abosalehg.khizana.util.normalizeForSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {

    @Test
    fun `tashkeel is stripped`() {
        assertEquals("محمد", normalizeForSearch("مُحَمَّد"))
    }

    @Test
    fun `alef variants unify`() {
        assertEquals(normalizeForSearch("احمد"), normalizeForSearch("أحمد"))
        assertEquals(normalizeForSearch("الان"), normalizeForSearch("الآن"))
    }

    @Test
    fun `ta marbuta and alef maqsura unify`() {
        assertTrue(matchesSearch("مكتبة", normalizeForSearch("مكتبه")))
        assertTrue(matchesSearch("الطبرى", normalizeForSearch("الطبري")))
    }

    @Test
    fun `latin search is case-insensitive`() {
        assertTrue(matchesSearch("A Tale of Two Cities", normalizeForSearch("tale")))
    }

    @Test
    fun `diacritic query finds plain title and vice versa`() {
        assertTrue(matchesSearch("تاريخ الطبري", normalizeForSearch("تَارِيخ")))
        assertTrue(matchesSearch("تَارِيخ الطبري", normalizeForSearch("تاريخ")))
    }

    @Test
    fun `non-matching text does not match`() {
        assertFalse(matchesSearch("تاريخ الطبري", normalizeForSearch("فيزياء")))
    }

    @Test
    fun `dagger alef is dropped so classical spellings match`() {
        assertTrue(matchesSearch("تفسير الرحمٰن", normalizeForSearch("الرحمن")))
        assertTrue(matchesSearch("هٰذا كتاب", normalizeForSearch("هذا")))
    }

    @Test
    fun `combining hamza and maddah marks are dropped`() {
        // ا + U+0654 (hamza above) is how some sources spell أ.
        assertEquals(normalizeForSearch("احمد"), normalizeForSearch("ا\u0654حمد"))
    }

    @Test
    fun `hamza on waw and ya unify with the bare letter`() {
        assertTrue(matchesSearch("المسؤولية", normalizeForSearch("المسوولية")))
        assertTrue(matchesSearch("رسائل", normalizeForSearch("رسايل")))
    }
}
