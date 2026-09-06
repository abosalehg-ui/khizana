package com.abosalehg.khizana.util

/**
 * Natural string ordering: digit runs compare as numbers, so "المجلد 2"
 * sorts before "المجلد 10". Understands Western (0-9), Arabic-Indic (٠-٩)
 * and Eastern Arabic-Indic (۰-۹) digits; text compares case-insensitively,
 * and Arabic letter variants (أ إ آ ٱ / ة / ى) fold the same way the search
 * folds them, so "أحمد" files under alef rather than ahead of it.
 * No number parsing into ints — digit runs compare by stripped length then
 * digit-by-digit, so arbitrarily long numbers can't overflow.
 */
object NaturalOrderComparator : Comparator<String> {

    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val da = digitValue(a[i])
            val db = digitValue(b[j])
            if (da != null && db != null) {
                val endA = digitRunEnd(a, i)
                val endB = digitRunEnd(b, j)
                val cmp = compareDigitRuns(a, i, endA, b, j, endB)
                if (cmp != 0) return cmp
                i = endA
                j = endB
            } else {
                val cmp = foldChar(a[i]).compareTo(foldChar(b[j]))
                if (cmp != 0) return cmp
                i++
                j++
            }
        }
        return (a.length - i).compareTo(b.length - j)
    }

    private fun digitRunEnd(s: String, start: Int): Int {
        var end = start
        while (end < s.length && digitValue(s[end]) != null) end++
        return end
    }

    private fun compareDigitRuns(
        a: String, startA: Int, endA: Int,
        b: String, startB: Int, endB: Int
    ): Int {
        var i = startA
        var j = startB
        while (i < endA && digitValue(a[i]) == 0) i++
        while (j < endB && digitValue(b[j]) == 0) j++
        val lenCmp = (endA - i).compareTo(endB - j)
        if (lenCmp != 0) return lenCmp
        while (i < endA && j < endB) {
            val cmp = digitValue(a[i])!!.compareTo(digitValue(b[j])!!)
            if (cmp != 0) return cmp
            i++
            j++
        }
        // Same numeric value: fewer leading zeros first ("1" before "01").
        return (endA - startA).compareTo(endB - startB)
    }

    /**
     * Folds a character to the form it should sort as.
     *
     * Comparing raw code points puts every hamza form (\u0621..\u0626) ahead of plain
     * alef (\u0627), so «أحمد» and «إبراهيم» collected in a clump before «ابن»
     * instead of interleaving with it — a reader looking for a title under
     * alef would not find it there. This applies the same unification
     * `normalizeForSearch` already applies, so the shelf and the search agree
     * on what counts as the same letter.
     */
    private fun foldChar(c: Char): Char = when (c) {
        'أ', 'إ', 'آ', 'ٱ' -> 'ا'   // hamza-carrying alefs fold onto plain alef
        'ة' -> 'ه'                  // ta marbuta folds onto ha
        'ى' -> 'ي'                  // alef maqsura folds onto ya
        else -> c.lowercaseChar()
    }

    private fun digitValue(c: Char): Int? = when (c) {
        in '0'..'9' -> c - '0'
        in '٠'..'٩' -> c - '٠'   // ٠-٩
        in '۰'..'۹' -> c - '۰'   // ۰-۹
        else -> null
    }
}

/**
 * Shelf ordering: manually ordered books first by their position, then
 * everything unordered (manualOrder <= 0) by natural title order.
 */
fun <T> manualThenNaturalComparator(
    manualOrder: (T) -> Int,
    title: (T) -> String
): Comparator<T> =
    compareBy<T> { manualOrder(it).let { o -> if (o <= 0) Int.MAX_VALUE else o } }
        .then(compareBy(NaturalOrderComparator) { title(it) })
