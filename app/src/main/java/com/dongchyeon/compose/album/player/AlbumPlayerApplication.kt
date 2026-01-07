package com.dongchyeon.compose.album.player

import android.app.Application
import android.content.ComponentCallbacks2
import com.dongchyeon.core.media.cache.CacheManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AlbumPlayerApplication : Application(), ComponentCallbacks2 {

    @Inject
    lateinit var cacheManager: CacheManager

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        cacheManager.trimCacheIfNeeded()
    }
}
