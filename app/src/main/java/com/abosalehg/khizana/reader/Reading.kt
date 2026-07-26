package com.abosalehg.khizana.reader

import com.abosalehg.khizana.domain.model.ReadingDirection

/**
 * Progress for the cover bar: 0..1. A single-page (or empty) book is done
 * the moment it's opened; otherwise the last page means 1.0.
 */
fun readingProgress(pageIndex: Int, pageCount: Int): Float = when {
    pageCount <= 0 -> 0f
    pageCount == 1 -> 1f
    else -> (pageIndex.toFloat() / (pageCount - 1)).coerceIn(0f, 1f)
}

/**
 * Resolves AUTO to a concrete direction: Arabic characters in the title
 * mean right-to-left, anything else left-to-right.
 */
fun resolveReadingDirection(direction: ReadingDirection, title: String): ReadingDirection =
    when (direction) {
        ReadingDirection.RTL, ReadingDirection.LTR -> direction
        ReadingDirection.AUTO ->
            if (containsArabic(title)) ReadingDirection.RTL else ReadingDirection.LTR
    }

private fun containsArabic(text: String): Boolean = text.any { ch ->
    ch.code in 0x0600..0x06FF ||    // Arabic
        ch.code in 0x0750..0x077F ||    // Arabic Supplement
        ch.code in 0xFB50..0xFDFF ||    // Presentation Forms-A
        ch.code in 0xFE70..0xFEFF       // Presentation Forms-B
}
