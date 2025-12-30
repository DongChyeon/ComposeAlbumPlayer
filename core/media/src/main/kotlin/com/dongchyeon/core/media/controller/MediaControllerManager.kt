package com.dongchyeon.core.media.controller

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.dongchyeon.core.media.service.MusicService
import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.RepeatMode
import com.dongchyeon.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaController를 관리하고 UI에 노출하는 매니저
 * MusicService와 연결하여 재생 제어
 */
@Singleton
class MediaControllerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var mediaController: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: Flow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: Flow<Track?> = _currentTrack.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: Flow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: Flow<Long> = _duration.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    val repeatMode: Flow<RepeatMode> = _repeatMode.asStateFlow()

    // 플레이리스트 정보 저장
    private var playlist: List<Track> = emptyList()
    private var currentIndex: Int = -1

    init {
        scope.launch {
            connectToService()
        }
    }

    /**
     * MusicService에 연결
     */
    private suspend fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java),
        )

        mediaController = MediaController.Builder(context, sessionToken)
            .buildAsync()
            .await()

        setupPlayerListener()
        startPositionUpdater()
    }

    /**
     * Player 상태 변화 리스너 설정
     */
    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlaybackState()
                if (playbackState == Player.STATE_READY) {
                    _duration.value = mediaController?.duration ?: 0L
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = mediaController?.currentMediaItemIndex ?: -1
                if (index >= 0 && index < playlist.size) {
                    currentIndex = index
                    _currentTrack.value = playlist[index]
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> RepeatMode.NONE
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.NONE
                }
            }
        })
    }

    /**
     * PlaybackState 업데이트
     */
    private fun updatePlaybackState() {
        val controller = mediaController ?: return
        _playbackState.value = when {
            controller.playbackState == Player.STATE_IDLE -> PlaybackState.Idle
            controller.playbackState == Player.STATE_ENDED -> PlaybackState.Idle
            controller.isPlaying -> PlaybackState.Playing
            else -> PlaybackState.Paused
        }
    }

    /**
     * 현재 위치 주기적 업데이트
     */
    private fun startPositionUpdater() {
        scope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _currentPosition.value = controller.currentPosition
                    }
                }
                delay(100)
            }
        }
    }

    /**
     * 트랙 재생
     */
    fun play(track: Track) {
        _currentTrack.value = track

        // 플레이리스트에서 해당 트랙 찾기
        val indexInPlaylist = playlist.indexOfFirst { it.id == track.id }
        if (indexInPlaylist != -1) {
            currentIndex = indexInPlaylist
            mediaController?.seekTo(indexInPlaylist, 0)
        } else {
            // 플레이리스트에 없으면 단일 트랙 재생
            val mediaItem = MediaItem.Builder()
                .setUri(track.streamUrl)
                .setMediaId(track.id)
                .build()
            mediaController?.setMediaItem(mediaItem)
            mediaController?.prepare()
        }

        mediaController?.play()
    }

    /**
     * 일시정지
     */
    fun pause() {
        mediaController?.pause()
    }

    /**
     * 재생 재개
     */
    fun resume() {
        mediaController?.play()
    }

    /**
     * 정지
     */
    fun stop() {
        _currentTrack.value = null
        mediaController?.stop()
        mediaController?.clearMediaItems()
    }

    /**
     * 위치 이동
     */
    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    /**
     * 플레이리스트 설정
     */
    fun setPlaylist(tracks: List<Track>) {
        playlist = tracks

        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setUri(track.streamUrl)
                .setMediaId(track.id)
                .build()
        }

        mediaController?.setMediaItems(mediaItems)
        mediaController?.prepare()

        if (tracks.isNotEmpty() && currentIndex < 0) {
            currentIndex = 0
        }
    }

    /**
     * 다음 트랙
     */
    fun skipToNext() {
        if (currentIndex >= 0 && currentIndex < playlist.size - 1) {
            currentIndex++
            val nextTrack = playlist[currentIndex]
            play(nextTrack)
        }
    }

    /**
     * 이전 트랙 (5초 임계값)
     */
    fun skipToPrevious() {
        val currentPos = mediaController?.currentPosition ?: 0L

        when {
            currentPos <= 5000L && currentIndex > 0 -> {
                currentIndex--
                val previousTrack = playlist[currentIndex]
                play(previousTrack)
            }
            else -> {
                seekTo(0L)
            }
        }
    }

    /**
     * 반복 모드 설정
     */
    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        mediaController?.repeatMode = when (mode) {
            RepeatMode.NONE -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    /**
     * 리소스 해제
     */
    fun release() {
        mediaController?.release()
        mediaController = null
    }
}
