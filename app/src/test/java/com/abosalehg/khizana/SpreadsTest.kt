package com.abosalehg.khizana

import com.abosalehg.khizana.reader.Spread
import com.abosalehg.khizana.reader.buildSpreads
import com.abosalehg.khizana.reader.spreadIndexOfPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpreadsTest {

    @Test
    fun `empty book has no spreads`() {
        assertTrue(buildSpreads(0).isEmpty())
    }

    @Test
    fun `single page is just the lone cover`() {
        assertEquals(listOf(Spread(0, null)), buildSpreads(1))
    }

    @Test
    fun `odd page count pairs cleanly after the cover`() {
        assertEquals(
            listOf(Spread(0, null), Spread(1, 2), Spread(3, 4)),
            buildSpreads(5)
        )
    }

    @Test
    fun `even page count leaves a trailing lone page`() {
        assertEquals(
            listOf(Spread(0, null), Spread(1, 2), Spread(3, 4), Spread(5, null)),
            buildSpreads(6)
        )
    }

    @Test
    fun `every page appears exactly once across spreads`() {
        val pages = buildSpreads(17).flatMap { listOfNotNull(it.first, it.second) }
        assertEquals((0 until 17).toList(), pages)
    }

    @Test
    fun `page to spread mapping matches the built structure`() {
        val spreads = buildSpreads(10)
        for (page in 0 until 10) {
            val index = spreadIndexOfPage(page)
            val spread = spreads[index]
            assertTrue(spread.first == page || spread.second == page)
        }
    }

    @Test
    fun `cover maps to spread zero`() {
        assertEquals(0, spreadIndexOfPage(0))
        assertEquals(1, spreadIndexOfPage(1))
        assertEquals(1, spreadIndexOfPage(2))
        assertEquals(2, spreadIndexOfPage(3))
    }
}
