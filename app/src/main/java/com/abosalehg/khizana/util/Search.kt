package com.abosalehg.khizana.util

/**
 * Search normalization tuned for Arabic titles: lowercases, strips tashkeel,
 * and unifies common letter variants (أ/إ/آ/ٱ → ا, ة → ه, ى → ي) so
 * "الطبرى" finds "الطبري" and "تاريخ" matches regardless of diacritics.
 */
fun normalizeForSearch(text: String): String = buildString(text.length) {
    for (raw in text.lowercase()) {
        when (raw) {
            in 'ً'..'ْ' -> Unit    // harakat/tashkeel: dropped
            'ـ' -> Unit                 // tatweel: dropped
            'أ', 'إ', 'آ', 'ٱ' -> append('ا')  // أ إ آ ٱ → ا
            'ة' -> append('ه')     // ة → ه
            'ى' -> append('ي')     // ى → ي
            else -> append(raw)
        }
    }
}

/**
 * Case/diacritic-insensitive containment; query must be pre-normalized.
 *
 * Convenience for one-off checks. The library screen does **not** use this —
 * it normalizes each book once per database emission and matches against the
 * stored blob, because normalizing the haystack on every keystroke is what
 * used to put the search on the main thread's critical path.
 */
fun matchesSearch(haystack: String, normalizedQuery: String): Boolean =
    normalizeForSearch(haystack).contains(normalizedQuery)
