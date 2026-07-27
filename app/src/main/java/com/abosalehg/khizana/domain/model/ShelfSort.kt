package com.abosalehg.khizana.domain.model

import com.abosalehg.khizana.util.enumOrNull

/**
 * How books are ordered inside every shelf, persisted in DataStore.
 *
 * [MANUAL] stays the default: drag-and-drop ordering predates this setting and
 * has to keep working. The other three are views over the same shelves — they
 * never rewrite `manualOrder`, so switching back to [MANUAL] restores the
 * arrangement the reader built by hand.
 */
enum class ShelfSort {
    /** Hand-placed books first, then natural title order. */
    MANUAL,

    /** Natural title order throughout ("المجلد 2" before "المجلد 10"). */
    TITLE,

    /** Most recently added first. */
    DATE_ADDED,

    /** Largest file first. */
    SIZE;

    companion object {
        fun fromName(name: String?): ShelfSort = enumOrNull<ShelfSort>(name) ?: MANUAL
    }
}
