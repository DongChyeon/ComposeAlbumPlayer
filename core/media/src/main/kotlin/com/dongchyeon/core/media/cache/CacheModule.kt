package com.dongchyeon.core.media.cache

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * 미디어 캐싱을 위한 Hilt 모듈
 *
 * SimpleCache는 앱 전체에서 하나의 인스턴스만 사용해야 함 (Singleton 필수)
 */
@OptIn(UnstableApi::class)
@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    /**
     * 미디어 캐시 디렉토리 제공
     */
    @Provides
    @Singleton
    fun provideCacheDir(@ApplicationContext context: Context): File {
        return File(context.cacheDir, "media_cache").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * 캐시 메타데이터용 SQLite 데이터베이스 제공
     */
    @Provides
    @Singleton
    fun provideDatabaseProvider(@ApplicationContext context: Context): StandaloneDatabaseProvider {
        return StandaloneDatabaseProvider(context)
    }

    /**
     * SimpleCache 제공
     *
     * - 디바이스 가용 공간 기준 동적 캐시 크기 (50MB ~ 500MB)
     * - LRU 정책으로 오래된 항목부터 자동 삭제
     */
    @Provides
    @Singleton
    fun provideSimpleCache(
        @ApplicationContext context: Context,
        cacheDir: File,
        databaseProvider: StandaloneDatabaseProvider,
    ): SimpleCache {
        val cacheSize = DynamicCacheConfig.calculateCacheSize(context)
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(cacheSize)

        return SimpleCache(cacheDir, cacheEvictor, databaseProvider)
    }

    /**
     * 캐시를 적용한 CacheDataSource.Factory 제공
     *
     * - 캐시 히트 시: 디스크에서 읽기
     * - 캐시 미스 시: 네트워크에서 다운로드 후 캐시에 저장
     * - 청크 크기: 2MB (음악에 최적화)
     */
    @Provides
    @Singleton
    fun provideCacheDataSourceFactory(
        simpleCache: SimpleCache,
    ): CacheDataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        val cacheDataSinkFactory = CacheDataSink.Factory()
            .setCache(simpleCache)
            .setFragmentSize(DynamicCacheConfig.FRAGMENT_SIZE_BYTES)

        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setCacheWriteDataSinkFactory(cacheDataSinkFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
