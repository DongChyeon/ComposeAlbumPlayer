package com.dongchyeon.core.media.di

import com.dongchyeon.core.media.controller.MediaControllerMusicPlayer
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
    abstract fun bindMusicPlayer(
        impl: MediaControllerMusicPlayer,
    ): MusicPlayer
}
