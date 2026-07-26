package com.abosalehg.khizana.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookEntity::class,
        TopicEntity::class,
        TagEntity::class,
        BookTagCrossRef::class,
        BookmarkEntity::class,
        ExcludedFolderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KhizanaDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun topicDao(): TopicDao
    abstract fun excludedFolderDao(): ExcludedFolderDao
    abstract fun tagDao(): TagDao
}
