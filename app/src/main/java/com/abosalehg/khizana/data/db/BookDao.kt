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
}
