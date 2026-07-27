package com.abosalehg.khizana.data.covers

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

/** Directory holding the generated cover JPEGs. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CoversDir

@Module
@InstallIn(SingletonComponent::class)
object CoversModule {

    @Provides
    @Singleton
    @CoversDir
    fun provideCoversDir(@ApplicationContext context: Context): File =
        File(context.filesDir, "covers")
}
