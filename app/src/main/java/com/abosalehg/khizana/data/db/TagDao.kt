package com.abosalehg.khizana.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Query("SELECT * FROM book_tags")
    fun observeAllRefs(): Flow<List<BookTagCrossRef>>

    @Query("SELECT tagId FROM book_tags WHERE bookId = :bookId")
    suspend fun tagIdsForBook(bookId: String): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addRef(ref: BookTagCrossRef)

    @Query("DELETE FROM book_tags WHERE bookId = :bookId AND tagId = :tagId")
    suspend fun removeRef(bookId: String, tagId: Long)

    /** Tags left with no books are removed to keep the filter row tidy. */
    @Query("DELETE FROM tags WHERE id NOT IN (SELECT DISTINCT tagId FROM book_tags)")
    suspend fun pruneUnused()
}
