package com.dongchyeon.core.media.cache

import android.content.Context
import android.os.StatFs
import java.io.File

/**
 * 디바이스 가용 공간을 기반으로 동적으로 캐시 크기를 계산하는 설정
 *
 * - 가용 공간의 3%를 캐시로 사용
 * - 최소 50MB, 최대 500MB로 제한
 * - 청크 크기는 음악에 최적화된 2MB 사용
 */
object DynamicCacheConfig {

    // 가용 공간의 3%를 캐시로 사용
    private const val CACHE_PERCENTAGE = 0.03

    // 최소 캐시 크기: 50MB (약 5-10곡)
    private const val MIN_CACHE_SIZE_BYTES = 50L * 1024 * 1024

    // 최대 캐시 크기: 500MB (약 50곡)
    private const val MAX_CACHE_SIZE_BYTES = 500L * 1024 * 1024

    // 청크 크기: 2MB (음악에 최적화)
    const val FRAGMENT_SIZE_BYTES = 2L * 1024 * 1024

    /**
     * 디바이스 가용 공간을 기준으로 캐시 크기를 동적으로 계산
     *
     * @param context Application context
     * @return 계산된 캐시 크기 (bytes)
     */
    fun calculateCacheSize(context: Context): Long {
        val cacheDir = context.cacheDir
        val availableBytes = getAvailableSpace(cacheDir)

        val dynamicSize = (availableBytes * CACHE_PERCENTAGE).toLong()

        return dynamicSize.coerceIn(MIN_CACHE_SIZE_BYTES, MAX_CACHE_SIZE_BYTES)
    }

    /**
     * 디렉토리의 가용 공간 조회
     */
    private fun getAvailableSpace(directory: File): Long {
        return StatFs(directory.absolutePath).availableBytes
    }
}
