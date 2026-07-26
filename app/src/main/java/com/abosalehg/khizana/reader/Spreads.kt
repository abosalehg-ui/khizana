package com.abosalehg.khizana.reader

/**
 * A two-page spread for landscape reading. [first] is the lower page index;
 * [second] is null for the cover (always alone) and for a trailing odd page.
 *
 * Rendering places [first] at the start of a Row — which is the RIGHT side
 * under an RTL layout direction and the LEFT under LTR — so the same model
 * reads correctly in both directions.
 */
data class Spread(val first: Int, val second: Int?)

/** Cover alone, then (1,2), (3,4), … with a trailing odd page alone. */
fun buildSpreads(pageCount: Int): List<Spread> {
    if (pageCount <= 0) return emptyList()
    return buildList {
        add(Spread(0, null))
        var page = 1
        while (page < pageCount) {
            val second = (page + 1).takeIf { it < pageCount }
            add(Spread(page, second))
            page += 2
        }
    }
}

/** Which spread a page lives on: 0 → 0, (1,2) → 1, (3,4) → 2, … */
fun spreadIndexOfPage(page: Int): Int = (page.coerceAtLeast(0) + 1) / 2
