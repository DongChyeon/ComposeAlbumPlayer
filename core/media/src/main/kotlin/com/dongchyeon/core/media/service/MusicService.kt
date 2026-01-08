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
        private const val ADJACENT_TRACK_PRELOAD_DURATION_MS = 30_000L // 인접 곡: 30초
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
                // 30초 (기본 50초)
                30_000,
                // 2분 (기본 50초)
                120_000,
                // 1.5초 (기본 2.5초)
                1_500,
                // 3초 (기본 5초)
                3_000,
            )
            .setBackBuffer(
                // 30초 (기본 0초)
                30_000,
                false,
            )
            .build()

        val preloadManagerBuilder = DefaultPreloadManager.Builder(
            this,
            MusicPreloadStatusControl(),
        )

        preloadManager = preloadManagerBuilder.build()

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
                preloadManager.add(mediaItem, i)
                Log.d(TAG, "Added to preload: ${mediaItem.mediaId} (distance=$distance)")
            }
        }

        Log.d(TAG, "Preload triggered for index: $newIndex")
    }

    /**
     * 프리로드된 트랙으로 재생 전환
     *
     * 플레이리스트 내에서 트랙을 전환합니다.
     * PreloadManager가 CacheDataSource를 통해 데이터를 미리 캐싱했으므로,
     * seekTo()로 전환해도 캐시 히트로 빠르게 재생됩니다.
     *
     * 참고: setMediaSource()를 사용하면 플레이리스트가 손실되므로 seekTo() 사용
     *
     * @param mediaId 재생할 트랙의 mediaId
     * @return 성공 여부
     */
    private fun playPreloadedTrack(mediaId: String): Boolean {
        Log.d(TAG, "playPreloadedTrack called with mediaId: $mediaId")
        Log.d(TAG, "Player mediaItemCount: ${player.mediaItemCount}")

        // 플레이리스트의 모든 mediaId 로깅
        val allMediaIds = (0 until player.mediaItemCount).map {
            player.getMediaItemAt(it).mediaId
        }
        Log.d(TAG, "Available mediaIds in player: $allMediaIds")

        // 플레이리스트에서 해당 mediaId의 인덱스 찾기
        val targetIndex = (0 until player.mediaItemCount)
            .firstOrNull { player.getMediaItemAt(it).mediaId == mediaId }

        if (targetIndex == null) {
            Log.e(TAG, "mediaId not found in player playlist: $mediaId")
            return false
        }

        Log.d(TAG, "Found targetIndex: $targetIndex for mediaId: $mediaId")

        // 플레이리스트 내에서 트랙 전환 (seekTo로 플레이리스트 유지)
        player.seekTo(targetIndex, 0)
        player.play()

        Log.d(TAG, "seekTo($targetIndex, 0) and play() called")

        // 현재 인덱스 업데이트 및 다음 프리로드 트리거
        currentPlayingIndex = targetIndex
        preloadAdjacentTracks(targetIndex)
        return true
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
            val availableCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(SessionCommand(MusicCommand.ACTION_PRELOAD_ADJACENT, Bundle.EMPTY))
                .add(SessionCommand(MusicCommand.ACTION_PLAY_PRELOADED, Bundle.EMPTY))
                .add(SessionCommand(MusicCommand.ACTION_RESET_PRELOAD, Bundle.EMPTY))
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
            Log.d(TAG, "onCustomCommand received: ${customCommand.customAction}")
            Log.d(TAG, "Args bundle: $args")

            val command = MusicCommand.fromAction(customCommand.customAction, args)
            Log.d(TAG, "Parsed command: $command")

            val resultCode = when (command) {
                is MusicCommand.PreloadAdjacentTracks -> {
                    Log.d(TAG, "Handling PreloadAdjacentTracks: currentIndex=${command.currentIndex}")
                    preloadAdjacentTracks(command.currentIndex)
                    SessionResult.RESULT_SUCCESS
                }

                is MusicCommand.PlayPreloaded -> {
                    Log.d(TAG, "Handling PlayPreloaded: mediaId=${command.mediaId}")
                    val success = playPreloadedTrack(command.mediaId)
                    Log.d(TAG, "PlayPreloaded result: success=$success")
                    if (success) SessionResult.RESULT_SUCCESS else SessionResult.RESULT_ERROR_UNKNOWN
                }

                is MusicCommand.ResetPreload -> {
                    Log.d(TAG, "Handling ResetPreload")
                    resetPreload()
                    SessionResult.RESULT_SUCCESS
                }

                null -> {
                    Log.w(TAG, "Unknown custom command: ${customCommand.customAction}")
                    SessionResult.RESULT_ERROR_NOT_SUPPORTED
                }
            }

            Log.d(TAG, "Command result: $resultCode")
            return Futures.immediateFuture(SessionResult(resultCode))
        }
    }
}
