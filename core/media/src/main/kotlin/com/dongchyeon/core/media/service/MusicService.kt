package com.dongchyeon.core.media.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
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
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var cacheDataSourceFactory: DataSource.Factory

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()

        // 캐시를 적용한 MediaSourceFactory 생성
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(cacheDataSourceFactory)

        // 음악 재생에 최적화된 LoadControl 설정
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs */ 30_000,                     // 30초 (기본 50초)
                /* maxBufferMs */ 120_000,                    // 2분 (기본 50초)
                /* bufferForPlaybackMs */ 1_500,              // 1.5초 (기본 2.5초)
                /* bufferForPlaybackAfterRebufferMs */ 3_000  // 3초 (기본 5초)
            )
            .setBackBuffer(
                /* backBufferDurationMs */ 30_000,            // 30초 (기본 0초)
                /* retainBackBufferFromKeyframe */ false
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

        // MediaSession 생성
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

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    /**
     * MediaSession 콜백
     * 필요시 커스텀 명령 처리 가능
     */
    private inner class MediaSessionCallback : MediaSession.Callback {
        // 기본 재생 제어(play, pause, seekTo 등)는 자동으로 처리됨
        // 필요시 onCustomCommand 등으로 커스텀 명령 추가 가능
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 앱이 태스크에서 제거되면 서비스 중지
        val player = mediaSession?.player
        if (player?.playWhenReady == false) {
            stopSelf()
        }
    }
}
