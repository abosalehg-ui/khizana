package com.abosalehg.khizana.data.scanner

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScannerModule {

    @Provides
    @Singleton
    fun provideLibraryScanner(@ApplicationContext context: Context): LibraryScanner =
        MediaStoreLibraryScanner(context)
}
