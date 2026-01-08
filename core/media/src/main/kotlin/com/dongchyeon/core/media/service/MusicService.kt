@file:OptIn(UnstableApi::class)

package com.dongchyeon.core.media.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.dongchyeon.core.media.command.MusicCommand
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs

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
 * 프리로드 정책 (DefaultPreloadManager 기반):
 * - 다음 곡: 30초 프리로드 (MediaSource 준비 + 캐시 저장)
 * - 이전 곡: 30초 프리로드 (MediaSource 준비 + 캐시 저장)
 * - 2칸 떨어진 곡: 트랙 선택까지만 준비
 * - CustomCommand를 통해 App Process에서 제어
 */
@AndroidEntryPoint
class MusicService : MediaSessionService() {

    companion object {
        private const val TAG = "MusicService"

        // 프리로드 시간 설정 (ms)
        private const val ADJACENT_TRACK_PRELOAD_DURATION_MS = 30_000L  // 인접 곡: 30초
    }

    @Inject
    lateinit var cacheDataSourceFactory: CacheDataSource.Factory

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var preloadManager: DefaultPreloadManager

    // 현재 재생 중인 인덱스 (프리로드 우선순위 계산에 사용)
    private var currentPlayingIndex: Int = C.INDEX_UNSET

    override fun onCreate() {
        super.onCreate()

        // 캐시를 적용한 MediaSourceFactory 생성
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(cacheDataSourceFactory)

        // 음악 재생에 최적화된 LoadControl 설정
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs */ 30_000,      // 30초 (기본 50초)
                /* maxBufferMs */ 120_000,     // 2분 (기본 50초)
                /* bufferForPlaybackMs */ 1_500,              // 1.5초 (기본 2.5초)
                /* bufferForPlaybackAfterRebufferMs */ 3_000, // 3초 (기본 5초)
            )
            .setBackBuffer(
                /* backBufferDurationMs */ 30_000, // 30초 (기본 0초)
                /* retainBackBufferFromKeyframe */ false,
            )
            .build()

        // PreloadManager 빌더 생성 (동적 인덱스 참조를 위해 inner class 사용)
        val preloadManagerBuilder = DefaultPreloadManager.Builder(
            this,
            MusicPreloadStatusControl(),
        )

        // PreloadManager 저장 (나중에 add(), getMediaSource() 호출에 사용)
        preloadManager = preloadManagerBuilder.build()

        // ExoPlayer 빌드 (PreloadManager와 컴포넌트 공유)
        player = preloadManagerBuilder.buildExoPlayer(
            ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true,
                )
                .setHandleAudioBecomingNoisy(true),
        )

        // MediaSession 생성 (CustomCommand 콜백 포함)
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MediaSessionCallback())
            .setSessionActivity(getPendingIntent())
            .build()
    }

    /**
     * 프리로드 우선순위 제어
     *
     * currentPlayingIndex를 기준으로 인접 트랙의 프리로드 수준을 결정합니다.
     * - 1칸 떨어진 곡 (다음/이전): 30초 프리로드
     * - 2칸 떨어진 곡: 트랙 선택까지만 준비
     * - 4칸 이내: 소스 준비까지만
     * - 그 외: 프리로드 안 함
     */
    private inner class MusicPreloadStatusControl :
        TargetPreloadStatusControl<Int, DefaultPreloadManager.PreloadStatus> {

        override fun getTargetPreloadStatus(rankingData: Int): DefaultPreloadManager.PreloadStatus {
            val distance = abs(rankingData - currentPlayingIndex)

            return when {
                // 다음/이전 곡: 30초 프리로드
                distance == 1 -> DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(
                    ADJACENT_TRACK_PRELOAD_DURATION_MS,
                )
                // 2칸 떨어진 곡: 트랙 선택까지만
                distance == 2 -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_TRACKS_SELECTED
                // 3~4칸 떨어진 곡: 소스 준비까지만
                distance in 3..4 -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_SOURCE_PREPARED
                // 그 외: 프리로드 안 함
                else -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
            }
        }
    }

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
        preloadManager.release()
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

    // ==================== 프리로드 관련 메서드 (DefaultPreloadManager 기반) ====================

    /**
     * 인접 트랙 프리로드 요청
     *
     * 현재 인덱스를 업데이트하고, PreloadManager에 인접 트랙들을 추가합니다.
     * PreloadManager가 CacheDataSource를 사용하므로 프리로드된 데이터는 캐시에도 저장됩니다.
     *
     * @param newIndex 현재 재생 중인 트랙의 인덱스
     */
    private fun preloadAdjacentTracks(newIndex: Int) {
        val mediaItemCount = player.mediaItemCount
        if (mediaItemCount == 0 || newIndex < 0) return

        // 현재 인덱스 업데이트 (MusicPreloadStatusControl에서 참조)
        currentPlayingIndex = newIndex

        // 기존 프리로드 초기화
        preloadManager.reset()

        // 인접 트랙들을 PreloadManager에 추가 (rankingData = 인덱스)
        // MusicPreloadStatusControl이 인덱스 차이로 프리로드 수준 결정
        for (i in 0 until mediaItemCount) {
            val distance = abs(i - newIndex)
            if (distance in 1..4) {
                val mediaItem = player.getMediaItemAt(i)
                preloadManager.add(mediaItem, /* rankingData */ i)
                Log.d(TAG, "Added to preload: ${mediaItem.mediaId} (distance=$distance)")
            }
        }

        Log.d(TAG, "Preload triggered for index: $newIndex")
    }

    /**
     * 프리로드된 트랙으로 재생 전환
     *
     * PreloadManager에서 준비된 MediaSource를 가져와 즉시 재생합니다.
     * 프리로드되지 않은 경우 일반 재생으로 fallback합니다.
     *
     * @param mediaId 재생할 트랙의 mediaId
     * @return 성공 여부
     */
    private fun playPreloadedTrack(mediaId: String): Boolean {
        // 플레이리스트에서 해당 mediaId의 인덱스 찾기
        val targetIndex = (0 until player.mediaItemCount)
            .firstOrNull { player.getMediaItemAt(it).mediaId == mediaId }
            ?: return false

        val mediaItem = player.getMediaItemAt(targetIndex)

        // 프리로드된 MediaSource 가져오기
        val preloadedSource = preloadManager.getMediaSource(mediaItem)

        return if (preloadedSource != null) {
            // 프리로드된 소스로 즉시 재생
            Log.d(TAG, "Playing with preloaded MediaSource: $mediaId")
            player.setMediaSource(preloadedSource)
            player.prepare()
            player.play()

            // 현재 인덱스 업데이트 및 다음 프리로드 트리거
            currentPlayingIndex = targetIndex
            preloadAdjacentTracks(targetIndex)
            true
        } else {
            // 프리로드되지 않은 경우 일반 재생
            Log.d(TAG, "MediaSource not preloaded, playing normally: $mediaId")
            player.seekTo(targetIndex, 0)
            player.play()

            // 현재 인덱스 업데이트 및 프리로드 트리거
            currentPlayingIndex = targetIndex
            preloadAdjacentTracks(targetIndex)
            true
        }
    }

    private fun resetPreload() {
        preloadManager.reset()
        currentPlayingIndex = C.INDEX_UNSET
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
