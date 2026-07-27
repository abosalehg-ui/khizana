package com.abosalehg.khizana.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BookEntity::class,
        TopicEntity::class,
        TagEntity::class,
        BookTagCrossRef::class,
        BookmarkEntity::class,
        ExcludedFolderEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class KhizanaDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun topicDao(): TopicDao
    abstract fun excludedFolderDao(): ExcludedFolderDao
    abstract fun tagDao(): TagDao

    companion object {
        /**
         * v1 → v2: adds the three `books` indices. Index names must match what
         * Room generates for the @Index annotations, or validation fails on open.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_topicId` ON `books` (`topicId`)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_books_isHidden_status` " +
                        "ON `books` (`isHidden`, `status`)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_addedAt` ON `books` (`addedAt`)")
            }
        }
    }
}
