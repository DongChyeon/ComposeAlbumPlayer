package com.dongchyeon.core.media.cache

import android.content.Context
import android.os.Build
import android.os.StatFs
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 미디어 캐시 관리 유틸리티
 *
 * 캐시 상태 조회, 정리 등의 기능 제공
 */
@OptIn(UnstableApi::class)
@Singleton
class CacheManager @Inject constructor(
    private val cache: SimpleCache,
    @ApplicationContext private val context: Context,
) {
    /**
     * 현재 캐시된 데이터 크기 (bytes)
     */
    val cacheSize: Long
        get() = cache.cacheSpace

    /**
     * 캐시된 데이터 크기 (MB 단위, 소수점 1자리)
     */
    val cacheSizeMB: Float
        get() = cacheSize / (1024f * 1024f)

    /**
     * 캐시된 콘텐츠 개수
     */
    val cachedContentCount: Int
        get() = cache.keys.size

    /**
     * 특정 URL이 완전히 캐시되어 있는지 확인
     */
    fun isFullyCached(url: String, contentLength: Long): Boolean {
        return cache.isCached(url, 0, contentLength)
    }

    /**
     * 특정 URL의 캐시된 바이트 수
     */
    fun getCachedBytes(url: String, contentLength: Long): Long {
        return cache.getCachedBytes(url, 0, contentLength)
    }

    /**
     * 특정 URL의 캐시 진행률 (0.0 ~ 1.0)
     */
    fun getCacheProgress(url: String, contentLength: Long): Float {
        if (contentLength <= 0) return 0f
        return getCachedBytes(url, contentLength).toFloat() / contentLength
    }

    /**
     * 전체 캐시 삭제
     */
    fun clearCache() {
        cache.keys.toList().forEach { key ->
            cache.removeResource(key)
        }
    }

    /**
     * 특정 URL의 캐시 삭제
     */
    fun removeFromCache(url: String) {
        cache.removeResource(url)
    }

    /**
     * 저장공간 부족 시 캐시 정리
     *
     * 가용 공간이 1GB 미만이면 캐시의 50%를 삭제
     */
    fun trimCacheIfNeeded() {
        val availableSpace = getAvailableSpace(context.cacheDir)

        // 가용 공간이 1GB 미만이면 캐시 50% 정리
        if (availableSpace < 1L * 1024 * 1024 * 1024) {
            val targetSize = cache.cacheSpace / 2
            trimCacheTo(targetSize)
        }
    }

    /**
     * 캐시를 특정 크기 이하로 정리 (LRU 순서)
     */
    private fun trimCacheTo(targetBytes: Long) {
        val keys = cache.keys.toList()
        var currentSize = cache.cacheSpace

        for (key in keys) {
            if (currentSize <= targetBytes) break

            val keySize = cache.getCachedBytes(key, 0, Long.MAX_VALUE)
            cache.removeResource(key)
            currentSize -= keySize
        }
    }

    /**
     * 디렉토리의 가용 공간 조회
     */
    private fun getAvailableSpace(directory: File): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            StatFs(directory.absolutePath).availableBytes
        } else {
            @Suppress("DEPRECATION")
            val statFs = StatFs(directory.absolutePath)
            statFs.availableBlocks.toLong() * statFs.blockSize.toLong()
        }
    }
}
