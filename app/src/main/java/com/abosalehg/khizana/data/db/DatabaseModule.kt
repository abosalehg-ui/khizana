package com.abosalehg.khizana.data.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KhizanaDatabase =
        Room.databaseBuilder(context, KhizanaDatabase::class.java, "khizana.db")
            // No destructive fallback: reading positions and shelves are the
            // whole point of the database, so every version needs a migration.
            .addMigrations(
                KhizanaDatabase.MIGRATION_1_2,
                KhizanaDatabase.MIGRATION_2_3
            )
            .build()

    @Provides
    @Singleton
    fun provideTransactionRunner(db: KhizanaDatabase): TransactionRunner =
        RoomTransactionRunner(db)

    @Provides
    fun provideBookDao(db: KhizanaDatabase): BookDao = db.bookDao()

    @Provides
    fun provideTopicDao(db: KhizanaDatabase): TopicDao = db.topicDao()

    @Provides
    fun provideExcludedFolderDao(db: KhizanaDatabase): ExcludedFolderDao = db.excludedFolderDao()

    @Provides
    fun provideTagDao(db: KhizanaDatabase): TagDao = db.tagDao()

    @Provides
    fun provideBookmarkDao(db: KhizanaDatabase): BookmarkDao = db.bookmarkDao()
}
