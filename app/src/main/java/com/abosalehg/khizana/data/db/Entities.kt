package com.abosalehg.khizana.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "books")
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
