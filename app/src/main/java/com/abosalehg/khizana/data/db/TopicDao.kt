package com.abosalehg.khizana.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {

    @Query("SELECT * FROM topics ORDER BY `order` ASC, id ASC")
    fun observeAll(): Flow<List<TopicEntity>>

    @Insert
    suspend fun insert(topic: TopicEntity): Long

    @Query("UPDATE topics SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun delete(id: Long)
}
