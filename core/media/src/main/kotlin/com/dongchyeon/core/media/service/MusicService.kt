package com.dongchyeon.core.media.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.dongchyeon.core.media.command.MusicCommand
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground Service로 동작하는 음악 재생 서비스
 * MediaSession을 통해 Notification 및 외부 제어 지원
 *
 * 캐싱 정책:
 * - 디바이스 가용 공간의 3% (50MB ~ 500MB)
 * - LRU 방식으로 오래된 캐시 자동 삭제
 * - 청크 크기 2MB (음악에 최적화)
 *
 * 버퍼링 정책 (음악 최적화):
 * - minBuffer: 30초 (비디오 기본 50초 대비 감소 - 음악은 비트레이트가 낮아 충분)
 * - maxBuffer: 2분 (비디오 기본 50초 대비 증가 - 다음 곡까지 여유있게 버퍼링)
 * - bufferForPlayback: 1.5초 (비디오 기본 2.5초 대비 감소 - 빠른 재생 시작)
 * - backBuffer: 30초 (비디오 기본 0초 대비 증가 - 뒤로 감기 시 즉각 반응)
 *
 * 프리로드 정책 (CacheWriter 기반):
 * - 다음 곡: 전체 프리로드
 * - 이전 곡: 처음 30초 프리로드
 * - CustomCommand를 통해 App Process에서 제어
 *.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MusicService : MediaSessionService() {

    companion object {
        private const val TAG = "MusicService"

        // 이전 곡 프리로드 바이트 (30초, 320kbps 기준 약 1.2MB)
        private const val PREV_TRACK_PRELOAD_BYTES = 30L * 320 * 1000 / 8
    }

    @Inject
    lateinit var cacheDataSourceFactory: CacheDataSource.Factory

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    // 프리로드 관련
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preloadJobs = mutableMapOf<String, Job>()

    override fun onCreate() {
        super.onCreate()

        // 캐시를 적용한 MediaSourceFactory 생성
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(cacheDataSourceFactory)

        // 음악 재생에 최적화된 LoadControl 설정
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                // minBufferMs: 30초 (기본 50초)
                30_000,
                // maxBufferMs: 2분 (기본 50초)
                120_000,
                // bufferForPlaybackMs: 1.5초 (기본 2.5초)
                1_500,
                // bufferForPlaybackAfterRebufferMs: 3초 (기본 5초)
                3_000,
            )
            .setBackBuffer(
                // backBufferDurationMs: 30초 (기본 0초)
                30_000,
                // retainBackBufferFromKeyframe
                false,
            )
            .build()

        // ExoPlayer 설정 (캐싱 + 음악 최적화 버퍼링 적용)
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true) // 헤드폰 제거 시 일시정지
            .build()

        // MediaSession 생성 (CustomCommand 콜백 포함)
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MediaSessionCallback())
            .setSessionActivity(getPendingIntent())
            .build()
    }

    /**
     * 앱 실행 PendingIntent (Notification 클릭 시)
     */
    private fun getPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        cancelAllPreloads()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 앱이 태스크에서 제거되면 서비스 중지
        val player = mediaSession?.player
        if (player?.playWhenReady == false) {
            stopSelf()
        }
    }

    private fun preloadAdjacentTracks(currentIndex: Int) {
        val mediaItemCount = player.mediaItemCount
        if (mediaItemCount == 0 || currentIndex < 0) return

        // 기존 프리로드 취소
        cancelAllPreloads()

        // 다음 곡 프리로드 (전체)
        if (currentIndex + 1 < mediaItemCount) {
            val nextItem = player.getMediaItemAt(currentIndex + 1)
            preloadTrack(nextItem, isFullPreload = true)
        }

        // 이전 곡 프리로드 (처음 30초만)
        if (currentIndex > 0) {
            val prevItem = player.getMediaItemAt(currentIndex - 1)
            preloadTrack(prevItem, isFullPreload = false)
        }
    }

    private fun preloadTrack(mediaItem: MediaItem, isFullPreload: Boolean) {
        val uri = mediaItem.localConfiguration?.uri ?: return
        val mediaId = mediaItem.mediaId

        // 이미 프리로드 중인 경우 스킵
        if (preloadJobs.containsKey(mediaId)) return

        val job = preloadScope.launch {
            try {
                val dataSpec = if (isFullPreload) {
                    DataSpec(uri)
                } else {
                    DataSpec.Builder()
                        .setUri(uri)
                        .setLength(PREV_TRACK_PRELOAD_BYTES)
                        .build()
                }

                val cacheWriter = CacheWriter(
                    cacheDataSourceFactory.createDataSource(),
                    dataSpec,
                    ByteArray(128 * 1024), // 128KB 버퍼
                ) { _, bytesCached, _ ->
                    Log.v(TAG, "Preload progress [$mediaId]: $bytesCached bytes")
                }

                cacheWriter.cache()
                Log.d(TAG, "Preload completed: $mediaId (full=$isFullPreload)")
            } catch (e: Exception) {
                Log.w(TAG, "Preload failed for $mediaId: ${e.message}")
            }
        }

        preloadJobs[mediaId] = job
        Log.d(TAG, "Preloading track: $mediaId (full=$isFullPreload)")
    }

    private fun playPreloadedTrack(mediaId: String): Boolean {
        val targetIndex = (0 until player.mediaItemCount)
            .firstOrNull { player.getMediaItemAt(it).mediaId == mediaId }
            ?: return false

        Log.d(TAG, "Playing track: $mediaId (preloaded in cache)")
        player.seekTo(targetIndex, 0)
        player.play()
        return true
    }

    private fun cancelAllPreloads() {
        preloadJobs.values.forEach { it.cancel() }
        preloadJobs.clear()
        Log.d(TAG, "All preloads cancelled")
    }

    private fun resetPreload() {
        cancelAllPreloads()
        Log.d(TAG, "Preload reset")
    }

    private inner class MediaSessionCallback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            // CustomCommand 허용 목록 설정
            val availableCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(SessionCommand(MusicCommand.ACTION_PRELOAD_ADJACENT, Bundle.EMPTY))
                .add(SessionCommand(MusicCommand.ACTION_PLAY_PRELOADED, Bundle.EMPTY))
                .add(SessionCommand(MusicCommand.ACTION_RESET_PRELOAD, Bundle.EMPTY))
                .add(SessionCommand(MusicCommand.ACTION_GET_PRELOAD_STATUS, Bundle.EMPTY))
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            val command = MusicCommand.fromAction(customCommand.customAction, args)

            val resultCode = when (command) {
                is MusicCommand.PreloadAdjacentTracks -> {
                    preloadAdjacentTracks(command.currentIndex)
                    SessionResult.RESULT_SUCCESS
                }

                is MusicCommand.PlayPreloaded -> {
                    val success = playPreloadedTrack(command.mediaId)
                    if (success) SessionResult.RESULT_SUCCESS else SessionResult.RESULT_ERROR_UNKNOWN
                }

                is MusicCommand.ResetPreload -> {
                    resetPreload()
                    SessionResult.RESULT_SUCCESS
                }

                is MusicCommand.GetPreloadStatus -> {
                    // 현재 프리로드 상태 반환 (확장 가능)
                    SessionResult.RESULT_SUCCESS
                }

                null -> {
                    Log.w(TAG, "Unknown custom command: ${customCommand.customAction}")
                    SessionResult.RESULT_ERROR_NOT_SUPPORTED
                }
            }

            return Futures.immediateFuture(SessionResult(resultCode))
        }
    }
}
