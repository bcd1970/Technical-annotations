package com.bcd.technotes.di

import com.bcd.technotes.feature.picker.MediaRepository
import com.bcd.technotes.feature.picker.MediaRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository
}
