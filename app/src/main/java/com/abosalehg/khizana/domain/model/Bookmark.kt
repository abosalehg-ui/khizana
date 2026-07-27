package com.abosalehg.khizana.domain.model

/** A saved place in a book, with an optional note the reader typed. */
data class Bookmark(
    val id: Long,
    val bookId: String,
    /** Zero-based page index — the same number the reading locator stores. */
    val page: Int,
    val note: String?,
    val createdAt: Long
)

/**
 * Longest note we keep. Notes are free text that is re-read on every reader
 * open and written into the backup file, so the field is bounded on purpose;
 * the dialog stops accepting input at this length rather than truncating
 * silently on save.
 */
const val MAX_NOTE_LENGTH = 500

/**
 * One representation for "no note": a note that is blank, or only spaces, is
 * stored as NULL rather than as an empty string, so the reader never has to
 * ask which of the two it is looking at.
 */
fun normalizeNote(raw: String?): String? =
    raw?.trim()?.take(MAX_NOTE_LENGTH)?.takeIf { it.isNotEmpty() }

/** The bookmark standing on [page] — a page carries at most one. */
fun bookmarkAt(bookmarks: List<Bookmark>, page: Int): Bookmark? =
    bookmarks.firstOrNull { it.page == page }
