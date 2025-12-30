package com.dongchyeon.core.media.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground Service로 동작하는 음악 재생 서비스
 * MediaSession을 통해 Notification 및 외부 제어 지원
 */
@AndroidEntryPoint
class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()

        // ExoPlayer 설정
        player = ExoPlayer.Builder(this)
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
