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
    version = 3,
    exportSchema = true
)
abstract class KhizanaDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun topicDao(): TopicDao
    abstract fun excludedFolderDao(): ExcludedFolderDao
    abstract fun tagDao(): TagDao

    /** No version bump: `bookmarks` has been part of the schema since v1. */
    abstract fun bookmarkDao(): BookmarkDao

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

        /**
         * v2 → v3: adds `books.lastModified`, the rescan fast path's third
         * signal beside path and size.
         *
         * Existing rows default to 0, which no real mtime equals, so every
         * book is fingerprinted once more on the next scan and then carries
         * its mtime from there on. Backfilling was not an option — the
         * database cannot stat the filesystem.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `books` ADD COLUMN `lastModified` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
