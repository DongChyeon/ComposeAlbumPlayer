package com.dongchyeon.core.media.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {
    // MediaControllerManager는 @Inject constructor가 있으므로
    // 자동으로 Hilt가 제공함
    // 명시적으로 제공하려면:
    // @Provides
    // @Singleton
    // fun provideMediaControllerManager(...): MediaControllerManager = ...
}
