package com.abosalehg.khizana.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books")
    suspend fun getAll(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: String): BookEntity?

    /** Everything the shelves may show: not hidden, still present on disk. */
    @Query("SELECT * FROM books WHERE isHidden = 0 AND status != 'MISSING' ORDER BY addedAt DESC")
    fun observeVisible(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(book: BookEntity)

    /**
     * Rescan found a known fingerprint at a (possibly) new location: update the
     * location fields and status ONLY — progress, hiding, topic, tags all survive.
     */
    @Query(
        "UPDATE books SET path = :path, fileName = :fileName, fileSize = :fileSize, " +
            "status = :status WHERE id = :id"
    )
    suspend fun updateLocation(id: String, path: String, fileName: String, fileSize: Long, status: String)

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

    @Query("UPDATE books SET topicId = :topicId, locator = :locator, progress = :progress, " +
        "isHidden = :isHidden, readingDirection = :readingDirection, lastReadAt = :lastReadAt " +
        "WHERE id = :id")
    suspend fun applyRestoredMetadata(
        id: String,
        topicId: Long?,
        locator: String?,
        progress: Float,
        isHidden: Boolean,
        readingDirection: String,
        lastReadAt: Long?
    )
}
