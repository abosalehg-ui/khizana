package com.abosalehg.khizana.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcludedFolderDao {

    @Query("SELECT path FROM excluded_folders")
    suspend fun getAllPaths(): List<String>

    @Query("SELECT * FROM excluded_folders")
    fun observeAll(): Flow<List<ExcludedFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(folder: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE path = :path")
    suspend fun delete(path: String)
}
