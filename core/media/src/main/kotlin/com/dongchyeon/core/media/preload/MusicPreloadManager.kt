package com.dongchyeon.core.media.preload

import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import com.dongchyeon.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 음악 프리로드 매니저
 *
 * 현재 재생 중인 곡의 인덱스가 변경될 때 다음/이전 곡을 미리 캐시에 로드합니다.
 * CacheWriter를 사용하여 백그라운드에서 데이터를 다운로드하고,
 * 기존 CacheDataSource 시스템과 완벽히 호환됩니다.
 *
 * 프리로드 정책:
 * - 다음 곡: 전체 프리로드 (높은 우선순위)
 * - 이전 곡: 처음 30초만 프리로드 (낮은 우선순위, 뒤로가기 대비)
 */
@OptIn(UnstableApi::class)
@Singleton
class MusicPreloadManager @Inject constructor(
    private val cacheDataSourceFactory: CacheDataSource.Factory,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preloadJobs = mutableMapOf<String, Job>()

    // 이전 곡 프리로드 시간 (30초, 320kbps 기준 약 1.2MB)
    private val previousTrackPreloadBytes = 30L * 320 * 1000 / 8 // 약 1.2MB

    companion object {
        private const val TAG = "MusicPreloadManager"
    }

    /**
     * 현재 재생 인덱스가 변경될 때 호출
     *
     * 이전 프리로드 작업을 취소하고, 새로운 다음/이전 곡을 프리로드합니다.
     *
     * @param playlist 전체 플레이리스트
     * @param currentIndex 현재 재생 중인 트랙의 인덱스
     */
    fun onCurrentIndexChanged(playlist: List<Track>, currentIndex: Int) {
        // 기존 프리로드 작업 취소
        cancelAllPreloads()

        if (playlist.isEmpty() || currentIndex < 0) return

        // 다음 곡 프리로드 (전체)
        if (currentIndex + 1 < playlist.size) {
            val nextTrack = playlist[currentIndex + 1]
            preloadTrack(nextTrack, PreloadPriority.HIGH)
            Log.d(TAG, "Preloading next track: ${nextTrack.title}")
        }

        // 이전 곡 프리로드 (처음 30초만)
        if (currentIndex > 0) {
            val prevTrack = playlist[currentIndex - 1]
            preloadTrack(prevTrack, PreloadPriority.LOW)
            Log.d(TAG, "Preloading previous track (partial): ${prevTrack.title}")
        }
    }

    /**
     * 특정 트랙을 캐시에 프리로드
     *
     * @param track 프리로드할 트랙
     * @param priority 프리로드 우선순위 (HIGH: 전체, LOW: 부분)
     */
    private fun preloadTrack(track: Track, priority: PreloadPriority) {
        val streamUrl = track.streamUrl

        // 이미 프리로드 중인 경우 스킵
        if (preloadJobs.containsKey(track.id)) return

        val job = scope.launch {
            try {
                val dataSpec = when (priority) {
                    PreloadPriority.HIGH -> {
                        // 전체 프리로드
                        DataSpec(streamUrl.toUri())
                    }
                    PreloadPriority.LOW -> {
                        // 부분 프리로드 (처음 30초)
                        DataSpec.Builder()
                            .setUri(streamUrl.toUri())
                            .setLength(previousTrackPreloadBytes)
                            .build()
                    }
                }

                val cacheWriter = CacheWriter(
                    cacheDataSourceFactory.createDataSource(),
                    dataSpec,
                    // temporaryBuffer: 128 KB 버퍼
                    ByteArray(128 * 1024),
                    // progressListener
                    { _, bytesCached, _ ->
                        Log.v(TAG, "Preload progress [${track.title}]: $bytesCached bytes")
                    },
                )

                cacheWriter.cache()
                Log.d(TAG, "Preload completed: ${track.title}")
            } catch (e: Exception) {
                // 프리로드 실패는 무시 (재생 시 네트워크에서 로드됨)
                Log.w(TAG, "Preload failed for ${track.title}: ${e.message}")
            }
        }

        preloadJobs[track.id] = job
    }

    /**
     * 특정 트랙의 프리로드 취소
     */
    fun cancelPreload(trackId: String) {
        preloadJobs[trackId]?.cancel()
        preloadJobs.remove(trackId)
    }

    /**
     * 모든 프리로드 작업 취소
     */
    fun cancelAllPreloads() {
        preloadJobs.values.forEach { it.cancel() }
        preloadJobs.clear()
    }

    /**
     * 리소스 해제
     */
    fun release() {
        cancelAllPreloads()
    }

    /**
     * 프리로드 우선순위
     */
    private enum class PreloadPriority {
        HIGH, // 다음 곡 - 전체 프리로드
        LOW, // 이전 곡 - 부분 프리로드 (30초)
    }
}
