package com.rockmusic.app.di

import android.content.Context
import androidx.room.Room
import com.rockmusic.app.data.local.LocalMusicRepository
import com.rockmusic.app.data.local.MediaStoreLocalMusicRepository
import com.rockmusic.app.data.local.RockDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindLocalMusicRepository(
        implementation: MediaStoreLocalMusicRepository,
    ): LocalMusicRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RockDatabase =
        Room.databaseBuilder(context, RockDatabase::class.java, "rock_music.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
}
