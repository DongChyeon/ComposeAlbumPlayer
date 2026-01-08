package com.dongchyeon.compose.album.player

import android.app.Application
import com.dongchyeon.core.network.BaseUrlProvider
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AlbumPlayerApplication : Application() {

    @Inject
    lateinit var baseUrlProvider: BaseUrlProvider

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // 앱 시작 시 Audius Discovery API 호출하여 동적 BASE_URL 설정
        applicationScope.launch {
            baseUrlProvider.initialize()
        }
    }
}
