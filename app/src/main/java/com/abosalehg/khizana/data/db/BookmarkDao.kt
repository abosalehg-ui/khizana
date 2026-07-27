package com.abosalehg.khizana.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Bookmarks are keyed by page, one per page: saving twice on the same page
 * edits the note instead of stacking a second row (see
 * `ReaderRepository.saveBookmark`). The `bookmarks` table itself has shipped
 * since schema v1, so giving it a DAO needs no migration.
 */
@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY page ASC")
    fun observeForBook(bookId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId AND page = :page LIMIT 1")
    suspend fun findAt(bookId: String, page: Int): BookmarkEntity?

    /** Backup export reads the whole table in one go. */
    @Query("SELECT * FROM bookmarks ORDER BY bookId ASC, page ASC")
    suspend fun getAll(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("UPDATE bookmarks SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String?)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * The book was deleted from the device: its notes go with it. Without
     * this the rows would outlive their book — nothing here is a foreign key,
     * so SQLite would not clean up on its own.
     */
    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}
