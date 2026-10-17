package com.abosalehg.khizana.reader.engine

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    fun provideBookEngineFactory(): BookEngineFactory = EngineFactory
}
