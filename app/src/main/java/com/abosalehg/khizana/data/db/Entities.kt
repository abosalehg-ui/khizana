package com.abosalehg.khizana.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Indices back the three hot access paths:
 *  - `observeVisible` / `observeHidden` filter on (isHidden, status) and sort by addedAt,
 *  - `booksOnShelf` and `clearTopic` filter on topicId,
 * all of which were full table scans before.
 */
@Entity(
    tableName = "books",
    indices = [
        Index("topicId"),
        Index(value = ["isHidden", "status"]),
        Index("addedAt")
    ]
)
data class BookEntity(
    /** Content fingerprint: SHA-256(fileSize + first 64 KB) — survives moves/renames. */
    @PrimaryKey val id: String,
    /** Updated in place on rescan when the file moved. */
    val path: String,
    val fileName: String,
    val format: String,                      // PDF | CBZ | EPUB
    val title: String,                       // defaults to file name without extension
    val author: String? = null,
    val topicId: Long? = null,               // null = the "New ⭐" shelf
    val pageCount: Int = 0,
    /** Abstract reading position. Stored as String from day one (EPUB CFI later). */
    val locator: String? = null,
    val progress: Float = 0f,                // 0..1 for the cover progress bar
    val readingDirection: String = "AUTO",   // RTL | LTR | AUTO
    val coverPath: String? = null,
    val coverFailed: Boolean = false,
    val status: String = "OK",               // OK | PROTECTED | CORRUPT | MISSING
    val isHidden: Boolean = false,
    val manualOrder: Int = 0,
    val fileSize: Long = 0,
    val addedAt: Long = 0,
    val lastReadAt: Long? = null
)

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val order: Int = 0
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "book_tags",
    primaryKeys = ["bookId", "tagId"],
    indices = [Index("tagId")]
)
data class BookTagCrossRef(
    val bookId: String,
    val tagId: Long
)

/**
 * Reserved for the bookmarks milestone: the table ships from v1 so adding
 * bookmarks later needs no migration. There is deliberately no DAO and no UI
 * yet — the README lists bookmarks under Roadmap, not Features.
 */
@Entity(tableName = "bookmarks", indices = [Index("bookId")])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val page: Int,
    val note: String? = null,
    val createdAt: Long
)

@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(
    @PrimaryKey val path: String
)
