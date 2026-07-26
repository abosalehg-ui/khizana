package com.abosalehg.khizana

import com.abosalehg.khizana.domain.model.ReadingDirection
import com.abosalehg.khizana.reader.readingProgress
import com.abosalehg.khizana.reader.resolveReadingDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingTest {

    @Test
    fun `zero pages means zero progress`() {
        assertEquals(0f, readingProgress(0, 0), 0f)
    }

    @Test
    fun `single-page book is complete on open`() {
        assertEquals(1f, readingProgress(0, 1), 0f)
    }

    @Test
    fun `first page of a long book is zero`() {
        assertEquals(0f, readingProgress(0, 100), 0f)
    }

    @Test
    fun `last page is exactly one`() {
        assertEquals(1f, readingProgress(99, 100), 1e-6f)
    }

    @Test
    fun `middle page is half`() {
        assertEquals(0.5f, readingProgress(50, 101), 1e-6f)
    }

    @Test
    fun `out-of-range indices clamp instead of exploding`() {
        assertEquals(0f, readingProgress(-5, 10), 0f)
        assertEquals(1f, readingProgress(500, 10), 0f)
    }

    @Test
    fun `auto direction with an Arabic title resolves to RTL`() {
        assertEquals(
            ReadingDirection.RTL,
            resolveReadingDirection(ReadingDirection.AUTO, "تاريخ الطبري - الجزء الأول")
        )
    }

    @Test
    fun `auto direction with a Latin title resolves to LTR`() {
        assertEquals(
            ReadingDirection.LTR,
            resolveReadingDirection(ReadingDirection.AUTO, "A Tale of Two Cities")
        )
    }

    @Test
    fun `auto direction with mixed text prefers RTL when Arabic present`() {
        assertEquals(
            ReadingDirection.RTL,
            resolveReadingDirection(ReadingDirection.AUTO, "Vol 3 - ألف ليلة وليلة")
        )
    }

    @Test
    fun `explicit direction always wins over the title`() {
        assertEquals(
            ReadingDirection.LTR,
            resolveReadingDirection(ReadingDirection.LTR, "كتاب عربي")
        )
        assertEquals(
            ReadingDirection.RTL,
            resolveReadingDirection(ReadingDirection.RTL, "English book")
        )
    }
}
