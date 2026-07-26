package com.abosalehg.khizana

import com.abosalehg.khizana.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `known names map to their mode`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromName("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromName("DARK"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("SYSTEM"))
    }

    @Test
    fun `null or unknown names fall back to SYSTEM`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("SEPIA"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("light"))
    }
}
