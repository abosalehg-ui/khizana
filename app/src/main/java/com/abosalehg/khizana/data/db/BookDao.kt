package com.abosalehg.khizana.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * The `'MISSING'` / `'OK'` literals below must stay in sync with
 * `BookStatus`. `KhizanaDatabaseTest` asserts the two against each other, so
 * renaming an enum constant without touching these queries fails the build
 * instead of quietly breaking the shelves.
 */
@Dao
interface BookDao {

    @Query("SELECT * FROM books")
    suspend fun getAll(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: String): BookEntity?

    /** Everything the shelves may show: not hidden, still present on disk. */
    @Query("SELECT * FROM books WHERE isHidden = 0 AND status != 'MISSING' ORDER BY addedAt DESC")
    fun observeVisible(): Flow<List<BookEntity>>

    /**
     * The visible books of one shelf (`topicId = null` is the "New ⭐" shelf).
     * Reordering only ever touches a single shelf, so it must not load the
     * whole library the way [getAll] does.
     */
    @Query(
        "SELECT * FROM books WHERE isHidden = 0 AND status != 'MISSING' " +
            "AND ((:topicId IS NULL AND topicId IS NULL) OR topicId = :topicId)"
    )
    suspend fun booksOnShelf(topicId: Long?): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(book: BookEntity)

    /**
     * Rescan found a known fingerprint at a (possibly) new location: update the
     * location fields and status ONLY — progress, hiding, topic, tags all survive.
     */
    @Query(
        "UPDATE books SET path = :path, fileName = :fileName, fileSize = :fileSize, " +
            "status = :status, lastModified = :lastModified WHERE id = :id"
    )
    suspend fun updateLocation(
        id: String,
        path: String,
        fileName: String,
        fileSize: Long,
        status: String,
        lastModified: Long
    )

    /** Books whose files disappeared. Rows are never deleted — data must survive. */
    @Query("UPDATE books SET status = 'MISSING' WHERE id IN (:ids)")
    suspend fun markMissing(ids: List<String>)

    /** Candidates for cover generation, including retries after a failed attempt is reset. */
    @Query("SELECT * FROM books WHERE status = 'OK' AND coverPath IS NULL AND coverFailed = 0")
    suspend fun getNeedingCovers(): List<BookEntity>

    @Query("UPDATE books SET coverPath = :coverPath, pageCount = :pageCount, coverFailed = 0 WHERE id = :id")
    suspend fun setCover(id: String, coverPath: String, pageCount: Int)

    @Query("UPDATE books SET coverFailed = 1 WHERE id = :id")
    suspend fun setCoverFailed(id: String)

    @Query("UPDATE books SET status = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: String)

    @Query("UPDATE books SET topicId = :topicId WHERE id = :id")
    suspend fun updateTopic(id: String, topicId: Long?)

    @Query(
        "UPDATE books SET locator = :locator, progress = :progress, lastReadAt = :lastReadAt " +
            "WHERE id = :id"
    )
    suspend fun saveReadingPosition(id: String, locator: String, progress: Float, lastReadAt: Long)

    @Query("UPDATE books SET pageCount = :pageCount WHERE id = :id")
    suspend fun updatePageCount(id: String, pageCount: Int)

    @Query("UPDATE books SET isHidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: String, hidden: Boolean)

    @Query("UPDATE books SET manualOrder = :order WHERE id = :id")
    suspend fun updateManualOrder(id: String, order: Int)

    @Query("SELECT * FROM books WHERE isHidden = 1 AND status != 'MISSING' ORDER BY title ASC")
    fun observeHidden(): Flow<List<BookEntity>>

    /** On shelf deletion its books return to the New shelf. */
    @Query("UPDATE books SET topicId = NULL WHERE topicId = :topicId")
    suspend fun clearTopic(topicId: Long)

    /** Explicit user deletion only — rescans never call this. */
    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM book_tags WHERE bookId = :bookId")
    suspend fun deleteTagRefsForBook(bookId: String)

    /**
     * Writes back everything a backup file owns for a book that already lives
     * on this device. `manualOrder` belongs in here: the file carries it, the
     * insert path below applies it, and Settings promises the backup keeps
     * "your own drag-and-drop order" — leaving it out silently flattened the
     * hand-made arrangement of every book the restoring device already had.
     *
     * Location, format and cover stay untouched on purpose: those describe the
     * file as it is here and now, not what some other device recorded.
     */
    @Query("UPDATE books SET topicId = :topicId, locator = :locator, progress = :progress, " +
        "isHidden = :isHidden, readingDirection = :readingDirection, lastReadAt = :lastReadAt, " +
        "manualOrder = :manualOrder WHERE id = :id")
    suspend fun applyRestoredMetadata(
        id: String,
        topicId: Long?,
        locator: String?,
        progress: Float,
        isHidden: Boolean,
        readingDirection: String,
        lastReadAt: Long?,
        manualOrder: Int
    )
}
