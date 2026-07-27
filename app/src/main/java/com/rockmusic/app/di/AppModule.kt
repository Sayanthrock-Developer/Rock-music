package com.rockmusic.app.di

import com.rockmusic.app.data.local.LocalMusicRepository
import com.rockmusic.app.data.local.MediaStoreLocalMusicRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindLocalMusicRepository(
        implementation: MediaStoreLocalMusicRepository,
    ): LocalMusicRepository
}
