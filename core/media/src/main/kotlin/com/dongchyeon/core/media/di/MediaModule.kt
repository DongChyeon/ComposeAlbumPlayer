package com.dongchyeon.core.media.di

import com.dongchyeon.core.media.player.AndroidExoPlayerManager
import com.dongchyeon.core.media.player.ExoPlayerManager
import com.dongchyeon.core.media.player.MusicPlayerImpl
import com.dongchyeon.domain.player.MusicPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {
    @Binds
    @Singleton
    abstract fun bindExoPlayerManager(androidExoPlayerManager: AndroidExoPlayerManager): ExoPlayerManager

    @Binds
    @Singleton
    abstract fun bindMusicPlayer(musicPlayerImpl: MusicPlayerImpl): MusicPlayer
}
