package com.abosalehg.khizana.domain.model

/** Supported book file formats. EPUB is reserved for a later milestone. */
enum class BookFormat {
    PDF,
    CBZ,
    EPUB;

    companion object {
        fun fromName(name: String?): BookFormat? =
            entries.firstOrNull { it.name == name }

        /** Maps a lowercase file extension to a format, or null if unsupported. */
        fun fromExtension(extension: String): BookFormat? = when (extension.lowercase()) {
            "pdf" -> PDF
            "cbz" -> CBZ
            else -> null
        }
    }
}

/** Health/availability of a book file on disk. */
enum class BookStatus {
    OK,
    PROTECTED,
    CORRUPT,
    MISSING;

    companion object {
        fun fromName(name: String?): BookStatus =
            entries.firstOrNull { it.name == name } ?: OK
    }
}

/** Per-book reading direction. AUTO guesses from Arabic characters in the title. */
enum class ReadingDirection {
    RTL,
    LTR,
    AUTO;

    companion object {
        fun fromName(name: String?): ReadingDirection =
            entries.firstOrNull { it.name == name } ?: AUTO
    }
}

/** Domain representation of a book in the library. */
data class Book(
    val id: String,
    val path: String,
    val fileName: String,
    val format: BookFormat,
    val title: String,
    val author: String?,
    val topicId: Long?,
    val pageCount: Int,
    /** Abstract reading position: page number today, EPUB CFI later. Always a String. */
    val locator: String?,
    val progress: Float,
    val readingDirection: ReadingDirection,
    val coverPath: String?,
    val coverFailed: Boolean,
    val status: BookStatus,
    val isHidden: Boolean,
    val manualOrder: Int,
    val fileSize: Long,
    val addedAt: Long,
    val lastReadAt: Long?
)
